package com.example.whatsubb

import com.example.whatsubb.privacy.Anonymizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AnonymizerTest {

    @Test
    fun removesContactDetailsAndDates() {
        val input = "Bel 06-12345678 of mail jan@test.nl, geboren 12-03-1990, woont Dorpsstraat 12, 1234 AB."
        val result = Anonymizer.scrub(input)
        assertFalse(result.text.contains("12345678"))
        assertFalse(result.text.contains("jan@test.nl"))
        assertFalse(result.text.contains("1990"))
        assertFalse(result.text.contains("Dorpsstraat"))
        assertFalse(result.text.contains("1234 AB"))
        assertEquals(5, result.replacements)
    }

    @Test
    fun replacesGivenNamesCaseInsensitive() {
        val result = Anonymizer.scrub("Piet was boos. Later ging PIET naar buiten.", listOf("Piet"))
        assertEquals("[naam] was boos. Later ging [naam] naar buiten.", result.text)
    }

    @Test
    fun leavesNormalTextAlone() {
        val text = "Hoe laat moet ik om 22:00 binnen zijn?"
        assertEquals(text, Anonymizer.scrub(text).text)
    }
}
