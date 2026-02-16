package br.com.openmonetis.companion.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.openmonetis.companion.data.local.dao.AppConfigDao
import br.com.openmonetis.companion.data.local.dao.KeywordsSettingsDao
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.dao.SyncLogDao
import br.com.openmonetis.companion.data.local.entities.AppConfigEntity
import br.com.openmonetis.companion.data.local.entities.KeywordsSettingsEntity
import br.com.openmonetis.companion.data.local.entities.NotificationEntity
import br.com.openmonetis.companion.data.local.entities.SyncLogEntity

@Database(
    entities = [
        NotificationEntity::class,
        AppConfigEntity::class,
        KeywordsSettingsEntity::class,
        SyncLogEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun keywordsSettingsDao(): KeywordsSettingsDao
    abstract fun syncLogDao(): SyncLogDao
}
