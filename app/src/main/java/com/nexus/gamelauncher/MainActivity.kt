package com.nexus.gamelauncher

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.gamelauncher.feature.library.LibraryViewModel
import com.nexus.gamelauncher.feature.personalization.ThemeViewModel
import com.nexus.gamelauncher.ui.NexusApp
import com.nexus.gamelauncher.ui.Night
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val library: LibraryViewModel by viewModels()
    private val theme: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val themeModel = theme
        var themeContentReady = false
        splash.setKeepOnScreenCondition { !themeContentReady }
        splash.setOnExitAnimationListener { screen ->
            if (ValueAnimator.areAnimatorsEnabled()) {
                screen.view.animate().alpha(0f).setDuration(140)
                    .withEndAction(screen::remove).start()
            } else screen.remove()
        }
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            val resolved by themeModel.theme.collectAsStateWithLifecycle()
            val palette = resolved
            if (palette == null) {
                Box(Modifier.fillMaxSize().background(Night))
            } else {
                NexusApp(library, themeModel, palette)
                SideEffect { themeContentReady = true }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        library.refresh()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
