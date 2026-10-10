package com.example.whatsubb

import com.example.whatsubb.util.Weeks
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class WeeksTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12) =
        Calendar.getInstance().apply { clear(); set(year, month - 1, day, hour, 0) }.timeInMillis

    @Test
    fun weekStartsOnMonday() {
        // zaterdag 10 oktober 2026 -> maandag 5 oktober
        assertEquals(millis(2026, 10, 5, 0), Weeks.start(0, millis(2026, 10, 10)))
        // zondag telt nog bij dezelfde week
        assertEquals(millis(2026, 10, 5, 0), Weeks.start(0, millis(2026, 10, 11, 23)))
        // volgende week
        assertEquals(millis(2026, 10, 12, 0), Weeks.start(1, millis(2026, 10, 10)))
    }

    @Test
    fun labelsWeek() {
        assertEquals("5 t/m 11 oktober", Weeks.label(millis(2026, 10, 5, 0)))
        assertEquals("28 september t/m 4 oktober", Weeks.label(millis(2026, 9, 28, 0)))
    }
}
