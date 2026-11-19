package com.nexus.gamelauncher

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.gamelauncher.audio.Cue
import com.nexus.gamelauncher.audio.ToruvaAudio
import com.nexus.gamelauncher.feature.library.LibraryViewModel
import com.nexus.gamelauncher.feature.personalization.ThemeViewModel
import com.nexus.gamelauncher.ui.NexusApp
import com.nexus.gamelauncher.ui.Night
import com.nexus.gamelauncher.ui.ToruvaIntro
import dagger.hilt.android.AndroidEntryPoint
import com.nexus.gamelauncher.ui.ColdStart
import javax.inject.Inject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val library: LibraryViewModel by viewModels()
    private val theme: ThemeViewModel by viewModels()
    @Inject lateinit var audio: ToruvaAudio
    private var intro by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val cold = coldStart.claim()
        val animate = ValueAnimator.areAnimatorsEnabled() && cold
        intro = animate
        val themeModel = theme
        suspend fun prepareStartupSound() {
            withTimeoutOrNull(120L) {
                val settings = themeModel.audio.filterNotNull().first()
                audio.update(settings)
                if (settings.startup && settings.volume > 0) audio.awaitStartup()
            }
        }
        var themeContentReady = false
        splash.setKeepOnScreenCondition { !themeContentReady }
        splash.setOnExitAnimationListener { it.remove() }
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            val palette by themeModel.theme.collectAsStateWithLifecycle()
            val audioSettings by themeModel.audio.collectAsStateWithLifecycle()
            val resolved = palette
            val sound = audioSettings
            if (sound != null) SideEffect { audio.update(sound) }
            if (resolved == null) {
                Box(Modifier.fillMaxSize().background(Night))
            } else {
                Box(Modifier.fillMaxSize()) {
                    NexusApp(library, themeModel, resolved, audio, sound)
                    if (intro) {
                        ToruvaIntro(prepare = ::prepareStartupSound, play = { audio.play(Cue.Startup) },
                            finished = { intro = false })
                        BackHandler { intro = false; audio.cancelStartup() }
                    }
                }
                if (cold && !animate) {
                    LaunchedEffect(Unit) {
                        prepareStartupSound()
                        audio.play(Cue.Startup)
                    }
                }
                SideEffect { themeContentReady = true }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        audio.enter()
    }

    override fun onResume() {
        super.onResume()
        library.refresh()
    }

    override fun onStop() {
        intro = false
        audio.leave()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        val coldStart = ColdStart()
    }
}
