package com.example.whatsubb

import com.example.whatsubb.util.DocumentImporter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DocxImportTest {

    @Test
    fun extractsParagraphsFromDocx() {
        val xml = """<w:document><w:body>
            <w:p><w:r><w:t>Huisregels</w:t></w:r></w:p>
            <w:p><w:r><w:t>Bezoek tot 21:00 &amp; rust na 22:30</w:t></w:r></w:p>
            </w:body></w:document>"""
        val bytes = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                zip.putNextEntry(ZipEntry("[Content_Types].xml")); zip.write("<Types/>".toByteArray()); zip.closeEntry()
                zip.putNextEntry(ZipEntry("word/document.xml")); zip.write(xml.toByteArray()); zip.closeEntry()
            }
        }.toByteArray()

        val text = DocumentImporter.docxToText(ByteArrayInputStream(bytes))
        assertEquals(listOf("Huisregels", "Bezoek tot 21:00 & rust na 22:30"), text.lines().map { it.trim() }.filter { it.isNotEmpty() })
    }
}
