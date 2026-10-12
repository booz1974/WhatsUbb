package com.example.whatsubb

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.whatsubb.data.Werkplan
import com.example.whatsubb.util.WerkplanDocx
import com.example.whatsubb.util.WerkplanPdf
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Tekent het voorbeeldwerkplan naar PDF en Word (in de cache van de app), om de opmaak te kunnen bekijken. */
@RunWith(AndroidJUnit4::class)
class WerkplanPdfTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun examplePlan(): Werkplan {
        val json = context.assets.open(Werkplan.EXAMPLE_ASSET).bufferedReader().use { it.readText() }
        return Werkplan.parse("```json\n$json\n```").also { assertNotNull(it) }!!
    }

    @Test
    fun rendersExampleWerkplan() {
        val file = File(context.cacheDir, "werkplan_test.pdf")
        file.outputStream().use { WerkplanPdf.write(context, it, examplePlan()) }
        assertTrue(file.length() > 1000)
    }

    @Test
    fun writesExampleWerkplanAsDocx() {
        val file = File(context.cacheDir, "werkplan_test.docx")
        file.outputStream().use { WerkplanDocx.write(context, it, examplePlan()) }
        assertTrue(file.length() > 1000)
    }
}
