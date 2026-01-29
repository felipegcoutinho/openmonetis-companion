package br.com.opensheets.companion.di

import android.content.Context
import androidx.room.Room
import br.com.opensheets.companion.data.local.AppDatabase
import br.com.opensheets.companion.data.local.dao.AppConfigDao
import br.com.opensheets.companion.data.local.dao.KeywordsSettingsDao
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.dao.SyncLogDao
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
            "opensheets_companion.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5
            )
            .build()
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
