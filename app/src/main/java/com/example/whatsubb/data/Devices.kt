package com.example.whatsubb.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Device(val id: String, val name: String, val text: String, val addedAt: Long)

/**
 * Apparaten op de afdeling (wasmachine, droger, koffieapparaat, kookplaat...).
 * Van elke foto wordt alleen de uitgelezen beschrijving bewaard, niet de foto zelf.
 */
class DeviceStore(context: Context) {

    private val file = File(context.filesDir, "apparaten.json")

    fun list(): List<Device> {
        if (!file.exists()) return emptyList()
        val array = runCatching { JSONArray(file.readText()) }.getOrElse { return emptyList() }
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                Device(o.getString("id"), o.getString("name"), o.getString("text"), o.getLong("addedAt"))
            }.getOrNull()
        }.sortedBy { it.name.lowercase() }
    }

    fun add(name: String, text: String): Device {
        val device = Device(UUID.randomUUID().toString(), name.trim(), text.trim(), System.currentTimeMillis())
        writeAll(list() + device)
        return device
    }

    /** Info van een extra foto (bijv. de achterkant of een handleiding) bij een bestaand apparaat. */
    fun append(id: String, text: String) =
        writeAll(list().map { if (it.id == id) it.copy(text = it.text + "\n\n" + text.trim()) else it })

    fun delete(id: String) = writeAll(list().filterNot { it.id == id })

    /** Tekst voor de AI, met per apparaat een kopje. */
    fun promptText(): String =
        list().joinToString("\n\n") { "## Apparaat: ${it.name}\n\n${it.text}" }

    private fun writeAll(devices: List<Device>) {
        val array = JSONArray()
        devices.forEach { d ->
            array.put(JSONObject().put("id", d.id).put("name", d.name).put("text", d.text).put("addedAt", d.addedAt))
        }
        file.writeText(array.toString())
    }

    companion object {
        fun instruction(name: String) = """
            Dit is een foto van een apparaat op een woonafdeling: "$name".
            Bewoners (jongvolwassenen met een licht verstandelijke beperking of autisme) gaan de AI vragen hoe ze dit
            apparaat gebruiken. Beschrijf daarom in Markdown wat je op de foto ziet:
            - Soort apparaat, en merk en type als die zichtbaar zijn.
            - Alle knoppen, draaiknoppen, standen, programma's en symbolen: waar ze zitten (bijv. "linksboven",
              "de grote draaiknop") en wat ze doen. Schrijf teksten en getallen precies over zoals ze erop staan.
              Leg de betekenis van symbolen uit.
            - Hoe je het apparaat stap voor stap gebruikt voor het gewone gebruik (bijv. een normale was draaien,
              een kop koffie zetten, een pan verwarmen), in korte, genummerde stappen.
            - Waar je op moet letten voor de veiligheid, en wat je moet doen als het apparaat een foutmelding geeft,
              als dat te zien is.
            Baseer je op wat je ziet. Herken je het model en weet je zeker hoe het werkt, dan mag je algemene uitleg
            toevoegen onder het kopje "Algemene uitleg (niet van de foto)". Verzin niets.
            Is iets onleesbaar, schrijf dan [onleesbaar]. Staan er mensen of persoonlijke gegevens op de foto,
            neem die dan niet over. Geef alleen de beschrijving, zonder inleiding.
        """.trimIndent()
    }
}
