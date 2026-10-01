package br.com.openmonetis.companion.data.remote.dto

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
    val tokenId: String?,
    val tokenName: String?,
    val expiresAt: String?,
    val error: String?
)

// Inbox Request
data class InboxRequest(
    val sourceApp: String,
    val sourceAppName: String?,
    val originalTitle: String?,
    val originalText: String,
    val notificationTimestamp: String,
    val parsedName: String?,
    val parsedAmount: Double?,
    val clientId: String?,
    // UTC timestamps must opt out of the API's legacy Brazil wall-clock correction.
    val timestampFormatVersion: Int = 2
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
