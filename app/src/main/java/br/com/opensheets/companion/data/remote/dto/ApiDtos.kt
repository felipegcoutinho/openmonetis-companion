package br.com.opensheets.companion.data.remote.dto

import com.google.gson.annotations.SerializedName

// Health Check
data class HealthResponse(
    val status: String,
    val name: String?,
    val version: String?,
    val timestamp: String?,
    val message: String?
)

// Token Verification
data class VerifyTokenResponse(
    val valid: Boolean,
    val userId: String?,
    val tokenId: String?,
    val tokenName: String?,
    val expiresAt: String?,
    val error: String?
)

// Token Refresh
data class RefreshTokenResponse(
    val accessToken: String?,
    val expiresAt: String?,
    val error: String?
)

// Inbox Request
data class InboxRequest(
    val sourceApp: String,
    val sourceAppName: String?,
    val deviceId: String?,
    val originalTitle: String?,
    val originalText: String,
    val notificationTimestamp: String,
    val parsedName: String?,
    val parsedAmount: Double?,
    val parsedDate: String?,
    val parsedCardLastDigits: String?,
    val parsedTransactionType: String?,
    val clientId: String?
)

// Inbox Response
data class InboxResponse(
    val id: String?,
    val clientId: String?,
    val message: String?,
    val error: String?
)

// Batch Request
data class InboxBatchRequest(
    val items: List<InboxRequest>
)

// Batch Response
data class InboxBatchResponse(
    val message: String?,
    val total: Int,
    val success: Int,
    val failed: Int,
    val results: List<BatchResult>,
    val error: String?
)

data class BatchResult(
    val clientId: String?,
    val serverId: String?,
    val success: Boolean,
    val error: String?
)

// Error Response
data class ErrorResponse(
    val error: String?,
    val message: String?,
    val retryAfter: Int?
)
