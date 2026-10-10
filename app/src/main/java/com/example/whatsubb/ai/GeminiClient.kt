package com.example.whatsubb.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Turn(val fromUser: Boolean, val text: String)

class AiException(message: String) : Exception(message)

/** Eén interface, zodat je later makkelijk een andere AI (of eigen server) kunt aansluiten. */
interface AiClient {
    suspend fun generate(systemPrompt: String, history: List<Turn>): String
}

/** Praat via de REST-API met Google Gemini. Geen extra libraries nodig. */
class GeminiClient(
    private val apiKey: String,
    private val model: String,
    private val temperature: Double = 0.3
) : AiClient {

    override suspend fun generate(systemPrompt: String, history: List<Turn>): String =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                throw AiException("Er is nog geen API-sleutel ingesteld. Een begeleider kan dit doen via Instellingen.")
            }

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

            val modelPath = URLEncoder.encode(model, "UTF-8")
            val connection = URL("https://generativelanguage.googleapis.com/v1beta/models/$modelPath:generateContent")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15_000
                connection.readTimeout = 90_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("x-goog-api-key", apiKey)
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val response = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()

                if (code !in 200..299) throw AiException(describeError(code, response))
                parseText(response)
            } catch (e: java.io.IOException) {
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
            429 -> "Te veel vragen tegelijk of het tegoed is op. Probeer het over een minuutje opnieuw."
            in 500..599 -> "De AI-dienst heeft even een storing (fout $code). Probeer het later opnieuw."
            else -> "Onbekende fout ($code). ${apiMessage.orEmpty()}"
        }.trim()
    }
}
