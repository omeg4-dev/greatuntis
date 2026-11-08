package io.github.omeg4dev.greatuntis.api

import io.github.omeg4dev.greatuntis.model.Account
import java.net.URLDecoder

object AccountParser {
    /**
     * Parses the content of an Untis QR code (`untis://setschool?url=…&school=…&user=…&key=…&schoolNumber=…`)
     * or the plain text block WebUntis shows next to it (Schulnummer/Schule/Url/Benutzer/Schlüssel lines).
     * Returns null when user, key, school or host is missing or the key is not valid base32.
     */
    fun parse(text: String): Account? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        return if (trimmed.startsWith("untis://", ignoreCase = true) ||
            trimmed.startsWith("setschool?", ignoreCase = true) ||
            trimmed.contains("://setschool", ignoreCase = true)
        ) {
            parseQrUrl(trimmed)
        } else {
            parseTextBlock(trimmed)
        }
    }

    private fun parseQrUrl(urlText: String): Account? {
        val query = urlText.substringAfter('?', "")
        if (query.isEmpty()) return null

        var host: String? = null
        var school: String? = null
        var user: String? = null
        var key: String? = null
        var schoolNumber: String? = null

        val pairs = query.split('&')
        for (pair in pairs) {
            if (pair.isEmpty()) continue
            val parts = pair.split('=', limit = 2)
            val paramName = try {
                URLDecoder.decode(parts[0], "UTF-8").trim().lowercase()
            } catch (e: Exception) {
                parts[0].trim().lowercase()
            }
            val paramValue = if (parts.size > 1) {
                try {
                    URLDecoder.decode(parts[1], "UTF-8").trim()
                } catch (e: Exception) {
                    parts[1].trim()
                }
            } else ""

            when (paramName) {
                "url", "server", "host" -> host = extractHost(paramValue)
                "school" -> school = paramValue
                "user", "username" -> user = paramValue
                "key" -> key = paramValue
                "schoolnumber" -> schoolNumber = paramValue
            }
        }

        return buildAccount(host, school, user, key, schoolNumber)
    }

    private fun parseTextBlock(text: String): Account? {
        var host: String? = null
        var school: String? = null
        var user: String? = null
        var key: String? = null
        var schoolNumber: String? = null

        val lines = text.lines()
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val colonIdx = trimmedLine.indexOf(':')
            val tabIdx = trimmedLine.indexOf('\t')
            val sepIdx = when {
                colonIdx >= 0 && tabIdx >= 0 -> minOf(colonIdx, tabIdx)
                colonIdx >= 0 -> colonIdx
                tabIdx >= 0 -> tabIdx
                else -> -1
            }

            if (sepIdx < 0) continue

            val label = trimmedLine.substring(0, sepIdx).trim().lowercase()
            val value = trimmedLine.substring(sepIdx + 1).trim()

            when (label) {
                "schulnummer", "school number", "schoolnumber" -> schoolNumber = value
                "schule", "school" -> school = value
                "url", "server" -> host = extractHost(value)
                "benutzer", "user", "username" -> user = value
                "schlüssel", "schluessel", "key" -> key = value
            }
        }

        return buildAccount(host, school, user, key, schoolNumber)
    }

    private fun extractHost(rawUrl: String): String {
        var s = rawUrl.trim()
        if (s.startsWith("http://", ignoreCase = true) || s.startsWith("https://", ignoreCase = true)) {
            val withoutScheme = s.substringAfter("://")
            val hostPart = withoutScheme.substringBefore('/').substringBefore('?').substringBefore(':')
            return hostPart.trim()
        }
        val hostPart = s.substringBefore('/').substringBefore('?').substringBefore(':')
        return hostPart.trim()
    }

    private fun buildAccount(
        host: String?,
        school: String?,
        user: String?,
        key: String?,
        schoolNumber: String?,
    ): Account? {
        if (host.isNullOrBlank() || school.isNullOrBlank() || user.isNullOrBlank() || key.isNullOrBlank()) {
            return null
        }

        val cleanedKey = key.filterNot { it.isWhitespace() }.uppercase()
        if (!Totp.isValidKey(cleanedKey)) {
            return null
        }

        return Account(
            host = host,
            school = school,
            user = user,
            key = cleanedKey,
            schoolNumber = schoolNumber?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}
