package io.github.omeg4dev.greatuntis.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpTest {
    private val rfcKey = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    private val pythonKey = "JBSWY3DPEHPK3PXP"

    @Test
    fun rfcTestVectors() {
        assertEquals(287082, Totp.code(rfcKey, 59L))
        assertEquals(81804, Totp.code(rfcKey, 1111111109L))
        assertEquals(50471, Totp.code(rfcKey, 1111111111L))
        assertEquals(5924, Totp.code(rfcKey, 1234567890L))
        assertEquals(279037, Totp.code(rfcKey, 2000000000L))
    }

    @Test
    fun crossCheckPython() {
        assertEquals(274694, Totp.code(pythonKey, 1793994791L))
    }

    @Test
    fun caseInsensitiveAndSpacesAndPadding() {
        val lowerKey = "jbswy3dpehpk3pxp"
        val spacedKey = "JBSWY 3DPE HPK3 PXP"
        val paddedKey = "JBSWY3DPEHPK3PXP="
        assertEquals(274694, Totp.code(lowerKey, 1793994791L))
        assertEquals(274694, Totp.code(spacedKey, 1793994791L))
        assertEquals(274694, Totp.code(paddedKey, 1793994791L))
    }

    @Test
    fun keyValidation() {
        assertFalse(Totp.isValidKey("abc!"))
        assertTrue(Totp.isValidKey("JBSWY3DPEHPK3PXP"))
        assertTrue(Totp.isValidKey("jbswy3dpehpk3pxp"))
        assertTrue(Totp.isValidKey("JBSWY 3DPE HPK3 PXP"))
        assertTrue(Totp.isValidKey("JBSWY3DPEHPK3PXP=="))
        assertFalse(Totp.isValidKey(""))
        assertFalse(Totp.isValidKey("   "))
        assertFalse(Totp.isValidKey("JBSWY3")) // too short (< 10 bytes)
        assertFalse(Totp.isValidKey("JBSWY389")) // 8 and 9 are invalid in base32
    }
}
