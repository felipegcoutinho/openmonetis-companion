package br.com.openmonetis.companion.data.remote

import br.com.openmonetis.companion.data.remote.dto.VerifyTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceConnectionVerifier @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun healthCheck(serverOrigin: HttpUrl) = withContext(Dispatchers.IO) {
        Retrofit.Builder().baseUrl(serverOrigin).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
            .create(OpenMonetisApi::class.java).healthCheck()
    }

    suspend fun verify(serverOrigin: HttpUrl, token: String): VerifyTokenResponse? =
        withContext(Dispatchers.IO) {
            val api = Retrofit.Builder()
                .baseUrl(serverOrigin)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeviceVerificationApi::class.java)
            val response = api.verify("Bearer $token")
            if (response.code() >= 500 || response.code() == 429) throw java.io.IOException("Servidor indisponível")
            response.body()?.takeIf { response.isSuccessful && it.valid }
        }
}

private interface DeviceVerificationApi {
    @POST("api/auth/device/verify")
    suspend fun verify(@Header("Authorization") authorization: String): retrofit2.Response<VerifyTokenResponse>
}
