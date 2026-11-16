package com.financialassistant

import com.financialassistant.tracking.MsisdnNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MsisdnNormalizerTest {

    @Test
    fun testNormalizeAcceptedFormats() {
        assertEquals("254712345678", MsisdnNormalizer.normalize("0712345678"))
        assertEquals("254712345678", MsisdnNormalizer.normalize("+254712345678"))
        assertEquals("254712345678", MsisdnNormalizer.normalize("254712345678"))
        assertEquals("254712345678", MsisdnNormalizer.normalize("712345678"))
        assertEquals("254712345678", MsisdnNormalizer.normalize("0712 345 678"))
        assertEquals("254712345678", MsisdnNormalizer.normalize("0712-345-678"))
        assertEquals("254112345678", MsisdnNormalizer.normalize("0112345678"))
    }

    @Test
    fun testNormalizeRejectsInvalid() {
        assertNull(MsisdnNormalizer.normalize(null))
        assertNull(MsisdnNormalizer.normalize(""))
        assertNull(MsisdnNormalizer.normalize("123"))
        assertNull(MsisdnNormalizer.normalize("abcdefghij"))
    }

    @Test
    fun testFormat() {
        assertEquals("0712 345 678", MsisdnNormalizer.format("254712345678"))
        assertEquals("0712 345 678", MsisdnNormalizer.format("0712345678"))
    }

    @Test
    fun testSameNumber() {
        assertTrue(MsisdnNormalizer.sameNumber("0712345678", "+254712345678"))
        assertFalse(MsisdnNormalizer.sameNumber("0712345678", "0722345678"))
    }
}
