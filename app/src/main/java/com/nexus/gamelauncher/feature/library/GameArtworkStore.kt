package com.nexus.gamelauncher.feature.library

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class ArtworkKind { Cover, Background }

@Singleton
class GameArtworkStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val directory = File(context.filesDir, "game_artwork")

    fun file(name: String?): File? =
        name?.takeIf { managedName.matches(it) }?.let { File(directory, it) }

    fun import(uri: Uri): String {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Artwork directory unavailable")
        val name = "${UUID.randomUUID()}.image"
        val target = File(directory, name)
        var valid = false
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Image unavailable")
            input.use { source ->
                target.outputStream().use { destination ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > ARTWORK_MAX_BYTES) throw IOException("Image exceeds 32 MiB")
                        destination.write(buffer, 0, count)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.path, bounds)
            if (!supportedArtworkDimensions(bounds.outWidth, bounds.outHeight)) throw IOException("Unsupported image")
            valid = true
            return name
        } finally {
            if (!valid) target.delete()
        }
    }

    fun cleanUnreferenced(used: Set<String>) {
        directory.listFiles()?.forEach { artwork ->
            if (artwork.isFile && managedName.matches(artwork.name) && artwork.name !in used) artwork.delete()
        }
    }

    private companion object {
        val managedName = Regex("[a-f0-9-]{36}\\.image")
    }
}
