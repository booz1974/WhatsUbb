package com.example.whatsubb.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import com.example.whatsubb.R
import com.example.whatsubb.data.Werkplan
import java.io.OutputStream

/**
 * Tekent een werkplan in de vaste UBB2-stijl (naar het voorbeeld "Werkplan kamer schoonmaken"):
 * werkplan voor het team, daarna op een nieuwe pagina de aftekenkaart voor de bewoner,
 * en als laatste het evaluatieformulier. Elk deel begint met het Whats Ubb-logo.
 */
object WerkplanPdf {

    private const val W = 595f   // A4 in punten
    private const val H = 842f
    private const val MARGIN = 50f
    private const val BOTTOM = 50f
    private const val PAD = 4f
    private const val CONTENT = W - 2 * MARGIN

    private val black = Color.rgb(0x22, 0x22, 0x22)
    private val headingBlue = Color.rgb(0x2F, 0x54, 0x96)
    private val shade = Color.rgb(0xEC, 0xEC, 0xEC)

    /** Een tabelcel. [checkbox] tekent een leeg aftekenvakje vóór de tekst. */
    private class Cell(
        val text: CharSequence = "",
        val bold: Boolean = false,
        val shaded: Boolean = false,
        val checkbox: Boolean = false,
        val size: Float = 9.5f,
        val center: Boolean = false
    )

    private class Row(val cells: List<Cell>, val minHeight: Float = 0f)

    fun write(context: Context, out: OutputStream, plan: Werkplan) {
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.whats_ubb_logo)
        val pdf = Pager("UBB2  •  Werkplan ${plan.taak.lowercase()}")
        teamPart(pdf, logo, plan)
        cardPart(pdf, logo, plan)
        evaluationPart(pdf, logo, plan)
        pdf.finish()
        pdf.doc.writeTo(out)
        pdf.doc.close()
    }

    // ---------- Deel 1: werkplan voor het team ----------

    private fun teamPart(pdf: Pager, logo: Bitmap?, plan: Werkplan) {
        pdf.newPage()
        pdf.logo(logo)
        pdf.title("WERKPLAN", plan.taak)
        pdf.table(
            floatArrayOf(0.22f, 0.28f, 0.22f, 0.28f),
            listOf(
                infoRow("Bewoner", "[bewoner]", "Frequentie", plan.frequentie),
                infoRow("Vaste dag", plan.vasteDag, "Vaste tijd", plan.vasteTijd),
                infoRow("Begeleider", plan.begeleider, "Duur", plan.duur),
                infoRow("Startdatum", plan.startdatum, "Evaluatiedatum", plan.evaluatiedatum)
            )
        )

        pdf.heading("1. Doel")
        pdf.paragraph(plan.doel)

        pdf.heading("2. Uitgangspunten voor het team")
        pdf.bullets(plan.uitgangspunten)

        pdf.heading("3. Vaste werkwijze")
        pdf.table(
            floatArrayOf(0.2f, 0.28f, 0.26f, 0.26f),
            listOf(Row(listOf("Stap", "Wat doen we?", "Rol bewoner", "Rol begeleiding").map { Cell(it, bold = true, shaded = true) })) +
                plan.werkwijze.mapIndexed { i, s ->
                    Row(listOf(Cell("${i + 1}. ${s.stap}"), Cell(s.wat), Cell(s.bewoner), Cell(s.begeleiding)))
                }
        )

        pdf.heading("4. ${plan.volgordeTitel}")
        pdf.numbered(plan.volgorde)

        pdf.heading("5. Begeleidingsniveau")
        pdf.paragraph("Gebruik onderstaande schaal om de hoeveelheid ondersteuning eenduidig te beschrijven:")
        pdf.table(
            floatArrayOf(0.4f, 0.6f),
            listOf(Row(listOf(Cell("Niveau", bold = true, shaded = true), Cell("Betekenis", bold = true, shaded = true)))) +
                Werkplan.BEGELEIDINGSNIVEAUS.map { (level, meaning) -> Row(listOf(Cell(level, bold = true), Cell(meaning))) }
        )

        pdf.heading("6. Eindcontrole")
        pdf.paragraph("Gebruik deze lijst bij het afronden. Vink af wat klaar is.")
        pdf.checkboxes(plan.eindcontrole)

        pdf.heading("7. Rapportage na afloop", keepWith = 160f)
        pdf.table(
            floatArrayOf(0.4f, 0.6f),
            listOf(
                Row(listOf(Cell("Datum", bold = true, shaded = true), Cell(""))),
                Row(listOf(Cell("Moment", bold = true, shaded = true), Cell("☐ uitgevoerd     ☐ niet uitgevoerd"))),
                Row(listOf(Cell("Begeleidingsniveau", bold = true, shaded = true), Cell("0     1     2     3     4     5"))),
                Row(listOf(Cell("Wat ging goed?", bold = true), Cell("")), minHeight = 52f),
                Row(listOf(Cell("Waar was hulp bij nodig?", bold = true), Cell("")), minHeight = 52f),
                Row(listOf(Cell("Aandacht voor volgende keer", bold = true), Cell("")), minHeight = 52f)
            )
        )

        if (plan.teamafspraak.isNotBlank()) {
            pdf.space(10f)
            pdf.paragraph(bold("Teamafspraak: ", plan.teamafspraak))
        }
    }

    // ---------- Deel 2: aftekenkaart voor de bewoner (nieuwe pagina) ----------

    private fun cardPart(pdf: Pager, logo: Bitmap?, plan: Werkplan) {
        pdf.newPage()
        pdf.logo(logo)
        pdf.title(plan.kaart.titel, "UBB2  •  ${plan.frequentie}".trimEnd(' ', '•'))
        pdf.table(
            floatArrayOf(0.22f, 0.28f, 0.22f, 0.28f),
            listOf(
                infoRow("Mijn naam", "", "Mijn vaste dag", plan.vasteDag),
                infoRow("Starttijd", plan.vasteTijd, "Samen met", "")
            )
        )

        pdf.heading("Ik doe het stap voor stap")
        pdf.table(
            floatArrayOf(0.1f, 0.1f, 0.36f, 0.44f),
            listOf(Row(listOf("Klaar", "", "Taak", "Wat doe ik?").map { Cell(it, bold = true, shaded = true) })) +
                plan.kaart.taken.map { t ->
                    Row(
                        listOf(Cell(checkbox = true), Cell(t.emoji, size = 15f, center = true), Cell(t.taak, bold = true, size = 11f), Cell(t.watDoeIk)),
                        minHeight = 30f
                    )
                }
        )

        pdf.space(14f)
        pdf.paragraph(bold("Hulp nodig? ", plan.kaart.hulp), center = true, size = 11f)
        pdf.space(4f)
        pdf.paragraph(bold("Mijn doel: ${plan.kaart.doel}", ""), center = true, size = 13f)

        pdf.heading("Zo ging het", keepWith = 150f)
        // Zoveel aftekenregels als er op de pagina passen (3 t/m 6), zodat de kaart op één pagina blijft.
        val rows = ((pdf.remaining() - 30f) / 30f).toInt().coerceIn(3, 6)
        pdf.table(
            floatArrayOf(0.2f, 0.3f, 0.25f, 0.25f),
            listOf(Row(listOf("Datum", "Hoe ging het?", "Mijn handtekening", "Begeleider").map { Cell(it, bold = true, shaded = true) })) +
                List(rows) {
                    Row(listOf(Cell(), Cell("🙂     😐     🙁", size = 13f, center = true), Cell(), Cell()), minHeight = 28f)
                }
        )
    }

    // ---------- Deel 3: evaluatieformulier (nieuwe pagina) ----------

    private fun evaluationPart(pdf: Pager, logo: Bitmap?, plan: Werkplan) {
        pdf.newPage()
        pdf.logo(logo)
        pdf.title("EVALUATIE", "Werkplan ${plan.taak.lowercase()}")
        pdf.table(
            floatArrayOf(0.22f, 0.28f, 0.22f, 0.28f),
            listOf(
                infoRow("Bewoner", "[bewoner]", "Evaluatiedatum", plan.evaluatiedatum),
                infoRow("Startdatum", plan.startdatum, "Aanwezig (rollen)", "")
            )
        )

        pdf.heading("1. Doel van het werkplan")
        pdf.paragraph(plan.doel)

        pdf.heading("2. Hoe ging het?")
        pdf.table(
            floatArrayOf(0.4f, 0.6f),
            listOf(
                Row(listOf(Cell("Is het doel bereikt?", bold = true, shaded = true), Cell("☐ ja     ☐ deels     ☐ nee"))),
                Row(listOf(Cell("Begeleidingsniveau bij de start", bold = true, shaded = true), Cell("0     1     2     3     4     5"))),
                Row(listOf(Cell("Begeleidingsniveau nu", bold = true, shaded = true), Cell("0     1     2     3     4     5"))),
                Row(listOf(Cell("Wat ging goed?", bold = true), Cell("")), minHeight = 52f),
                Row(listOf(Cell("Wat was lastig?", bold = true), Cell("")), minHeight = 52f),
                Row(listOf(Cell("Wat zegt de bewoner zelf?", bold = true), Cell("")), minHeight = 52f),
                Row(listOf(Cell("Wat passen we aan?", bold = true), Cell("")), minHeight = 52f)
            )
        )

        pdf.heading("3. Vervolg")
        pdf.table(
            floatArrayOf(0.4f, 0.6f),
            listOf(
                Row(listOf(Cell("Werkplan", bold = true, shaded = true), Cell("☐ doorgaan     ☐ aanpassen     ☐ afsluiten"))),
                Row(listOf(Cell("Volgende evaluatiedatum", bold = true, shaded = true), Cell("")))
            )
        )

        pdf.space(16f)
        pdf.table(
            floatArrayOf(0.25f, 0.375f, 0.375f),
            listOf(
                Row(listOf("Datum", "Handtekening bewoner", "Handtekening begeleider").map { Cell(it, bold = true, shaded = true) }),
                Row(listOf(Cell(), Cell(), Cell()), minHeight = 40f)
            )
        )
    }

    private fun infoRow(l1: String, v1: String, l2: String, v2: String) =
        Row(listOf(Cell(l1, bold = true, shaded = true), Cell(v1), Cell(l2, bold = true, shaded = true), Cell(v2)))

    /** Vet begin, gewone rest. */
    private fun bold(boldPart: String, rest: String): CharSequence =
        SpannableStringBuilder(boldPart).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, boldPart.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(rest)
        }

    private fun layout(text: CharSequence, width: Float, size: Float, bold: Boolean = false, color: Int = black, center: Boolean = false) =
        StaticLayout.Builder
            .obtain(text, 0, text.length, TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                if (bold) typeface = Typeface.DEFAULT_BOLD
            }, width.toInt().coerceAtLeast(1))
            .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1f)
            .build()

    /** Houdt de huidige pagina en de schrijfpositie bij; begint vanzelf een nieuwe pagina als iets niet past. */
    private class Pager(private val footer: String) {
        val doc = PdfDocument()
        private var page: PdfDocument.Page? = null
        private var number = 0
        private var y = MARGIN
        private val canvas: Canvas get() = page!!.canvas

        fun newPage() {
            finish()
            number++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), number).create())
            y = MARGIN
        }

        fun finish() {
            val current = page ?: return
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.GRAY; textAlign = Paint.Align.CENTER }
            current.canvas.drawText(footer, W / 2, H - BOTTOM / 2, paint)
            paint.textAlign = Paint.Align.RIGHT
            current.canvas.drawText("$number", W - MARGIN, H - BOTTOM / 2, paint)
            doc.finishPage(current)
            page = null
        }

        private fun ensure(height: Float) {
            if (page == null || y + height > H - BOTTOM) newPage()
        }

        /** Ruimte tot de onderkant van de huidige pagina. */
        fun remaining(): Float = H - BOTTOM - y

        fun space(height: Float) {
            y += height
        }

        fun logo(bitmap: Bitmap?) {
            bitmap ?: return
            val size = 64f
            val height = size * bitmap.height / bitmap.width
            canvas.drawBitmap(bitmap, null, RectF((W - size) / 2, y - 14, (W + size) / 2, y - 14 + height), Paint(Paint.FILTER_BITMAP_FLAG))
            y += height - 4
        }

        fun title(title: String, subtitle: String) {
            draw(layout(title, CONTENT, 22f, bold = true, center = true), MARGIN)
            y += 6
            draw(layout(subtitle, CONTENT, 14f, bold = true, center = true), MARGIN)
            y += 14
        }

        /** [keepWith]: zoveel ruimte moet er onder het kopje nog zijn, anders begint het kopje op een nieuwe pagina. */
        fun heading(text: String, keepWith: Float = 50f) {
            val l = layout(text, CONTENT, 13.5f, bold = true, color = headingBlue)
            y += 14
            ensure(l.height + keepWith)
            draw(l, MARGIN)
            y += 3
        }

        fun paragraph(text: CharSequence, center: Boolean = false, size: Float = 10f) {
            val l = layout(text, CONTENT, size, center = center)
            ensure(l.height.toFloat())
            draw(l, MARGIN)
            y += 2
        }

        fun bullets(items: List<String>) = items.forEach { listItem("•", it) }

        fun numbered(items: List<String>) = items.forEachIndexed { i, s -> listItem("${i + 1}.", s, gap = 4f) }

        fun checkboxes(items: List<String>) = items.forEach { item ->
            val l = layout(item, CONTENT - 18, 10f)
            ensure(l.height + 4f)
            checkbox(MARGIN + 2, y + 2)
            draw(l, MARGIN + 18)
            y += 4
        }

        private fun listItem(label: String, text: String, gap: Float = 1.5f) {
            val l = layout(text, CONTENT - 18, 10f)
            ensure(l.height.toFloat())
            draw(layout(label, 18f, 10f), MARGIN + 2, advance = false)
            draw(l, MARGIN + 18)
            y += gap
        }

        fun table(weights: FloatArray, rows: List<Row>) {
            val border = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 0.6f; color = black }
            val fill = Paint().apply { style = Paint.Style.FILL; color = shade }
            rows.forEach { row ->
                val widths = weights.map { it * CONTENT }
                val layouts = row.cells.mapIndexed { i, c ->
                    val textWidth = widths[i] - 2 * PAD - if (c.checkbox) 14 else 0
                    layout(c.text, textWidth, c.size, bold = c.bold, center = c.center)
                }
                val height = maxOf(row.minHeight, (layouts.maxOf { it.height } + 2 * PAD))
                ensure(height)
                var x = MARGIN
                row.cells.forEachIndexed { i, c ->
                    val rect = RectF(x, y, x + widths[i], y + height)
                    if (c.shaded) canvas.drawRect(rect, fill)
                    if (c.checkbox) checkbox(x + PAD + 2, y + (height - 10) / 2)
                    val textX = x + PAD + if (c.checkbox) 14 else 0
                    val textY = if (row.minHeight > 0 && c.center || c.checkbox) y + (height - layouts[i].height) / 2 else y + PAD
                    canvas.save()
                    canvas.translate(textX, textY)
                    layouts[i].draw(canvas)
                    canvas.restore()
                    canvas.drawRect(rect, border)
                    x += widths[i]
                }
                y += height
            }
        }

        private fun checkbox(x: Float, top: Float) {
            canvas.drawRect(RectF(x, top, x + 10, top + 10), Paint().apply {
                style = Paint.Style.STROKE; strokeWidth = 0.8f; color = black
            })
        }

        private fun draw(l: StaticLayout, x: Float, advance: Boolean = true) {
            canvas.save()
            canvas.translate(x, y)
            l.draw(canvas)
            canvas.restore()
            if (advance) y += l.height
        }
    }
}
