package com.example.whatsubb.util

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import androidx.core.content.FileProvider
import com.example.whatsubb.R
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Maakt een A4-PDF van een AI-document (met logo, titel, datum en paginanummers). */
object PdfExporter {

    private const val PAGE_WIDTH = 595  // A4 in punten
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 56f
    private const val FOOTER_SPACE = 36f
    private const val FIRST_PAGE_HEADER = 84f

    private val textColor = Color.rgb(0x2C, 0x3E, 0x35)
    private val accentColor = Color.rgb(0x5B, 0x82, 0x76)

    const val FOOTER_DOCUMENT = "Whats Ubb  ·  Opgesteld met AI, gecontroleerd door de begeleider"
    const val FOOTER_ANSWER = "Whats Ubb  ·  Antwoord gemaakt met AI. Twijfel je? Vraag het aan de begeleiding."

    /** Map voor PDF's die gedeeld worden (via de FileProvider). */
    private fun shareDir(context: Context) = File(context.cacheDir, "pdf")

    /** Wist eerder gedeelde PDF's, zodat er geen gesprekken op het toestel blijven staan. */
    fun clearShared(context: Context) {
        shareDir(context).listFiles()?.forEach { it.delete() }
    }

    /** Maakt een tijdelijke PDF en opent het deel-menu van Android. */
    fun share(context: Context, fileName: String, title: String, markdown: String, footer: String = FOOTER_DOCUMENT) =
        share(context, fileName, title) { write(context, it, title, markdown, footer) }

    /** Zelfde als hierboven, maar [writePdf] tekent de PDF (bijv. een werkplan). */
    fun share(context: Context, fileName: String, title: String, writePdf: (OutputStream) -> Unit) {
        clearShared(context)
        val file = File(shareDir(context).apply { mkdirs() }, fileName)
        file.outputStream().use(writePdf)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "PDF delen via"))
    }

    fun write(context: Context, out: OutputStream, title: String, markdown: String, footer: String = FOOTER_DOCUMENT) {
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = textColor }
        val body = buildBody(markdown)
        val layout = StaticLayout.Builder
            .obtain(body, 0, body.length, textPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(3f, 1f)
            .build()

        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.whats_ubb_logo)
        val date = SimpleDateFormat("d MMMM yyyy", Locale("nl", "NL")).format(Date())
        val document = PdfDocument()

        var line = 0
        var pageNumber = 1
        do {
            val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            val canvas = page.canvas

            val bodyTop = if (pageNumber == 1) {
                drawHeader(canvas, logo, title, date)
                MARGIN + FIRST_PAGE_HEADER
            } else MARGIN

            if (layout.lineCount > 0) {
                val available = PAGE_HEIGHT - FOOTER_SPACE - MARGIN / 2 - bodyTop
                val startY = layout.getLineTop(line)
                var end = line
                while (end < layout.lineCount && layout.getLineBottom(end) - startY <= available) end++
                if (end == line) end = line + 1

                canvas.save()
                canvas.translate(MARGIN, bodyTop - startY)
                canvas.clipRect(0f, startY.toFloat(), contentWidth.toFloat(), layout.getLineBottom(end - 1).toFloat())
                layout.draw(canvas)
                canvas.restore()
                line = end
            }

            drawFooter(canvas, pageNumber, footer)
            document.finishPage(page)
            pageNumber++
        } while (line < layout.lineCount)

        document.writeTo(out)
        document.close()
    }

    private fun drawHeader(canvas: android.graphics.Canvas, logo: android.graphics.Bitmap?, title: String, date: String) {
        val logoSize = 56f
        logo?.let {
            val height = logoSize * it.height / it.width
            canvas.drawBitmap(it, null, RectF(MARGIN, MARGIN - 8, MARGIN + logoSize, MARGIN - 8 + height), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val textLeft = MARGIN + logoSize + 14
        canvas.drawText(title, textLeft, MARGIN + 18, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f; color = textColor; typeface = Typeface.DEFAULT_BOLD
        })
        canvas.drawText("Afdeling UBB2  ·  $date", textLeft, MARGIN + 38, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f; color = accentColor
        })
        canvas.drawLine(MARGIN, MARGIN + 62, PAGE_WIDTH - MARGIN, MARGIN + 62, Paint().apply {
            color = accentColor; strokeWidth = 1f
        })
    }

    private fun drawFooter(canvas: android.graphics.Canvas, pageNumber: Int, footer: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = Color.GRAY }
        val y = PAGE_HEIGHT - FOOTER_SPACE / 2
        canvas.drawText(footer, MARGIN, y, paint)
        val pageText = "Pagina $pageNumber"
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - paint.measureText(pageText), y, paint)
    }

    private fun buildBody(markdown: String): SpannableStringBuilder {
        val builder = SpannableStringBuilder()
        val lines = Markdown.parse(markdown)
        lines.forEachIndexed { index, line ->
            val start = builder.length
            when (line.kind) {
                Markdown.Kind.HEADING1, Markdown.Kind.HEADING2 -> {
                    builder.append(line.text.replace("**", ""))
                    builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    val size = if (line.kind == Markdown.Kind.HEADING1) 15 else 13
                    builder.setSpan(AbsoluteSizeSpan(size), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                Markdown.Kind.BULLET -> {
                    builder.append("•  ")
                    appendInline(builder, line.text)
                }
                Markdown.Kind.PARAGRAPH -> appendInline(builder, line.text)
                Markdown.Kind.BLANK -> Unit
            }
            if (index < lines.lastIndex) builder.append("\n")
        }
        return builder
    }

    private fun appendInline(builder: SpannableStringBuilder, text: String) {
        Markdown.inline(text).forEach { segment ->
            val start = builder.length
            builder.append(segment.text)
            if (segment.bold) builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
