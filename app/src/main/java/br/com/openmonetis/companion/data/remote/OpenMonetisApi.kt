package br.com.openmonetis.companion.data.remote

import br.com.openmonetis.companion.data.remote.dto.HealthResponse
import br.com.openmonetis.companion.data.remote.dto.InboxBatchRequest
import br.com.openmonetis.companion.data.remote.dto.InboxBatchResponse
import br.com.openmonetis.companion.data.remote.dto.InboxRequest
import br.com.openmonetis.companion.data.remote.dto.InboxResponse
import br.com.openmonetis.companion.data.remote.dto.RefreshTokenResponse
import br.com.openmonetis.companion.data.remote.dto.VerifyTokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenMonetisApi {

    @GET("api/health")
    suspend fun healthCheck(): Response<HealthResponse>

    @POST("api/auth/device/verify")
    suspend fun verifyToken(): Response<VerifyTokenResponse>

    @POST("api/auth/device/refresh")
    suspend fun refreshToken(
        @Header("Authorization") refreshToken: String
    ): Response<RefreshTokenResponse>

    @POST("api/inbox")
    suspend fun submitNotification(
        @Body request: InboxRequest
    ): Response<InboxResponse>

    @POST("api/inbox/batch")
    suspend fun submitBatch(
        @Body request: InboxBatchRequest
    ): Response<InboxBatchResponse>
}
