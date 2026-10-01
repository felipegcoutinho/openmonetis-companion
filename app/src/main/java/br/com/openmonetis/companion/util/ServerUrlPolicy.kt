package br.com.openmonetis.companion.util

import br.com.openmonetis.companion.BuildConfig
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Policy for origins that may receive an OpenMonetis device bearer token. */
object ServerUrlPolicy {
    fun normalize(
        rawValue: String,
        allowLocalCleartext: Boolean = BuildConfig.DEBUG
    ): String? = parse(rawValue, allowLocalCleartext)?.toString()?.removeSuffix("/")

    fun parse(
        rawValue: String,
        allowLocalCleartext: Boolean = BuildConfig.DEBUG
    ): HttpUrl? {
        val url = rawValue.trim().toHttpUrlOrNull() ?: return null
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) return null
        if (url.query != null || url.fragment != null || url.encodedPath != "/") return null

        val isAllowed = url.isHttps ||
            (allowLocalCleartext && url.scheme == "http" && url.host in localDevelopmentHosts)
        if (!isAllowed) return null

        return url.newBuilder()
            .encodedPath("/")
            .query(null)
            .fragment(null)
            .build()
    }

    fun parseStored(rawValue: String?): HttpUrl? {
        if (rawValue.isNullOrBlank()) return null
        return parse(rawValue)
    }

    private val localDevelopmentHosts = setOf("localhost", "127.0.0.1", "::1", "10.0.2.2")
}
