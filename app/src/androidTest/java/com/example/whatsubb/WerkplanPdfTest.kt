package com.example.whatsubb

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.whatsubb.data.Werkplan
import com.example.whatsubb.util.WerkplanPdf
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Tekent het voorbeeldwerkplan naar een PDF (in de cache van de app), om de opmaak te kunnen bekijken. */
@RunWith(AndroidJUnit4::class)
class WerkplanPdfTest {

    @Test
    fun rendersExampleWerkplan() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val json = context.assets.open(Werkplan.EXAMPLE_ASSET).bufferedReader().use { it.readText() }
        val plan = Werkplan.parse("```json\n$json\n```")
        assertNotNull(plan)
        val file = File(context.cacheDir, "werkplan_test.pdf")
        file.outputStream().use { WerkplanPdf.write(context, it, plan!!) }
        assertTrue(file.length() > 1000)
    }
}
