package com.example.whatsubb.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.whatsubb.ai.AiClient
import com.example.whatsubb.ai.AiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Haalt de tekst uit een geüpload document (PDF, Word .docx, .txt of .md). */
object DocumentImporter {

    const val MIME_PDF = "application/pdf"
    const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    val supportedMimeTypes = arrayOf(MIME_PDF, MIME_DOCX, "text/plain", "text/markdown", "text/x-markdown")

    private const val PDF_INSTRUCTION = """
Dit document bevat regels, afspraken of werkwijzen van een zorgafdeling.
Geef de VOLLEDIGE tekst van het document letterlijk weer in Markdown (kopjes met #, opsommingen met -).
Vat niets samen en laat niets weg. Tabellen mag je als Markdown-tabel weergeven.
Staan er namen of andere gegevens van bewoners in, vervang die dan door [naam] of [gegeven].
Geef alleen de tekst van het document, zonder inleiding.
"""

    data class Imported(val name: String, val text: String)

    suspend fun import(context: Context, uri: Uri, ai: AiClient): Imported = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val name = displayName(context, uri) ?: "document"
        val mime = resolver.getType(uri) ?: guessMime(name)

        val text = when {
            mime == MIME_PDF -> {
                // PDF-tekst laten uitlezen door Gemini (werkt ook voor scans).
                val temp = File.createTempFile("import", ".pdf", context.cacheDir)
                try {
                    resolver.openInputStream(uri)!!.use { input -> temp.outputStream().use { input.copyTo(it) } }
                    ai.processFile(temp, MIME_PDF, PDF_INSTRUCTION)
                } finally {
                    temp.delete()
                }
            }
            mime == MIME_DOCX -> resolver.openInputStream(uri)!!.use(::docxToText)
            mime.startsWith("text/") -> resolver.openInputStream(uri)!!.use { it.bufferedReader().readText() }
            else -> throw AiException("Dit bestandstype wordt niet ondersteund. Gebruik PDF, Word (.docx) of tekst.")
        }.trim()

        if (text.isEmpty()) throw AiException("Er is geen tekst gevonden in dit document.")
        Imported(name, text)
    }

    /** Leest de tekst uit word/document.xml in een .docx-bestand. */
    fun docxToText(input: InputStream): String {
        val xml = ZipInputStream(input).use { zip ->
            generateSequence { zip.nextEntry }
                .firstOrNull { it.name == "word/document.xml" }
                ?.let { zip.readBytes().toString(Charsets.UTF_8) }
        } ?: throw AiException("Dit Word-bestand kan niet gelezen worden.")

        return xml
            .replace(Regex("""<w:tab\s*/>"""), "\t")
            .replace(Regex("""<w:br\s*/>"""), "\n")
            .replace("</w:p>", "\n")
            .replace(Regex("""<[^>]+>"""), "")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&")
            .lines().joinToString("\n") { it.trimEnd() }
            .replace(Regex("""\n{3,}"""), "\n\n")
    }

    private fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "pdf" -> MIME_PDF
        "docx" -> MIME_DOCX
        "txt" -> "text/plain"
        "md" -> "text/markdown"
        else -> "application/octet-stream"
    }
}
