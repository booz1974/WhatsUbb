package com.example.whatsubb.util

/** Hulpfuncties voor telefoonnummers. */
object Phones {

    /**
     * Nummer in internationaal formaat zonder + of spaties, zoals WhatsApp (wa.me) het wil.
     * "06 12345678" wordt "31612345678"; "+32 470..." en "0032 470..." blijven buitenlands.
     */
    fun whatsAppNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            phone.trim().startsWith("+") -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("0") -> "31" + digits.drop(1)
            else -> digits
        }
    }
}
