package com.nexus.gamelauncher.feature.library

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import android.net.Uri
import com.nexus.gamelauncher.feature.emulators.KnownEmulator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class InstalledApp(
    val packageName: String,
    val name: String,
    val activityName: String,
    val hasGameMetadata: Boolean
) {
    val emulator: KnownEmulator? = KnownEmulator.forPackage(packageName)
    val isGame: Boolean get() = hasGameMetadata && emulator == null
}

@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: LibraryDao,
    private val artwork: GameArtworkStore
) {
    val entries = dao.observe()
    private val artworkLock = Mutex()
    private val icons = LruCache<String, Drawable.ConstantState>(32)

    fun artworkFile(name: String?) = artwork.file(name)

    suspend fun setDisplayName(packageName: String, name: String) {
        dao.saveDisplayName(packageName, name.trim().take(80).ifBlank { null })
    }

    suspend fun setArtworkFrame(packageName: String, kind: ArtworkKind, frame: ArtworkFrame) {
        artworkLock.withLock {
            val entry = dao.find(packageName) ?: return
            val file = if (kind == ArtworkKind.Cover) entry.coverFile else entry.backgroundFile
            if (file != null) dao.saveFrame(packageName, kind, frame)
        }
    }

    suspend fun changeArtwork(packageName: String, kind: ArtworkKind?, uri: Uri?) =
        withContext(Dispatchers.IO) {
            artworkLock.withLock {
                val name = uri?.let(artwork::import)
                dao.saveArtwork(packageName, kind, name)
                artwork.cleanUnreferenced(dao.artworkReferences().toSet())
            }
        }

    suspend fun cleanArtwork() = withContext(Dispatchers.IO) {
        artworkLock.withLock { artwork.cleanUnreferenced(dao.artworkReferences().toSet()) }
    }

    @Suppress("DEPRECATION")
    suspend fun discover(): List<InstalledApp> = withContext(Dispatchers.IO) {
        icons.evictAll()
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
        activities.mapNotNull { result ->
            val activity = result.activityInfo
            if (activity.packageName == context.packageName || !activity.exported) {
                return@mapNotNull null
            }
            val info = activity.applicationInfo
            InstalledApp(
                activity.packageName,
                info.loadLabel(pm).toString().ifBlank { activity.packageName },
                activity.name,
                info.category == ApplicationInfo.CATEGORY_GAME ||
                    info.flags and ApplicationInfo.FLAG_IS_GAME != 0
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { app: InstalledApp -> app.name })
            .distinctBy(InstalledApp::packageName)
    }

    suspend fun icon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            icons.get(packageName)?.newDrawable() ?: context.packageManager.getApplicationIcon(packageName).also { icon ->
                icon.constantState?.let { icons.put(packageName, it) }
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }

    suspend fun setIncluded(packageName: String, included: Boolean) {
        dao.setIncluded(packageName, included)
    }

    suspend fun toggleFavorite(packageName: String) {
        dao.toggleFavorite(packageName)
    }

    fun launch(app: InstalledApp): Boolean = try {
        val component = ComponentName(app.packageName, app.activityName)
        context.startActivity(Intent.makeMainActivity(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    suspend fun recordLaunch(app: InstalledApp) {
        dao.recordLaunch(app.packageName, System.currentTimeMillis())
    }
}
