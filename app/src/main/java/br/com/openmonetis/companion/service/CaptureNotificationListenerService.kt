package br.com.openmonetis.companion.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import br.com.openmonetis.companion.data.local.dao.AppConfigDao
import br.com.openmonetis.companion.data.local.dao.KeywordsSettingsDao
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.entities.KeywordsSettingsEntity
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.domain.parser.NotificationParser
import br.com.openmonetis.companion.util.SecureStorage
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
    lateinit var keywordsSettingsDao: KeywordsSettingsDao

    @Inject
    lateinit var secureStorage: SecureStorage

    @Inject
    lateinit var notificationParser: NotificationParser

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

                // Get global trigger keywords
                val settings = keywordsSettingsDao.get() ?: KeywordsSettingsEntity()
                val triggerKeywords = settings.getTriggerKeywordsList()

                // Check if notification matches any trigger keyword
                val fullText = "${title.orEmpty()} $text".lowercase()
                val matchesTrigger = triggerKeywords.any { trigger ->
                    fullText.contains(trigger.lowercase())
                }

                if (!matchesTrigger) {
                    Log.d(TAG, "Notification doesn't match any trigger: $text")
                    return@launch
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
                    parsedDate = null,
                    parsedCardLastDigits = null
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

