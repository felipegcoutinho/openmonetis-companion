package br.com.openmonetis.companion.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives boot completed broadcasts to ensure the NotificationListenerService
 * is restarted after device reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, scheduling sync check")

            // Schedule a sync to catch up on any pending notifications
            SyncWorker.enqueue(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
