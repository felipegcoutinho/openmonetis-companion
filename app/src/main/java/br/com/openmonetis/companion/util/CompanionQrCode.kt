package br.com.openmonetis.companion.util

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/** Parser for the versioned QR payload emitted by the OpenMonetis web client. */
object CompanionQrCode {
    private const val SCHEME = "openmonetis"
    private const val HOST = "companion"
    private const val PATH = "/token"
    private const val VERSION = "1"
    private val deviceTokenPattern = Regex("^opm_[A-Za-z0-9_-]{43}$")

    fun extractToken(rawValue: String): String? {
        val value = rawValue.trim()
        if (deviceTokenPattern.matches(value)) return value

        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return null
        }
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return null
        if (!uri.host.equals(HOST, ignoreCase = true) || uri.path != PATH) return null
        if (uri.userInfo != null || uri.port != -1 || uri.fragment != null) return null

        val parameters = parseQuery(uri.rawQuery ?: return null) ?: return null
        if (parameters.keys != setOf("v", "token") || parameters["v"] != VERSION) return null
        return parameters["token"]?.takeIf(deviceTokenPattern::matches)
    }

    private fun parseQuery(rawQuery: String): Map<String, String>? {
        val parameters = mutableMapOf<String, String>()
        for (part in rawQuery.split("&")) {
            val separator = part.indexOf('=')
            if (separator <= 0) return null
            val key = decode(part.substring(0, separator)) ?: return null
            val value = decode(part.substring(separator + 1)) ?: return null
            if (parameters.put(key, value) != null) return null
        }
        return parameters
    }

    private fun decode(value: String): String? = try {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    } catch (_: Exception) {
        null
    }
}
