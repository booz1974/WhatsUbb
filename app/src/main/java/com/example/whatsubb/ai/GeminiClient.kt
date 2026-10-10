package com.example.whatsubb.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Turn(val fromUser: Boolean, val text: String)

class AiException(message: String) : Exception(message)

/** Eén interface, zodat je later makkelijk een andere AI (of eigen server) kunt aansluiten. */
interface AiClient {
    suspend fun generate(systemPrompt: String, history: List<Turn>): String

    /** Zet een audiobestand om naar tekst volgens [instruction]. */
    suspend fun transcribe(audio: File, mimeType: String, instruction: String): String
}

/** Praat via de REST-API met Google Gemini. Geen extra libraries nodig. */
class GeminiClient(
    private val apiKey: String,
    private val model: String,
    private val temperature: Double = 0.3
) : AiClient {

    private class Response(val code: Int, val body: String, val headers: Map<String, List<String>>) {
        fun header(name: String) = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
    }

    override suspend fun generate(systemPrompt: String, history: List<Turn>): String =
        withContext(Dispatchers.IO) {
            requireKey()
            val body = JSONObject().apply {
                put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                put("contents", JSONArray().apply {
                    history.forEach { turn ->
                        put(JSONObject().apply {
                            put("role", if (turn.fromUser) "user" else "model")
                            put("parts", JSONArray().put(JSONObject().put("text", turn.text)))
                        })
                    }
                })
                put("generationConfig", JSONObject().put("temperature", temperature))
            }
            parseText(postJson(generateUrl(), body, readTimeoutMs = 90_000).body)
        }

    override suspend fun transcribe(audio: File, mimeType: String, instruction: String): String =
        withContext(Dispatchers.IO) {
            requireKey()
            val fileName = uploadFile(audio, mimeType)
            try {
                val fileUri = waitUntilActive(fileName)
                val body = JSONObject().apply {
                    put("contents", JSONArray().put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray()
                            .put(JSONObject().put("file_data", JSONObject().put("mime_type", mimeType).put("file_uri", fileUri)))
                            .put(JSONObject().put("text", instruction)))
                    }))
                    put("generationConfig", JSONObject().put("temperature", 0.1))
                }
                parseText(postJson(generateUrl(), body, readTimeoutMs = 600_000).body)
            } finally {
                // Opname direct weer bij Google verwijderen (anders blijft hij 48 uur staan).
                runCatching { request("DELETE", "$BASE/v1beta/$fileName") }
            }
        }

    /** Uploadt via de Files API (resumable) en geeft de naam terug, bijv. "files/abc123". */
    private fun uploadFile(file: File, mimeType: String): String {
        val start = request(
            method = "POST",
            url = "$BASE/upload/v1beta/files",
            headers = mapOf(
                "X-Goog-Upload-Protocol" to "resumable",
                "X-Goog-Upload-Command" to "start",
                "X-Goog-Upload-Header-Content-Length" to file.length().toString(),
                "X-Goog-Upload-Header-Content-Type" to mimeType,
                "Content-Type" to "application/json"
            ),
            body = JSONObject().put("file", JSONObject().put("display_name", "gesprek")).toString().toByteArray()
        )
        val uploadUrl = start.header("x-goog-upload-url")
            ?: throw AiException("Uploaden van de opname is niet gelukt. Probeer het opnieuw.")

        val upload = request(
            method = "POST",
            url = uploadUrl,
            headers = mapOf("X-Goog-Upload-Offset" to "0", "X-Goog-Upload-Command" to "upload, finalize"),
            file = file,
            readTimeoutMs = 300_000
        )
        return JSONObject(upload.body).getJSONObject("file").getString("name")
    }

    /** Wacht tot Google de audio verwerkt heeft en geeft de file-URI terug. */
    private suspend fun waitUntilActive(fileName: String): String {
        repeat(60) {
            val info = JSONObject(request("GET", "$BASE/v1beta/$fileName").body)
            when (info.optString("state")) {
                "ACTIVE" -> return info.getString("uri")
                "FAILED" -> throw AiException("Google kon de opname niet verwerken.")
            }
            delay(2_000)
        }
        throw AiException("Het verwerken van de opname duurt te lang. Probeer het later opnieuw.")
    }

    private fun generateUrl(): String =
        "$BASE/v1beta/models/${URLEncoder.encode(model, "UTF-8")}:generateContent"

    private fun requireKey() {
        if (apiKey.isBlank()) {
            throw AiException("Er is nog geen API-sleutel ingesteld. Een begeleider kan dit doen via Instellingen.")
        }
    }

    private fun postJson(url: String, json: JSONObject, readTimeoutMs: Int): Response =
        request(
            method = "POST",
            url = url,
            headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
            body = json.toString().toByteArray(Charsets.UTF_8),
            readTimeoutMs = readTimeoutMs
        )

    private fun request(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: ByteArray? = null,
        file: File? = null,
        readTimeoutMs: Int = 60_000
    ): Response {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("x-goog-api-key", apiKey)
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            when {
                file != null -> {
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(file.length())
                    connection.outputStream.use { out -> file.inputStream().use { it.copyTo(out) } }
                }
                body != null -> {
                    connection.doOutput = true
                    connection.setFixedLengthStreamingMode(body.size)
                    connection.outputStream.use { it.write(body) }
                }
            }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw AiException(describeError(code, text))
            return Response(code, text, connection.headerFields.filterKeys { it != null })
        } catch (e: IOException) {
            throw AiException("Geen verbinding met de AI. Controleer je internet en probeer het opnieuw.")
        } finally {
            connection.disconnect()
        }
    }

    private fun parseText(response: String): String {
        val json = JSONObject(response)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            ?: throw AiException("De AI gaf geen antwoord. Probeer je vraag anders te formuleren.")
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val text = buildString {
            if (parts != null) for (i in 0 until parts.length()) append(parts.optJSONObject(i)?.optString("text").orEmpty())
        }.trim()
        if (text.isEmpty()) {
            val reason = candidate.optString("finishReason")
            throw AiException(
                if (reason == "SAFETY") "Op deze vraag kan ik geen antwoord geven. Vraag het aan de begeleiding."
                else "De AI gaf een leeg antwoord. Probeer het opnieuw."
            )
        }
        return text
    }

    private fun describeError(code: Int, response: String): String {
        val apiMessage = runCatching { JSONObject(response).getJSONObject("error").getString("message") }.getOrNull()
        return when (code) {
            400 -> "De aanvraag werd geweigerd (400). Klopt de API-sleutel en het model? ${apiMessage.orEmpty()}"
            401, 403 -> "De API-sleutel is ongeldig of heeft geen toegang (fout $code)."
            404 -> "Het model '$model' bestaat niet (meer). Pas het model aan in Instellingen."
            413 -> "De opname is te groot om te versturen."
            429 -> "Te veel vragen tegelijk of het tegoed is op. Probeer het over een minuutje opnieuw."
            in 500..599 -> "De AI-dienst heeft even een storing (fout $code). Probeer het later opnieuw."
            else -> "Onbekende fout ($code). ${apiMessage.orEmpty()}"
        }.trim()
    }

    private companion object {
        const val BASE = "https://generativelanguage.googleapis.com"
    }
}
