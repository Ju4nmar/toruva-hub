package com.nexus.gamelauncher.feature.library

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class LibraryMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun migratesVersionOneThroughVersionFour() {
        verifyMigration(
            version = 1,
            insert = "INSERT INTO library_entries (packageName, selected, lastOpened) VALUES ('saved', 1, 123)",
            expected = LibraryEntry("saved", manuallyAdded = true, lastOpened = 123, favorite = true)
        )
    }

    @Test
    fun migratesVersionTwoAndPersistsCustomizationOnReopen() {
        verifyMigration(
            version = 2,
            insert = "INSERT INTO library_entries (packageName, selected, lastOpened, favorite, excluded) " +
                "VALUES ('saved', 0, 345, 1, 1)",
            expected = LibraryEntry("saved", lastOpened = 345, favorite = true, excluded = true)
        )
    }

    @Test
    fun migratesVersionThreeWithoutChangingItsArtwork() {
        verifyMigration(
            version = 3,
            insert = "INSERT INTO library_entries (packageName, selected, lastOpened, favorite, excluded, " +
                "displayName, coverFile, backgroundFile) VALUES ('saved', 1, 432, 1, 0, 'Antes', 'old-cover', 'old-bg')",
            expected = LibraryEntry("saved", manuallyAdded = true, lastOpened = 432, favorite = true,
                displayName = "Antes", coverFile = "old-cover", backgroundFile = "old-bg")
        )
    }

    private fun verifyMigration(version: Int, insert: String, expected: LibraryEntry) = runBlocking {
        val databaseName = "migration-${UUID.randomUUID()}.db"
        try {
            val schemaPath = "com.nexus.gamelauncher.feature.library.LibraryDatabase/$version.json"
            val schema = instrumentation.context.assets.open(schemaPath).bufferedReader().use { JSONObject(it.readText()) }
                .getJSONObject("database").getJSONArray("entities").getJSONObject(0)
            val createSql = schema.getString("createSql").replace("${'$'}{TABLE_NAME}", schema.getString("tableName"))
            context.openOrCreateDatabase(databaseName, 0, null).use { sqlite ->
                sqlite.execSQL(createSql)
                sqlite.execSQL(insert)
                sqlite.version = version
            }

            val cover = "cover.image"
            val backdrop = "background.image"
            val coverFrame = ArtworkFrame(ArtworkFit.Custom.name, 2f, .35f, -.5f)
            val backgroundFrame = ArtworkFrame(ArtworkFit.Fit.name)
            open(databaseName).useRoom { db ->
                val dao = db.libraryDao()
                assertEquals(expected, dao.find("saved"))
                dao.saveDisplayName("saved", "Nombre personalizado")
                dao.saveArtwork("saved", ArtworkKind.Cover, cover)
                dao.saveArtwork("saved", ArtworkKind.Background, backdrop)
                dao.saveFrame("saved", ArtworkKind.Cover, coverFrame)
                dao.saveFrame("saved", ArtworkKind.Background, backgroundFrame)
            }
            open(databaseName).useRoom { db ->
                val dao = db.libraryDao()
                assertEquals(expected.copy(displayName = "Nombre personalizado",
                    coverFile = cover, backgroundFile = backdrop, coverFrame = coverFrame, backgroundFrame = backgroundFrame), dao.find("saved"))
                dao.saveArtwork("saved", ArtworkKind.Cover, null)
                assertEquals(listOf(backdrop), dao.artworkReferences())
                assertEquals(null, dao.find("saved")?.coverFrame)
                assertEquals(backgroundFrame, dao.find("saved")?.backgroundFrame)
                dao.saveArtwork("saved", null, null)
                assertEquals(emptyList<String>(), dao.artworkReferences())
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
        Unit
    }

    private fun open(name: String) = Room.databaseBuilder(context, LibraryDatabase::class.java, name)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
}

internal inline fun <T : RoomDatabase, R> T.useRoom(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        close()
    }
}