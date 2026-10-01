package br.com.openmonetis.companion.ui.screens.history

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import br.com.openmonetis.companion.data.local.dao.NotificationDao
import br.com.openmonetis.companion.ui.notifications.NotificationsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    @ApplicationContext context: Context,
    notificationDao: NotificationDao,
    savedState: SavedStateHandle
) : NotificationsViewModel(context, notificationDao, 50, savedState)
