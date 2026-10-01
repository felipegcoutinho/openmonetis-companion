package br.com.openmonetis.companion.util

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

data class ExportResult(
    val fileName: String,
    val notificationCount: Int,
    val uri: android.net.Uri
)

@Singleton
class NotificationsExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationDao: NotificationDao
) {
    suspend fun exportToDownloads(): ExportResult {
        val notifications = notificationDao.getAll()
        val fileName = buildFileName()
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(collection, contentValues)
            ?: throw IOException("Nao foi possivel criar o arquivo de exportacao")

        try {
            resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(buildJsonPayload(notifications).toString(2))
            } ?: throw IOException("Nao foi possivel abrir o arquivo de exportacao")

            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null
            )
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }

        return ExportResult(
            fileName = fileName,
            notificationCount = notifications.size,
            uri = uri
        )
    }

    private fun buildJsonPayload(notifications: List<NotificationEntity>): JSONObject {
        val items = JSONArray()

        notifications.forEach { notification ->
            items.put(
                JSONObject().apply {
                    put("id", notification.id)
                    put("sourceApp", notification.sourceApp)
                    put("sourceAppName", notification.sourceAppName)
                    put("originalTitle", notification.originalTitle)
                    put("originalText", notification.originalText)
                    put("notificationTimestamp", notification.notificationTimestamp)
                    put("parsedName", notification.parsedName)
                    put("parsedAmount", notification.parsedAmount)
                    put("parsedDate", notification.parsedDate)
                    put("parsedCardLastDigits", notification.parsedCardLastDigits)
                    put("syncStatus", notification.syncStatus.name)
                    put("serverItemId", notification.serverItemId)
                    put("syncError", notification.syncError)
                    put("createdAt", notification.createdAt)
                    put("syncedAt", notification.syncedAt)
                }
            )
        }

        return JSONObject().apply {
            put("exportedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            put("count", notifications.size)
            put("notifications", items)
        }
    }

    private fun buildFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "openmonetis_notifications_$timestamp.json"
    }
}
