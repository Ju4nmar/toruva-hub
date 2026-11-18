package com.nexus.gamelauncher.feature.home

import com.nexus.gamelauncher.feature.library.InstalledApp
import com.nexus.gamelauncher.feature.library.LibraryState
import kotlin.math.abs

internal fun LibraryState.quickAccess(): List<InstalledApp> =
    (games.filter(::isFavorite) + recent).distinctBy(InstalledApp::packageName).ifEmpty { games }

internal fun List<InstalledApp>.restoreActive(packageName: String?): InstalledApp? =
    firstOrNull { it.packageName == packageName } ?: firstOrNull()

internal fun centeredGame(
    games: List<InstalledApp>,
    page: Int,
    settledPage: Int,
    offset: Float,
    scrolling: Boolean
): InstalledApp? =
    if (!scrolling && page == settledPage && abs(offset) <= .005f) games.getOrNull(page) else null

internal fun visuallyFocusedGame(
    games: List<InstalledApp>,
    page: Int,
    destination: Int,
    offset: Float,
    dragging: Boolean
): InstalledApp? =
    if (!dragging && page == destination && abs(offset) <= .06f) games.getOrNull(page) else null

internal fun runForActiveGame(
    displayed: InstalledApp,
    centered: InstalledApp?,
    action: (InstalledApp) -> Unit
) {
    if (centered?.packageName == displayed.packageName) action(centered)
}
