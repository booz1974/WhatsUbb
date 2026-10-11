package com.example.whatsubb

import com.example.whatsubb.util.Phones
import org.junit.Assert.assertEquals
import org.junit.Test

class PhonesTest {
    @Test
    fun dutchMobileGetsCountryCode() = assertEquals("31612345678", Phones.whatsAppNumber("06 12345678"))

    @Test
    fun dashesAreRemoved() = assertEquals("31612345678", Phones.whatsAppNumber("06-1234 5678"))

    @Test
    fun internationalPlusIsKept() = assertEquals("32470123456", Phones.whatsAppNumber("+32 470 123456"))

    @Test
    fun doubleZeroPrefixIsDropped() = assertEquals("32470123456", Phones.whatsAppNumber("0032470123456"))
}
