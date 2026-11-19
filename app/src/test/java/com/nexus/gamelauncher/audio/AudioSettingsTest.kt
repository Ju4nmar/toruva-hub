package com.nexus.gamelauncher.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSettingsTest {
    @Test fun startupAndEffectsHaveIndependentTogglesAndShareVolume() {
        val startupOnly = AudioSettings(effects = false, startup = true, volume = 73)
        assertEquals(.73f, startupOnly.gain(Cue.Startup, true), .001f)
        Cue.entries.filter { it != Cue.Startup }.forEach { assertEquals(0f, startupOnly.gain(it, true), 0f) }
        val navigationOnly = startupOnly.copy(effects = true, startup = false)
        assertEquals(0f, navigationOnly.gain(Cue.Startup, true), 0f)
        assertEquals(.73f, navigationOnly.gain(Cue.Tab, true), .001f)
    }

    @Test fun silentModeAndZeroVolumeMuteAllCues() {
        Cue.entries.forEach {
            assertEquals(0f, AudioSettings().gain(it, false), 0f)
            assertEquals(0f, AudioSettings(volume = 0).gain(it, true), 0f)
            assertEquals(1f, AudioSettings(volume = 150).gain(it, true), 0f)
        }
    }
}
