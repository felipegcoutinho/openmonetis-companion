package br.com.openmonetis.companion.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import br.com.openmonetis.companion.ui.navigation.AppNavigation
import br.com.openmonetis.companion.ui.theme.OpenMonetisCompanionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkTheme = isSystemInDarkTheme()
            SideEffect {
                window.isStatusBarContrastEnforced = false
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !darkTheme
            }
            OpenMonetisCompanionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(Modifier.fillMaxSize()) {
                        AppNavigation()
                        // Draw behind the transparent system bar, including on Android 15+.
                        if (!darkTheme) Box(Modifier.align(Alignment.TopCenter)
                            .fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
        }
    }

}
