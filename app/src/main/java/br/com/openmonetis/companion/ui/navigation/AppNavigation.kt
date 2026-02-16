package br.com.openmonetis.companion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.openmonetis.companion.ui.screens.history.HistoryScreen
import br.com.openmonetis.companion.ui.screens.home.HomeScreen
import br.com.openmonetis.companion.ui.screens.logs.LogsScreen
import br.com.openmonetis.companion.ui.screens.settings.SettingsScreen
import br.com.openmonetis.companion.ui.screens.settings.keywords.KeywordsSettingsScreen
import br.com.openmonetis.companion.ui.screens.setup.SetupScreen
import br.com.openmonetis.companion.ui.screens.setup.SetupViewModel

sealed class Screen(val route: String) {
    data object Setup : Screen("setup")
    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object KeywordsSettings : Screen("keywords_settings")
    data object Logs : Screen("logs")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val setupViewModel: SetupViewModel = hiltViewModel()
    val isConfigured by setupViewModel.isConfigured.collectAsState()

    val startDestination = if (isConfigured) Screen.Home.route else Screen.Setup.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToKeywords = {
                    navController.navigate(Screen.KeywordsSettings.route)
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.Logs.route)
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onDisconnected = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.KeywordsSettings.route) {
            KeywordsSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Logs.route) {
            LogsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
