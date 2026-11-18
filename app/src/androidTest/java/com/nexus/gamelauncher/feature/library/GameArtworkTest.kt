package com.nexus.gamelauncher.feature.library

import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class GameArtworkTest {
    @Test
    fun replacesPrivateCopiesAndRetainsTheSourceImage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = kotlin.io.path.createTempDirectory(context.cacheDir.toPath()).toFile()
        val isolated = object : ContextWrapper(context) {
            override fun getFilesDir() = root
        }
        try {
            val source = File(root, "source.png")
            val bitmap = Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888)
            try {
                bitmap.eraseColor(Color.BLUE)
                source.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            } finally {
                bitmap.recycle()
            }
            val uri = Uri.fromFile(source)
            Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build().useRoom { db ->
                val dao = db.libraryDao()
                val repository = LibraryRepository(isolated, dao, GameArtworkStore(isolated))
                repository.changeArtwork("game", ArtworkKind.Cover, uri)
                val old = requireNotNull(repository.artworkFile(dao.find("game")?.coverFile))
                assertTrue(old.isFile)

                repository.changeArtwork("game", ArtworkKind.Background, uri)
                repository.changeArtwork("game", ArtworkKind.Cover, uri)
                val entry = requireNotNull(dao.find("game"))
                val newCover = requireNotNull(repository.artworkFile(entry.coverFile))
                assertNotEquals(old, newCover)
                assertFalse(old.exists())
                assertTrue(newCover.isFile)
                assertTrue(requireNotNull(repository.artworkFile(entry.backgroundFile)).isFile)

                repository.changeArtwork("game", ArtworkKind.Cover, null)
                assertNull(dao.find("game")?.coverFile)
                assertFalse(newCover.exists())
                assertTrue(requireNotNull(repository.artworkFile(entry.backgroundFile)).isFile)
                repository.changeArtwork("game", null, null)
                assertTrue(requireNotNull(File(root, "game_artwork").listFiles()).isEmpty())
                assertTrue(source.isFile)
            }
        } finally {
            root.deleteRecursively()
        }
        Unit
    }

    @Test
    fun rejectsOversizedInputAndCleansThePartialPrivateCopy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = kotlin.io.path.createTempDirectory(context.cacheDir.toPath()).toFile()
        val isolated = object : ContextWrapper(context) { override fun getFilesDir() = root }
        try {
            val source = File(root, "large.png")
            val size = ARTWORK_MAX_BYTES + 1
            RandomAccessFile(source, "rw").use { it.setLength(size) }
            val result = runCatching { GameArtworkStore(isolated).import(Uri.fromFile(source)) }
            assertTrue(result.exceptionOrNull() is IOException)
            assertEquals("Image exceeds 32 MiB", result.exceptionOrNull()?.message)
            assertEquals(size, source.length())
            assertTrue(requireNotNull(File(root, "game_artwork").listFiles()).isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
