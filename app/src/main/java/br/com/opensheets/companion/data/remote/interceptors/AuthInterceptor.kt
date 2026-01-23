package br.com.opensheets.companion.data.remote.interceptors

import br.com.opensheets.companion.util.SecureStorage
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that adds the Authorization header with the API token.
 * Skips adding the header for public endpoints like health check.
 */
class AuthInterceptor @Inject constructor(
    private val secureStorage: SecureStorage
) : Interceptor {

    private val publicPaths = listOf(
        "/api/health"
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Check if this is a public endpoint
        val path = originalRequest.url.encodedPath
        if (publicPaths.any { path.endsWith(it) }) {
            return chain.proceed(originalRequest)
        }

        // Check if we have an existing Authorization header (for refresh token)
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        // Add access token
        val accessToken = secureStorage.accessToken
        if (accessToken.isNullOrBlank()) {
            return chain.proceed(originalRequest)
        }

        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $accessToken")
            .build()

        return chain.proceed(newRequest)
    }
}
