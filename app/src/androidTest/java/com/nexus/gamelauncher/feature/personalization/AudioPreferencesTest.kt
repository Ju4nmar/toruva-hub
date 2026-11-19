package com.nexus.gamelauncher.feature.personalization

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.nexus.gamelauncher.audio.AudioSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.UUID

class AudioPreferencesTest {
    @Test fun audioSettingsSurviveReopenWithoutChangingSavedTheme() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "${UUID.randomUUID()}.preferences_pb")
        val settings = AudioSettings(effects = false, startup = true, volume = 37)

        suspend fun usePreferences(block: suspend (ThemeRepository) -> Unit) {
            val job = SupervisorJob()
            val store = PreferenceDataStoreFactory.create(scope = CoroutineScope(job + Dispatchers.IO)) { file }
            try {
                block(ThemeRepository(store))
            } finally {
                job.cancelAndJoin()
            }
        }

        try {
            usePreferences {
                it.set(NexusTheme.Purple)
                assertEquals(AudioSettings(), it.audio.first())
                it.setAudio(settings)
            }
            usePreferences {
                assertEquals(settings, it.audio.first())
                assertEquals(NexusTheme.Purple, it.theme.first())
            }
        } finally {
            file.delete()
        }
        Unit
    }
}
