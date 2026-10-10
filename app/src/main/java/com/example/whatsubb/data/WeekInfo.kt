package com.example.whatsubb.data

import android.content.Context
import com.example.whatsubb.util.Weeks
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class WeekInfoType(val id: String, val title: String, val emoji: String, val instruction: String) {
    MENU(
        id = "menu",
        title = "Weekmenu",
        emoji = "🍽️",
        instruction = """
            Dit is een foto van het weekmenu van een zorgafdeling.
            Schrijf het menu volledig uit in Markdown: per dag (maandag t/m zondag) een kopje met alle maaltijden en gerechten.
            Neem informatie over dieet, allergenen of alternatieven over als die er staat.
            Verzin niets. Is iets onleesbaar, schrijf dan [onleesbaar]. Geef alleen het menu, zonder inleiding.
        """.trimIndent()
    ),
    ROOSTER(
        id = "rooster",
        title = "Werkrooster",
        emoji = "👥",
        instruction = """
            Dit is een foto van het werkrooster van de begeleiding van een zorgafdeling.
            Schrijf in Markdown per dag (maandag t/m zondag) uit wie er werkt en in welke dienst
            (bijv. vroege, late, nacht), met tijden als die op het rooster staan.
            Schrijf de namen precies zoals ze op het rooster staan. Neem geen telefoonnummers of andere privégegevens over.
            Verzin niets. Is iets onleesbaar, schrijf dan [onleesbaar]. Geef alleen het rooster, zonder inleiding.
        """.trimIndent()
    );

    companion object {
        fun fromId(id: String) = entries.first { it.id == id }
    }
}

data class WeekInfoEntry(
    val type: WeekInfoType,
    val weekStart: Long,
    val weekEnd: Long,
    val text: String,
    val addedAt: Long
)

/**
 * Weekmenu en werkrooster, per week. Alleen de uitgelezen tekst wordt bewaard (niet de foto).
 * Info van een week die voorbij is wordt automatisch verwijderd.
 */
class WeekInfoStore(context: Context) {

    private val file = File(context.filesDir, "weekinfo.json")

    /** Alle actuele en toekomstige weekinfo; verlopen info wordt eerst verwijderd. */
    fun current(now: Long = System.currentTimeMillis()): List<WeekInfoEntry> {
        val all = readAll()
        val valid = all.filter { it.weekEnd > now }
        if (valid.size != all.size) writeAll(valid)
        return valid.sortedWith(compareBy({ it.type.ordinal }, { it.weekStart }))
    }

    /** Voegt info toe; bestaande info van hetzelfde type en dezelfde week wordt vervangen. */
    fun put(type: WeekInfoType, weekStart: Long, text: String) {
        val others = current().filterNot { it.type == type && it.weekStart == weekStart }
        writeAll(others + WeekInfoEntry(type, weekStart, Weeks.end(weekStart), text, System.currentTimeMillis()))
    }

    fun delete(type: WeekInfoType, weekStart: Long) =
        writeAll(current().filterNot { it.type == type && it.weekStart == weekStart })

    /** Tekst voor de AI, met per blok de week waarvoor het geldt. */
    fun promptText(now: Long = System.currentTimeMillis()): String =
        current(now).joinToString("\n\n") { entry ->
            val status = if (entry.weekStart <= now) "deze week" else "volgende week"
            "## ${entry.type.title} ($status: ${Weeks.label(entry.weekStart)})\n\n${entry.text.trim()}"
        }

    private fun readAll(): List<WeekInfoEntry> {
        if (!file.exists()) return emptyList()
        val array = runCatching { JSONArray(file.readText()) }.getOrElse { return emptyList() }
        return (0 until array.length()).mapNotNull { i ->
            runCatching {
                val o = array.getJSONObject(i)
                WeekInfoEntry(
                    WeekInfoType.fromId(o.getString("type")),
                    o.getLong("weekStart"), o.getLong("weekEnd"),
                    o.getString("text"), o.getLong("addedAt")
                )
            }.getOrNull()
        }
    }

    private fun writeAll(entries: List<WeekInfoEntry>) {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject().put("type", e.type.id).put("weekStart", e.weekStart).put("weekEnd", e.weekEnd)
                    .put("text", e.text).put("addedAt", e.addedAt)
            )
        }
        file.writeText(array.toString())
    }
}
