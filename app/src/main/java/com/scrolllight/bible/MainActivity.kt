package com.scrolllight.bible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.scrolllight.bible.ui.navigation.ScrollLightNavHost
import com.scrolllight.bible.ui.theme.AuroraBackground
import com.scrolllight.bible.ui.theme.ScrollLightTheme
import com.scrolllight.bible.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeVm: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeVm.themeMode.collectAsState()
            ScrollLightTheme(themeMode = themeMode) {
                AuroraBackground(modifier = Modifier.fillMaxSize()) {
                    ScrollLightNavHost()
                }
            }
        }
    }
}
