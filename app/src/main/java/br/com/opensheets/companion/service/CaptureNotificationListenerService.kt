package br.com.opensheets.companion.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import br.com.opensheets.companion.data.local.dao.AppConfigDao
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.entities.NotificationEntity
import br.com.opensheets.companion.domain.parser.NotificationParser
import br.com.opensheets.companion.util.SecureStorage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CaptureNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationDao: NotificationDao

    @Inject
    lateinit var appConfigDao: AppConfigDao

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var notificationParser: NotificationParser

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Check if app is configured
        if (!secureStorage.isConfigured()) {
            return
        }

        val packageName = sbn.packageName

        serviceScope.launch {
            try {
                // Check if this app is being monitored
                val appConfig = appConfigDao.getByPackageName(packageName) ?: return@launch

                if (!appConfig.isEnabled) {
                    return@launch
                }

                // Extract notification text
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

                if (text.isNullOrBlank()) {
                    return@launch
                }

                // Check keywords filter
                val keywords: List<String> = try {
                    val type = object : TypeToken<List<String>>() {}.type
                    gson.fromJson(appConfig.keywords, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                if (keywords.isNotEmpty()) {
                    val matchesKeyword = keywords.any { keyword ->
                        text.contains(keyword, ignoreCase = true) ||
                        (title?.contains(keyword, ignoreCase = true) == true)
                    }
                    if (!matchesKeyword) {
                        return@launch
                    }
                }

                // Parse notification
                val parsed = notificationParser.parse(packageName, title, text)

                // Save to database
                val notification = NotificationEntity(
                    sourceApp = packageName,
                    sourceAppName = appConfig.displayName,
                    originalTitle = title,
                    originalText = text,
                    notificationTimestamp = sbn.postTime,
                    parsedName = parsed.merchantName,
                    parsedAmount = parsed.amount,
                    parsedDate = parsed.date?.time,
                    parsedCardLastDigits = parsed.cardLastDigits,
                    parsedTransactionType = parsed.transactionType
                )

                notificationDao.insert(notification)

                // Schedule sync
                SyncWorker.enqueue(applicationContext)

                Log.d(TAG, "Notification captured: ${appConfig.displayName} - $text")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "NotificationCapture"
    }
}
