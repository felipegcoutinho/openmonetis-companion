package br.com.opensheets.companion.di

import android.content.Context
import androidx.room.Room
import br.com.opensheets.companion.data.local.AppDatabase
import br.com.opensheets.companion.data.local.dao.AppConfigDao
import br.com.opensheets.companion.data.local.dao.NotificationDao
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
}
