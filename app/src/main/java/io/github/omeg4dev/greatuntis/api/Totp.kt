package io.github.omeg4dev.greatuntis.api

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Totp {
    private fun cleanupKey(base32Key: String): String {
        return base32Key.filterNot { it.isWhitespace() || it == '=' }.uppercase()
    }

    private fun decodeBase32(base32Key: String): ByteArray {
        val cleaned = cleanupKey(base32Key)
        var buffer = 0
        var bitsLeft = 0
        val output = ByteArray((cleaned.length * 5) / 8)
        var count = 0
        for (ch in cleaned) {
            val value = when (ch) {
                in 'A'..'Z' -> ch - 'A'
                in '2'..'7' -> ch - '2' + 26
                else -> throw IllegalArgumentException("Illegal character in Base32 string: $ch")
            }
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[count++] = ((buffer shr bitsLeft) and 0xFF).toByte()
            }
        }
        return output
    }

    /** Returns true if key is non-empty after cleanup, contains only A-Z and 2-7, and decodes to >= 10 bytes. */
    fun isValidKey(base32Key: String): Boolean {
        val cleaned = cleanupKey(base32Key)
        if (cleaned.isEmpty()) return false
        for (ch in cleaned) {
            if (ch !in 'A'..'Z' && ch !in '2'..'7') return false
        }
        val byteLength = (cleaned.length * 5) / 8
        return byteLength >= 10
    }

    /** 6-digit TOTP (SHA1, 30 s step) for a base32 key (case-insensitive, spaces/padding ignored). */
    fun code(base32Key: String, epochSeconds: Long): Int {
        val keyBytes = decodeBase32(base32Key)
        val counter = epochSeconds / 30L
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = mac.doFinal(counterBytes)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        return binary % 1_000_000
    }
}
