package com.nexus.gamelauncher.feature.library

import com.nexus.gamelauncher.feature.emulators.KnownEmulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryStateTest {
    private val stardew = app("com.chucklefish.stardewvalley", "Stardew Valley", game = true)
    private val settings = app("com.android.settings", "Ajustes")
    private val manual = app("test.manual", "Juego Manual")

    @Test
    fun defaultCatalogUsesGameMetadataAndExcludesOrdinaryApps() {
        val state = state(listOf(settings, stardew, manual))

        assertEquals(listOf(stardew), state.games)
        assertEquals(listOf(settings, manual), state.addCandidates(""))
    }

    @Test
    fun onlyMembershipChangesWhatAppearsInCatalogAndRecents() {
        val settingsEntry = LibraryEntry(settings.packageName, lastOpened = 20, favorite = true)
        val selected = settingsEntry.copy(manuallyAdded = true, favorite = false)
        val state = state(listOf(stardew, settings), settingsEntry)

        assertTrue(state.isFavorite(settings))
        assertTrue(state.recent.isEmpty())
        assertFalse(settings in state.games)

        val afterAdd = state.copy(entries = mapOf(settings.packageName to selected))
        assertFalse(afterAdd.isFavorite(settings))
        assertTrue(settings in afterAdd.games)
        assertEquals(listOf(settings), afterAdd.recent)

        val removed = selected.copy(manuallyAdded = false, excluded = true, favorite = true)
        val afterRemove = state.copy(entries = mapOf(settings.packageName to removed))
        assertTrue(afterRemove.isFavorite(settings))
        assertFalse(settings in afterRemove.games)
        assertTrue(afterRemove.recent.isEmpty())
    }

    @Test
    fun removingDetectedGameHidesItUntilAddedAgain() {
        val excluded = LibraryEntry(stardew.packageName, lastOpened = 27, excluded = true, favorite = true)
        val state = state(listOf(stardew), excluded)

        assertTrue(state.games.isEmpty())
        assertTrue(state.recent.isEmpty())
        assertEquals(listOf(stardew), state.addCandidates(""))
        val restored = state.copy(entries = mapOf(
            stardew.packageName to excluded.copy(manuallyAdded = true, excluded = false)
        ))
        assertEquals(listOf(stardew), restored.games)
        assertTrue(restored.isFavorite(stardew))
    }

    @Test
    fun searchesStayWithinTheirRespectiveLists() {
        val state = state(listOf(stardew, settings, manual),
            LibraryEntry(stardew.packageName, favorite = true),
            LibraryEntry(manual.packageName, manuallyAdded = true)
        )

        assertEquals(listOf(stardew), state.searchGames("  STARDEW ", favoritesOnly = true))
        assertTrue(state.searchGames("ajustes", favoritesOnly = false).isEmpty())
        assertTrue(state.searchGames("manual", favoritesOnly = true).isEmpty())
        assertEquals(listOf(settings), state.addCandidates("AJUSTES"))
        assertTrue(state.addCandidates("stardew").isEmpty())
    }

    @Test
    fun recognizedEmulatorsOverrideGameMetadata() {
        val packages = mapOf(
            "org.ppsspp.ppsspp" to KnownEmulator.PPSSPP,
            "org.ppsspp.ppssppgold" to KnownEmulator.PPSSPP,
            "org.ppsspp.ppsspplegacy" to KnownEmulator.PPSSPP,
            "com.swordfish.lemuroid" to KnownEmulator.Lemuroid
        )
        val emulators = packages.map { (id, expected) ->
            app(id, expected.displayName, game = true).also {
                assertEquals(expected, it.emulator)
                assertFalse(it.isGame)
            }
        }

        assertEquals(listOf(stardew), state(emulators + stardew).games)
        assertEquals(null, KnownEmulator.forPackage("com.swordfish.lemuroid.unrelated"))
    }

    @Test
    fun uninstalledGamesAreAbsentWithoutLosingTheirPreferences() {
        val saved = LibraryEntry(manual.packageName, manuallyAdded = true, lastOpened = 5, favorite = true)
        val state = state(emptyList(), saved)

        assertTrue(state.games.isEmpty())
        assertTrue(state.recent.isEmpty())
        assertTrue(state.addCandidates("").isEmpty())
        assertEquals(saved, state.entries[manual.packageName])

        val reinstalled = state.copy(discovery = Discovery(listOf(manual), loading = false))
        assertEquals(listOf(manual), reinstalled.games)
        assertTrue(reinstalled.isFavorite(manual))
    }

    @Test
    fun searchFindsCustomAndOriginalNamesWithoutRenamingTheApp() {
        val state = state(listOf(stardew), LibraryEntry(stardew.packageName, displayName = "La granja"))

        assertEquals("La granja", state.title(stardew))
        assertEquals(listOf(stardew), state.searchGames("granja", favoritesOnly = false))
        assertEquals(listOf(stardew), state.searchGames("stardew", favoritesOnly = false))
        assertEquals("Stardew Valley", stardew.name)
    }

    private fun app(packageName: String, name: String, game: Boolean = false) =
        InstalledApp(packageName, name, "MainActivity", hasGameMetadata = game)

    private fun state(apps: List<InstalledApp>, vararg entries: LibraryEntry) =
        LibraryState(Discovery(apps, loading = false), entries.associateBy(LibraryEntry::packageName))
}
