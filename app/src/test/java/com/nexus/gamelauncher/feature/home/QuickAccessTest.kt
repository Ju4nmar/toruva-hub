package com.nexus.gamelauncher.feature.home

import com.nexus.gamelauncher.feature.library.Discovery
import com.nexus.gamelauncher.feature.library.InstalledApp
import com.nexus.gamelauncher.feature.library.LibraryEntry
import com.nexus.gamelauncher.feature.library.LibraryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickAccessTest {
    private val first = game("first")
    private val second = game("second")
    private val third = game("third")
    private val games = listOf(first, second, third)

    @Test
    fun favoritesLeadRecentGamesWithoutDuplicatesOrExcludedEntries() {
        val state = state(
            games,
            LibraryEntry(first.packageName, favorite = true, lastOpened = 1),
            LibraryEntry(second.packageName, lastOpened = 2),
            LibraryEntry(third.packageName, favorite = true, lastOpened = 3, excluded = true)
        )

        assertEquals(listOf(first, second), state.quickAccess())
    }

    @Test
    fun catalogFallbackSupportsZeroOneAndSeveralGames() {
        assertEquals(emptyList<InstalledApp>(), state(emptyList()).quickAccess())
        assertEquals(listOf(first), state(listOf(first)).quickAccess())
        assertEquals(games, state(games).quickAccess())
        assertNull(centeredGame(emptyList(), 0, 0, 0f, false))
        assertEquals(first, centeredGame(listOf(first), 0, 0, 0f, false))
        assertEquals(third, centeredGame(games, 2, 2, 0f, false))
    }

    @Test
    fun selectionSurvivesChangesInOrderAndFallsBackAfterRemoval() {
        val storedPackage = second.packageName

        assertEquals(second, games.restoreActive(storedPackage))
        assertEquals(second, listOf(third, first, second).restoreActive(storedPackage))
        assertEquals(third, listOf(third, first).restoreActive(storedPackage))
        assertNull(emptyList<InstalledApp>().restoreActive(storedPackage))
    }

    @Test
    fun activeGameOnlyChangesAfterMotionAndCenteringFinish() {
        assertNull(centeredGame(games, 1, 0, .3f, true))
        assertNull(centeredGame(games, 1, 0, 0f, false))
        assertNull(centeredGame(games, 1, 1, .1f, false))
        assertEquals(second, centeredGame(games, 1, 1, .001f, false))
    }

    @Test
    fun playAndDetailsRejectAnOutdatedFeaturedGame() {
        val played = mutableListOf<InstalledApp>()
        val viewed = mutableListOf<InstalledApp>()
        val centered = centeredGame(games, 1, 1, 0f, false)

        runForActiveGame(first, centered, played::add)
        runForActiveGame(first, centered, viewed::add)
        runForActiveGame(second, null, played::add)
        runForActiveGame(second, null, viewed::add)
        runForActiveGame(second, centered, played::add)
        runForActiveGame(second, centered, viewed::add)

        assertEquals(listOf(second), played)
        assertEquals(listOf(second), viewed)
    }

    @Test
    fun quickGesturesOnlyPreviewTheNearDestinationAndActionsWaitForConfirmation() {
        val calls = mutableListOf<InstalledApp>()
        assertNull(visuallyFocusedGame(games, 1, 2, .01f, dragging = false))
        assertNull(visuallyFocusedGame(games, 1, 1, .01f, dragging = true))
        assertNull(visuallyFocusedGame(games, 2, 2, .15f, dragging = false))
        val preview = requireNotNull(visuallyFocusedGame(games, 2, 2, .025f, dragging = false))
        assertEquals(third, preview)
        runForActiveGame(preview, centeredGame(games, 2, 1, .025f, true), calls::add)
        runForActiveGame(preview, centeredGame(games, 2, 2, 0f, false), calls::add)
        assertEquals(listOf(third), calls)
    }

    private fun game(id: String) = InstalledApp("test.$id", id, "MainActivity", hasGameMetadata = true)

    private fun state(apps: List<InstalledApp>, vararg entries: LibraryEntry) =
        LibraryState(Discovery(apps, loading = false), entries.associateBy(LibraryEntry::packageName))
}
