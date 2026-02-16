package br.com.openmonetis.companion.data.remote.interceptors

import br.com.openmonetis.companion.util.SecureStorage
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor that replaces the base URL with the user-configured server URL.
 * This allows the app to connect to any self-hosted OpenMonetis instance.
 */
class DynamicUrlInterceptor @Inject constructor(
    private val secureStorage: SecureStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val serverUrl = secureStorage.serverUrl
        if (serverUrl.isNullOrBlank()) {
            // No server configured, proceed with original request
            // This will likely fail, but allows health check during setup
            return chain.proceed(originalRequest)
        }

        val serverHttpUrl = serverUrl.toHttpUrlOrNull()
            ?: return chain.proceed(originalRequest)

        val newUrl = originalRequest.url.newBuilder()
            .scheme(serverHttpUrl.scheme)
            .host(serverHttpUrl.host)
            .port(serverHttpUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}
