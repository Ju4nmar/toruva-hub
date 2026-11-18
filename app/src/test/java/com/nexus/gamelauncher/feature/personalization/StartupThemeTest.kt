package com.nexus.gamelauncher.feature.personalization

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupThemeTest {
    @Test
    fun coldStartWaitsForStoredThemeWithoutEmittingDynamic() = runBlocking {
        val scope = this + SupervisorJob()
        try {
            val stored = CompletableDeferred<NexusTheme>()
            val state = startupTheme(flow { emit(stored.await()) }, scope)
            assertNull(state.value)

            stored.complete(NexusTheme.Purple)
            assertEquals(NexusTheme.Purple, withTimeout(2000) { state.filterNotNull().first() })
            assertEquals(NexusTheme.Purple, state.value)
        } finally {
            scope.cancel()
        }
    }
}
