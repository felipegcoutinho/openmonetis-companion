package br.com.opensheets.companion.di

import android.content.Context
import br.com.opensheets.companion.domain.parser.NotificationParser
import br.com.opensheets.companion.util.SecureStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSecureStorage(
        @ApplicationContext context: Context
    ): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideNotificationParser(): NotificationParser {
        return NotificationParser()
    }
}
