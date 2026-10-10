package com.example.whatsubb.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Hulpfuncties voor weken (maandag t/m zondag). */
object Weeks {

    private val dutch = Locale("nl", "NL")

    /** Begin (maandag 00:00) van deze week + [offsetWeeks]. */
    fun start(offsetWeeks: Int = 0, now: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val daysSinceMonday = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // maandag = 0 ... zondag = 6
        c.add(Calendar.DAY_OF_MONTH, -daysSinceMonday + 7 * offsetWeeks)
        return c.timeInMillis
    }

    /** Einde van de week (volgende maandag 00:00, exclusief). */
    fun end(weekStart: Long): Long =
        Calendar.getInstance().apply { timeInMillis = weekStart; add(Calendar.DAY_OF_MONTH, 7) }.timeInMillis

    /** Bijv. "12 t/m 18 oktober" of "29 september t/m 5 oktober". */
    fun label(weekStart: Long): String {
        val start = Calendar.getInstance().apply { timeInMillis = weekStart }
        val last = Calendar.getInstance().apply { timeInMillis = weekStart; add(Calendar.DAY_OF_MONTH, 6) }
        val sameMonth = start.get(Calendar.MONTH) == last.get(Calendar.MONTH)
        val first = SimpleDateFormat(if (sameMonth) "d" else "d MMMM", dutch).format(start.time)
        return "$first t/m ${SimpleDateFormat("d MMMM", dutch).format(last.time)}"
    }

    fun today(now: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("EEEE d MMMM yyyy, 'het is nu' HH:mm 'uur'", dutch).format(Date(now))
}
