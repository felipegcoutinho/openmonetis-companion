package br.com.openmonetis.companion.di

import android.content.Context
import androidx.room.Room
import br.com.openmonetis.companion.data.local.AppDatabase
import br.com.openmonetis.companion.data.local.dao.AppConfigDao
import br.com.openmonetis.companion.data.local.dao.KeywordsSettingsDao
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.data.local.dao.SyncLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "openmonetis_companion.db"
        ).build()
    }

    @Provides
    fun provideNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    fun provideAppConfigDao(database: AppDatabase): AppConfigDao {
        return database.appConfigDao()
    }

    @Provides
    fun provideKeywordsSettingsDao(database: AppDatabase): KeywordsSettingsDao {
        return database.keywordsSettingsDao()
    }

    @Provides
    fun provideSyncLogDao(database: AppDatabase): SyncLogDao {
        return database.syncLogDao()
    }
}
