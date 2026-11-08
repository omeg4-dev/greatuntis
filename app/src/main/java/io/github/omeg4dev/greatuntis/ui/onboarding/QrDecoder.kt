package io.github.omeg4dev.greatuntis.ui.onboarding

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

object QrDecoder {

    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
    )

    fun decodeQr(pixels: IntArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null

        val source = RGBLuminanceSource(width, height, pixels)
        val reader = MultiFormatReader().apply { setHints(hints) }

        // Normal decode
        val normalBitmap = BinaryBitmap(HybridBinarizer(source))
        val normalResult = runCatching { reader.decodeWithState(normalBitmap) }.getOrNull()
        if (normalResult != null) {
            return normalResult.text
        }

        // Retry with inverted luminance
        val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
        val invertedResult = runCatching { reader.decodeWithState(invertedBitmap) }.getOrNull()
        return invertedResult?.text
    }

    fun decodeYuv(
        yPlaneBytes: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
    ): String? {
        return runCatching {
            val source = PlanarYUVLuminanceSource(
                yPlaneBytes,
                rowStride,
                height,
                0,
                0,
                width,
                height,
                false,
            )
            val qrReader = QRCodeReader()
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            qrReader.decode(bitmap, hints).text
        }.getOrNull()
    }
}
