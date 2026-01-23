package br.com.opensheets.companion.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.opensheets.companion.data.local.dao.AppConfigDao
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.entities.AppConfigEntity
import br.com.opensheets.companion.data.local.entities.NotificationEntity

@Database(
    entities = [
        NotificationEntity::class,
        AppConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appConfigDao(): AppConfigDao
}
