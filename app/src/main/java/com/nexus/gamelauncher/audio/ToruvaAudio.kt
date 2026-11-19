package com.nexus.gamelauncher.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.nexus.gamelauncher.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

data class AudioSettings(val effects: Boolean = true, val startup: Boolean = true, val volume: Int = 65) {
    fun gain(cue: Cue, audibleDevice: Boolean): Float =
        if (!audibleDevice || !(if (cue == Cue.Startup) startup else effects)) 0f
        else volume.coerceIn(0, 100) / 100f
}

enum class Cue(val resource: Int) {
    Startup(R.raw.startup), Tab(R.raw.tab_switch), Carousel(R.raw.carousel_move),
    Select(R.raw.select), Launch(R.raw.launch_game), Back(R.raw.back)
}

// Main-thread owner of one predecoded voice. New sounds replace the previous voice.
@Singleton
class ToruvaAudio @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val release = Runnable { releasePool() }
    private var pool: SoundPool? = null
    private var foreground = false
    private var startupReady = CompletableDeferred<Unit>()
    private var settings = AudioSettings(effects = false, startup = false)
    private val samples = mutableMapOf<Cue, Int>()
    private val loaded = mutableSetOf<Int>()
    private var voice = 0
    private var cue: Cue? = null
    private var lastCarousel = -1000L
    private val ringerListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!deviceAudible()) stop()
        }
    }

    fun enter() {
        if (!foreground) ContextCompat.registerReceiver(
            context, ringerListener, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION),
            ContextCompat.RECEIVER_EXPORTED
        )
        foreground = true
        handler.removeCallbacks(release)
        if (pool != null) return
        val player = SoundPool.Builder().setMaxStreams(1)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        pool = player
        player.setOnLoadCompleteListener { source, id, status ->
            if (source === pool && status == 0) {
                loaded.add(id)
                if (samples[Cue.Startup] == id) startupReady.complete(Unit)
            }
        }
        Cue.entries.forEach { samples[it] = player.load(context, it.resource, 1) }
    }

    fun leave() {
        if (foreground) context.unregisterReceiver(ringerListener)
        foreground = false
        if (cue != Cue.Launch) stop()
        handler.removeCallbacks(release)
        handler.postDelayed(release, if (cue == Cue.Launch) 520L else 0L)
    }

    fun update(value: AudioSettings) {
        settings = value
        val current = cue ?: return
        val gain = value.gain(current, deviceAudible())
        if (gain == 0f) stop() else pool?.setVolume(voice, gain, gain)
    }

    suspend fun awaitStartup() = startupReady.await()

    fun cancelStartup() {
        if (cue == Cue.Startup) stop()
    }

    fun play(event: Cue) {
        if (!foreground) return
        val gain = settings.gain(event, deviceAudible())
        if (gain == 0f) {
            if (!deviceAudible()) stop()
            return
        }
        val id = samples[event]?.takeIf { it in loaded } ?: return
        val now = SystemClock.elapsedRealtime()
        if (event == Cue.Carousel && now - lastCarousel < 125L) return
        stop()
        voice = pool?.play(id, gain, gain, 1, 0, 1f) ?: 0
        if (voice != 0) {
            cue = event
            if (event == Cue.Carousel) lastCarousel = now
        }
    }

    private fun deviceAudible() =
        manager.ringerMode == AudioManager.RINGER_MODE_NORMAL && manager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0

    private fun stop() {
        if (voice != 0) pool?.stop(voice)
        voice = 0
        cue = null
    }

    private fun releasePool() {
        stop()
        pool?.release()
        pool = null
        samples.clear()
        loaded.clear()
        startupReady = CompletableDeferred()
    }
}
