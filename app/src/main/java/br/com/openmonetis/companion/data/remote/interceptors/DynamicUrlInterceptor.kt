package br.com.openmonetis.companion.data.remote.interceptors

import br.com.openmonetis.companion.util.SecureStorage
import br.com.openmonetis.companion.util.ServerUrlPolicy
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
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

        val serverHttpUrl = ServerUrlPolicy.parseStored(secureStorage.serverUrl)
            ?: throw IOException("OpenMonetis server URL is not configured securely")

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
