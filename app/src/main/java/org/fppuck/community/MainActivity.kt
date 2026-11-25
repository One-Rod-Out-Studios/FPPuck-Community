package org.fppuck.community

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import org.fppuck.community.data.DeviceFavoritesStore
import org.fppuck.community.ui.FphlApp
import org.fppuck.community.ui.theme.FPHLTheme
import org.fppuck.community.ui.theme.FppuckAppearance

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = getSharedPreferences("fphl_preferences", MODE_PRIVATE)
        val systemBars = WindowCompat.getInsetsController(window, window.decorView)
        setContent {
            LaunchedEffect(Unit) {
                DeviceFavoritesStore.initialize(applicationContext)
            }
            var appearance by remember {
                val saved = settings.getString("appearance", null)
                mutableStateOf(FppuckAppearance.entries.firstOrNull { it.name == saved } ?: FppuckAppearance.DARK)
            }
            FPHLTheme(appearance) {
                val lightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
                SideEffect {
                    systemBars.isAppearanceLightStatusBars = lightSurface
                    systemBars.isAppearanceLightNavigationBars = lightSurface
                }
                FphlApp(appearance) { mode ->
                    settings.edit().putString("appearance", mode.name).apply()
                    appearance = mode
                }
            }
        }
    }
}
