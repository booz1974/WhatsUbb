package com.example.whatsubb.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class KnowledgeDoc(val id: String, val name: String, val addedAt: Long, val chars: Int)

/**
 * Geüploade documenten met regels, afspraken en werkwijze.
 * Van elk document wordt alleen de tekst bewaard (in de privé-opslag van de app).
 */
class KnowledgeDocuments(context: Context) {

    private val dir = File(context.filesDir, "kennis_docs").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    fun list(): List<KnowledgeDoc> {
        if (!indexFile.exists()) return emptyList()
        val array = runCatching { JSONArray(indexFile.readText()) }.getOrElse { return emptyList() }
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            KnowledgeDoc(o.getString("id"), o.getString("name"), o.getLong("addedAt"), o.getInt("chars"))
        }
    }

    fun add(name: String, text: String): KnowledgeDoc {
        val doc = KnowledgeDoc(UUID.randomUUID().toString(), name, System.currentTimeMillis(), text.length)
        File(dir, "${doc.id}.md").writeText(text)
        saveIndex(list() + doc)
        return doc
    }

    fun delete(id: String) {
        File(dir, "$id.md").delete()
        saveIndex(list().filterNot { it.id == id })
    }

    fun text(id: String): String = File(dir, "$id.md").takeIf { it.exists() }?.readText().orEmpty()

    /** Alle documenten achter elkaar, met de bestandsnaam als kopje. */
    fun combinedText(): String =
        list().joinToString("\n\n") { doc -> "## Document: ${doc.name}\n\n${text(doc.id).trim()}" }

    fun totalChars(): Int = list().sumOf { it.chars }

    private fun saveIndex(docs: List<KnowledgeDoc>) {
        val array = JSONArray()
        docs.forEach { doc ->
            array.put(JSONObject().put("id", doc.id).put("name", doc.name).put("addedAt", doc.addedAt).put("chars", doc.chars))
        }
        indexFile.writeText(array.toString())
    }
}
