package com.example.whatsubb

import com.example.whatsubb.data.WeekInfoEntry
import com.example.whatsubb.data.WeekInfoStore
import com.example.whatsubb.data.WeekInfoType
import com.example.whatsubb.util.Weeks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class WeekReminderTest {

    private fun millis(year: Int, month: Int, day: Int, hour: Int = 12) =
        Calendar.getInstance().apply { clear(); set(year, month - 1, day, hour, 0) }.timeInMillis

    private fun entry(type: WeekInfoType, weekStart: Long) =
        WeekInfoEntry(type, weekStart, Weeks.end(weekStart), "tekst", weekStart)

    private val saturday = millis(2026, 10, 10)
    private val sunday = millis(2026, 10, 11)
    private val thisWeek = millis(2026, 10, 5, 0)
    private val nextWeek = millis(2026, 10, 12, 0)

    @Test
    fun weekdayChecksThisWeek() {
        val reminder = WeekInfoStore.reminderFor(listOf(entry(WeekInfoType.MENU, thisWeek)), saturday)!!
        assertEquals(0, reminder.weekOffset)
        assertEquals(listOf(WeekInfoType.ROOSTER), reminder.missing)
    }

    @Test
    fun weekdayWithEverythingHasNoReminder() {
        val entries = WeekInfoType.entries.map { entry(it, thisWeek) }
        assertNull(WeekInfoStore.reminderFor(entries, saturday))
    }

    @Test
    fun sundayChecksNextWeek() {
        // Deze week is compleet, maar volgende week nog niet.
        val entries = WeekInfoType.entries.map { entry(it, thisWeek) }
        val reminder = WeekInfoStore.reminderFor(entries, sunday)!!
        assertEquals(1, reminder.weekOffset)
        assertEquals(nextWeek, reminder.weekStart)
        assertEquals(WeekInfoType.entries, reminder.missing)
        assertEquals(
            "Het weekmenu en werkrooster voor volgende week (12 t/m 18 oktober) zijn nog niet toegevoegd.",
            reminder.text
        )
    }

    @Test
    fun sundayWithNextWeekReadyHasNoReminder() {
        val entries = WeekInfoType.entries.map { entry(it, nextWeek) }
        assertNull(WeekInfoStore.reminderFor(entries, sunday))
    }
}
