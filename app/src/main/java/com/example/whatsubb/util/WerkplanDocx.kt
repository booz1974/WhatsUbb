package com.example.whatsubb.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.whatsubb.R
import com.example.whatsubb.data.Werkplan
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Maakt een Word-document (.docx) van een werkplan, in dezelfde opbouw als WerkplanPdf:
 * werkplan voor het team, aftekenkaart voor de bewoner en evaluatieformulier, elk op een eigen pagina
 * met het Whats Ubb-logo. Een .docx is een zip met XML; er is geen extra library nodig.
 */
object WerkplanDocx {

    const val MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    private const val CONTENT_WIDTH = 9906 // A4 (11906 twips) min 2 × 1000 marge
    private const val BLUE = "2F5496"
    private const val SHADE = "ECECEC"

    private class Cell(
        val text: String = "",
        val bold: Boolean = false,
        val shaded: Boolean = false,
        val size: Int = 19, // halve punten
        val center: Boolean = false
    )

    private class Row(val cells: List<Cell>, val minHeight: Int = 0) // minHeight in twips

    fun write(context: Context, out: OutputStream, plan: Werkplan) {
        val logo = logoPng(context)
        val body = StringBuilder()
        Builder(body, logo?.second).apply {
            teamPart(plan)
            cardPart(plan)
            evaluationPart(plan)
        }
        ZipOutputStream(out).use { zip ->
            zip.entry("[Content_Types].xml", CONTENT_TYPES)
            zip.entry("_rels/.rels", ROOT_RELS)
            zip.entry("word/_rels/document.xml.rels", DOCUMENT_RELS)
            zip.entry("word/styles.xml", STYLES)
            zip.entry("word/footer1.xml", footer("UBB2  •  Werkplan ${plan.taak.lowercase()}"))
            zip.entry("word/document.xml", document(body.toString()))
            logo?.let { zip.putNextEntry(ZipEntry("word/media/logo.png")); zip.write(it.first); zip.closeEntry() }
        }
    }

    // ---------- Inhoud (zelfde opbouw als de PDF) ----------

    private fun Builder.teamPart(plan: Werkplan) {
        logo()
        title("WERKPLAN", plan.taak)
        table(
            intArrayOf(22, 28, 22, 28),
            listOf(
                infoRow("Bewoner", "[bewoner]", "Frequentie", plan.frequentie),
                infoRow("Vaste dag", plan.vasteDag, "Vaste tijd", plan.vasteTijd),
                infoRow("Begeleider", plan.begeleider, "Duur", plan.duur),
                infoRow("Startdatum", plan.startdatum, "Evaluatiedatum", plan.evaluatiedatum)
            )
        )
        heading("1. Doel")
        paragraph(plan.doel)
        heading("2. Uitgangspunten voor het team")
        plan.uitgangspunten.forEach { listItem("•", it) }
        heading("3. Vaste werkwijze")
        table(
            intArrayOf(20, 28, 26, 26),
            listOf(headerRow("Stap", "Wat doen we?", "Rol bewoner", "Rol begeleiding")) +
                plan.werkwijze.mapIndexed { i, s -> Row(listOf(Cell("${i + 1}. ${s.stap}"), Cell(s.wat), Cell(s.bewoner), Cell(s.begeleiding))) }
        )
        heading("4. ${plan.volgordeTitel}")
        plan.volgorde.forEachIndexed { i, s -> listItem("${i + 1}.", s) }
        heading("5. Begeleidingsniveau")
        paragraph("Gebruik onderstaande schaal om de hoeveelheid ondersteuning eenduidig te beschrijven:")
        table(
            intArrayOf(40, 60),
            listOf(headerRow("Niveau", "Betekenis")) +
                Werkplan.BEGELEIDINGSNIVEAUS.map { (level, meaning) -> Row(listOf(Cell(level, bold = true), Cell(meaning))) }
        )
        heading("6. Eindcontrole")
        paragraph("Gebruik deze lijst bij het afronden. Vink af wat klaar is.")
        plan.eindcontrole.forEach { listItem("☐", it) }
        heading("7. Rapportage na afloop")
        table(
            intArrayOf(40, 60),
            listOf(
                labelRow("Datum", ""),
                labelRow("Moment", "☐ uitgevoerd     ☐ niet uitgevoerd"),
                labelRow("Begeleidingsniveau", "0     1     2     3     4     5"),
                openRow("Wat ging goed?"),
                openRow("Waar was hulp bij nodig?"),
                openRow("Aandacht voor volgende keer")
            )
        )
        if (plan.teamafspraak.isNotBlank()) {
            paragraph(plan.teamafspraak, boldStart = "Teamafspraak: ", spaceBefore = 200)
        }
    }

    private fun Builder.cardPart(plan: Werkplan) {
        logo(newPage = true)
        title(plan.kaart.titel, "UBB2  •  ${plan.frequentie}".trimEnd(' ', '•'))
        table(
            intArrayOf(22, 28, 22, 28),
            listOf(
                infoRow("Mijn naam", "", "Mijn vaste dag", plan.vasteDag),
                infoRow("Starttijd", plan.vasteTijd, "Samen met", "")
            )
        )
        heading("Ik doe het stap voor stap")
        table(
            intArrayOf(10, 10, 36, 44),
            listOf(headerRow("Klaar", "", "Taak", "Wat doe ik?")) +
                plan.kaart.taken.map { t ->
                    Row(
                        listOf(Cell("☐", size = 28, center = true), Cell(t.emoji, size = 28, center = true), Cell(t.taak, bold = true, size = 22), Cell(t.watDoeIk)),
                        minHeight = 520
                    )
                }
        )
        paragraph(plan.kaart.hulp, boldStart = "Hulp nodig? ", center = true, size = 22, spaceBefore = 240)
        paragraph("", boldStart = "Mijn doel: ${plan.kaart.doel}", center = true, size = 26)
        heading("Zo ging het")
        table(
            intArrayOf(20, 30, 25, 25),
            listOf(headerRow("Datum", "Hoe ging het?", "Mijn handtekening", "Begeleider")) +
                List(3) { Row(listOf(Cell(), Cell("🙂     😐     🙁", size = 26, center = true), Cell(), Cell()), minHeight = 520) }
        )
    }

    private fun Builder.evaluationPart(plan: Werkplan) {
        logo(newPage = true)
        title("EVALUATIE", "Werkplan ${plan.taak.lowercase()}")
        table(
            intArrayOf(22, 28, 22, 28),
            listOf(
                infoRow("Bewoner", "[bewoner]", "Evaluatiedatum", plan.evaluatiedatum),
                infoRow("Startdatum", plan.startdatum, "Aanwezig (rollen)", "")
            )
        )
        heading("1. Doel van het werkplan")
        paragraph(plan.doel)
        heading("2. Hoe ging het?")
        table(
            intArrayOf(40, 60),
            listOf(
                labelRow("Is het doel bereikt?", "☐ ja     ☐ deels     ☐ nee"),
                labelRow("Begeleidingsniveau bij de start", "0     1     2     3     4     5"),
                labelRow("Begeleidingsniveau nu", "0     1     2     3     4     5"),
                openRow("Wat ging goed?"),
                openRow("Wat was lastig?"),
                openRow("Wat zegt de bewoner zelf?"),
                openRow("Wat passen we aan?")
            )
        )
        heading("3. Vervolg")
        table(
            intArrayOf(40, 60),
            listOf(
                labelRow("Werkplan", "☐ doorgaan     ☐ aanpassen     ☐ afsluiten"),
                labelRow("Volgende evaluatiedatum", "")
            )
        )
        paragraph("")
        table(
            intArrayOf(25, 37, 38),
            listOf(
                headerRow("Datum", "Handtekening bewoner", "Handtekening begeleider"),
                Row(listOf(Cell(), Cell(), Cell()), minHeight = 800)
            )
        )
    }

    private fun infoRow(l1: String, v1: String, l2: String, v2: String) =
        Row(listOf(Cell(l1, bold = true, shaded = true), Cell(v1), Cell(l2, bold = true, shaded = true), Cell(v2)))

    private fun headerRow(vararg titles: String) = Row(titles.map { Cell(it, bold = true, shaded = true) })

    private fun labelRow(label: String, value: String) = Row(listOf(Cell(label, bold = true, shaded = true), Cell(value)))

    /** Rij met ruimte om met de hand te schrijven. */
    private fun openRow(label: String) = Row(listOf(Cell(label, bold = true), Cell()), minHeight = 1000)

    // ---------- WordprocessingML ----------

    private class Builder(private val xml: StringBuilder, private val logoSize: Pair<Int, Int>?) {
        private var imageId = 1

        /** [newPage]: dit deel begint op een nieuwe pagina (zonder extra lege alinea die kan doorschuiven). */
        fun logo(newPage: Boolean = false) {
            val (w, h) = logoSize ?: return
            val cx = 64 * 12700L // 64 pt breed, in EMU
            val cy = cx * h / w
            val id = imageId++
            xml.append(
                "<w:p><w:pPr>" + (if (newPage) "<w:pageBreakBefore/>" else "") + """<w:jc w:val="center"/><w:spacing w:after="60"/></w:pPr><w:r><w:drawing>""" +
                    """<wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="$cx" cy="$cy"/><wp:docPr id="$id" name="Logo $id"/>""" +
                    """<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">""" +
                    """<pic:pic xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:nvPicPr><pic:cNvPr id="$id" name="logo.png"/><pic:cNvPicPr/></pic:nvPicPr>""" +
                    """<pic:blipFill><a:blip r:embed="rIdLogo"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>""" +
                    """<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr>""" +
                    """</pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r></w:p>"""
            )
        }

        fun title(title: String, subtitle: String) {
            xml.append(p(run(title, bold = true, size = 44), center = true, after = 80))
            xml.append(p(run(subtitle, bold = true, size = 28), center = true, after = 240))
        }

        fun heading(text: String) {
            xml.append("""<w:p><w:pPr><w:keepNext/><w:spacing w:before="280" w:after="80"/></w:pPr>""")
            xml.append(run(text, bold = true, size = 27, color = BLUE)).append("</w:p>")
        }

        fun paragraph(text: String, boldStart: String = "", center: Boolean = false, size: Int = 20, spaceBefore: Int = 0) {
            val runs = (if (boldStart.isNotEmpty()) run(boldStart, bold = true, size = size) else "") + run(text, size = size)
            xml.append(p(runs, center = center, before = spaceBefore, after = 60))
        }

        /** Opsomming met een hangend label ("•", "1." of een aftekenvakje). */
        fun listItem(label: String, text: String) {
            xml.append("""<w:p><w:pPr><w:spacing w:after="40"/><w:ind w:left="360" w:hanging="360"/></w:pPr>""")
            xml.append(run(label, size = if (label == "☐") 24 else 20)).append("<w:r><w:tab/></w:r>")
            xml.append(run(text, size = 20)).append("</w:p>")
        }


        fun table(percentages: IntArray, rows: List<Row>) {
            val widths = percentages.map { CONTENT_WIDTH * it / 100 }
            xml.append("""<w:tbl><w:tblPr><w:tblW w:w="$CONTENT_WIDTH" w:type="dxa"/><w:tblLayout w:type="fixed"/><w:tblBorders>""")
            listOf("top", "left", "bottom", "right", "insideH", "insideV").forEach {
                xml.append("""<w:$it w:val="single" w:sz="4" w:space="0" w:color="222222"/>""")
            }
            xml.append("""</w:tblBorders><w:tblCellMar><w:top w:w="60" w:type="dxa"/><w:left w:w="90" w:type="dxa"/>""")
            xml.append("""<w:bottom w:w="60" w:type="dxa"/><w:right w:w="90" w:type="dxa"/></w:tblCellMar></w:tblPr><w:tblGrid>""")
            widths.forEach { xml.append("""<w:gridCol w:w="$it"/>""") }
            xml.append("</w:tblGrid>")
            rows.forEach { row ->
                xml.append("<w:tr><w:trPr><w:cantSplit/>")
                if (row.minHeight > 0) xml.append("""<w:trHeight w:val="${row.minHeight}" w:hRule="atLeast"/>""")
                xml.append("</w:trPr>")
                row.cells.forEachIndexed { i, c ->
                    xml.append("""<w:tc><w:tcPr><w:tcW w:w="${widths[i]}" w:type="dxa"/>""")
                    if (c.shaded) xml.append("""<w:shd w:val="clear" w:color="auto" w:fill="$SHADE"/>""")
                    if (row.minHeight > 0 && (c.center || c.text == "☐")) xml.append("""<w:vAlign w:val="center"/>""")
                    xml.append("</w:tcPr>")
                    xml.append(p(if (c.text.isEmpty()) "" else run(c.text, bold = c.bold, size = c.size), center = c.center, after = 0))
                    xml.append("</w:tc>")
                }
                xml.append("</w:tr>")
            }
            xml.append("</w:tbl>")
        }

        private fun p(runs: String, center: Boolean = false, before: Int = 0, after: Int = 60) =
            "<w:p><w:pPr><w:spacing w:before=\"$before\" w:after=\"$after\"/>" +
                (if (center) "<w:jc w:val=\"center\"/>" else "") + "</w:pPr>$runs</w:p>"

        private fun run(text: String, bold: Boolean = false, size: Int = 20, color: String? = null): String {
            val props = (if (bold) "<w:b/>" else "") + (color?.let { "<w:color w:val=\"$it\"/>" } ?: "") +
                "<w:sz w:val=\"$size\"/><w:szCs w:val=\"$size\"/>"
            return "<w:r><w:rPr>$props</w:rPr><w:t xml:space=\"preserve\">${escape(text)}</w:t></w:r>"
        }
    }

    private fun escape(text: String) = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    /** Het logo als (kleine) PNG, met de oorspronkelijke breedte en hoogte. */
    private fun logoPng(context: Context): Pair<ByteArray, Pair<Int, Int>>? {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.whats_ubb_logo) ?: return null
        val scaled = if (bitmap.width > 300) Bitmap.createScaledBitmap(bitmap, 300, 300 * bitmap.height / bitmap.width, true) else bitmap
        val bytes = ByteArrayOutputStream().also { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        return bytes to (scaled.width to scaled.height)
    }

    private fun ZipOutputStream.entry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun document(body: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" """ +
            """xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" """ +
            """xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"><w:body>""" +
            body +
            """<w:sectPr><w:footerReference w:type="default" r:id="rIdFooter"/><w:pgSz w:w="11906" w:h="16838"/>""" +
            """<w:pgMar w:top="900" w:right="1000" w:bottom="900" w:left="1000" w:header="500" w:footer="500" w:gutter="0"/></w:sectPr>""" +
            "</w:body></w:document>"

    private fun footer(text: String) =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:p><w:pPr><w:jc w:val="center"/></w:pPr>""" +
            """<w:r><w:rPr><w:color w:val="808080"/><w:sz w:val="16"/></w:rPr><w:t xml:space="preserve">${escape(text)}</w:t></w:r></w:p></w:ftr>"""

    private const val CONTENT_TYPES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""" +
            """<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""" +
            """<Default Extension="xml" ContentType="application/xml"/>""" +
            """<Default Extension="png" ContentType="image/png"/>""" +
            """<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>""" +
            """<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>""" +
            """<Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>""" +
            "</Types>"

    private const val ROOT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>""" +
            "</Relationships>"

    private const val DOCUMENT_RELS =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
            """<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" +
            """<Relationship Id="rIdFooter" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>""" +
            """<Relationship Id="rIdLogo" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/logo.png"/>""" +
            "</Relationships>"

    private const val STYLES =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""" +
            """<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""" +
            """<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="Calibri" w:cs="Calibri"/>""" +
            """<w:color w:val="222222"/><w:sz w:val="20"/><w:szCs w:val="20"/><w:lang w:val="nl-NL"/></w:rPr></w:rPrDefault>""" +
            """<w:pPrDefault><w:pPr><w:spacing w:after="60" w:line="264" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>""" +
            """<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>""" +
            "</w:styles>"
}
