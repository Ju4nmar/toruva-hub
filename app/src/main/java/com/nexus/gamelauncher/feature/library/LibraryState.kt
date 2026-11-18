package com.nexus.gamelauncher.feature.library

data class Discovery(
    val apps: List<InstalledApp> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false
)

data class LibraryState(
    val discovery: Discovery = Discovery(),
    val entries: Map<String, LibraryEntry> = emptyMap()
) {
    val games: List<InstalledApp> = discovery.apps.filter { app ->
        val entry = entries[app.packageName]
        entry?.excluded != true && (entry?.manuallyAdded == true || app.isGame)
    }

    val recent: List<InstalledApp> = games.mapNotNull { app ->
        entries[app.packageName]?.lastOpened?.let { app to it }
    }.sortedByDescending { it.second }.map { it.first }.take(10)

    fun title(app: InstalledApp) = entries[app.packageName]?.displayName ?: app.name

    fun isFavorite(app: InstalledApp) = entries[app.packageName]?.favorite == true

    fun searchGames(query: String, favoritesOnly: Boolean): List<InstalledApp> {
        val name = query.trim()
        return games.filter {
            (!favoritesOnly || isFavorite(it)) &&
                (title(it).contains(name, ignoreCase = true) || it.name.contains(name, ignoreCase = true))
        }
    }

    fun addCandidates(query: String): List<InstalledApp> {
        val included = games.mapTo(hashSetOf(), InstalledApp::packageName)
        return discovery.apps.filter { it.packageName !in included }.matchingName(query)
    }

    private fun List<InstalledApp>.matchingName(query: String): List<InstalledApp> {
        val name = query.trim()
        return filter { it.name.contains(name, ignoreCase = true) }
    }
}
