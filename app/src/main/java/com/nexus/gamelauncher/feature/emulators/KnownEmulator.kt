package com.nexus.gamelauncher.feature.emulators

enum class KnownEmulator(val displayName: String, private val packageNames: Set<String>) {
    // https://dev.ppsspp.org/docs/reference/front-end-integration/
    PPSSPP("PPSSPP", setOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold", "org.ppsspp.ppsspplegacy")),
    // https://github.com/Swordfish90/Lemuroid/wiki/Scanning
    Lemuroid("Lemuroid", setOf("com.swordfish.lemuroid"));

    companion object {
        fun forPackage(packageName: String): KnownEmulator? =
            entries.firstOrNull { packageName in it.packageNames }
    }
}
