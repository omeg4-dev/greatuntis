package io.github.omeg4dev.greatuntis.ui.onboarding

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Test

class QrDecoderTest {

    @Test
    fun testDecodeNormalAndInvertedQr() {
        val content = "untis://setschool?url=demo.webuntis.com&school=demo-schule&user=mmustermann&key=TY6CQ6J5X7Y2Z3A4"
        val size = 256

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)

        // Normal QR code: black modules (0xFF000000) on white background (0xFFFFFFFF)
        val normalPixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                normalPixels[y * size + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        val decodedNormal = QrDecoder.decodeQr(normalPixels, size, size)
        assertEquals(content, decodedNormal)

        // Inverted QR code: white modules (0xFFFFFFFF) on black background (0xFF000000)
        val invertedPixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                invertedPixels[y * size + x] = if (bitMatrix.get(x, y)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            }
        }

        val decodedInverted = QrDecoder.decodeQr(invertedPixels, size, size)
        assertEquals(content, decodedInverted)
    }
}
