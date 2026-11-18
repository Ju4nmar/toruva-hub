package com.nexus.gamelauncher.feature.library

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "library_entries")
data class LibraryEntry(
    @PrimaryKey val packageName: String,
    @ColumnInfo(name = "selected") val manuallyAdded: Boolean = false,
    val lastOpened: Long? = null,
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0") val excluded: Boolean = false,
    val displayName: String? = null,
    val coverFile: String? = null,
    val backgroundFile: String? = null,
    @Embedded(prefix = "coverFrame_") val coverFrame: ArtworkFrame? = null,
    @Embedded(prefix = "backgroundFrame_") val backgroundFrame: ArtworkFrame? = null
)

@Dao
abstract class LibraryDao {
    @Query("SELECT * FROM library_entries")
    abstract fun observe(): Flow<List<LibraryEntry>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun ensureEntry(entry: LibraryEntry)

    @Query("UPDATE library_entries SET selected = :included, excluded = NOT :included WHERE packageName = :packageName")
    abstract suspend fun updateMembership(packageName: String, included: Boolean)

    @Query("UPDATE library_entries SET favorite = NOT favorite WHERE packageName = :packageName")
    abstract suspend fun invertFavorite(packageName: String)

    @Query("UPDATE library_entries SET lastOpened = :time WHERE packageName = :packageName AND (lastOpened IS NULL OR lastOpened < :time)")
    abstract suspend fun updateLastOpened(packageName: String, time: Long)

    @Query("SELECT * FROM library_entries WHERE packageName = :packageName")
    abstract suspend fun find(packageName: String): LibraryEntry?

    @Query("SELECT coverFile FROM library_entries WHERE coverFile IS NOT NULL UNION SELECT backgroundFile FROM library_entries WHERE backgroundFile IS NOT NULL")
    abstract suspend fun artworkReferences(): List<String>

    @Query("UPDATE library_entries SET displayName = :name WHERE packageName = :packageName")
    abstract suspend fun updateDisplayName(packageName: String, name: String?)

    @Query("UPDATE library_entries SET coverFile = :cover, backgroundFile = :background WHERE packageName = :packageName")
    abstract suspend fun updateArtwork(packageName: String, cover: String?, background: String?)

    @Query("UPDATE library_entries SET coverFrame_mode = :mode, coverFrame_zoom = :zoom, coverFrame_x = :x, coverFrame_y = :y WHERE packageName = :packageName")
    abstract suspend fun updateCoverFrame(packageName: String, mode: String?, zoom: Float?, x: Float?, y: Float?)

    @Query("UPDATE library_entries SET backgroundFrame_mode = :mode, backgroundFrame_zoom = :zoom, backgroundFrame_x = :x, backgroundFrame_y = :y WHERE packageName = :packageName")
    abstract suspend fun updateBackgroundFrame(packageName: String, mode: String?, zoom: Float?, x: Float?, y: Float?)

    @Transaction
    open suspend fun saveFrame(packageName: String, kind: ArtworkKind, frame: ArtworkFrame?) {
        ensureEntry(LibraryEntry(packageName))
        val value = frame?.normalized()
        when (kind) {
            ArtworkKind.Cover -> updateCoverFrame(packageName, value?.mode, value?.zoom, value?.x, value?.y)
            ArtworkKind.Background -> updateBackgroundFrame(packageName, value?.mode, value?.zoom, value?.x, value?.y)
        }
    }

    @Transaction
    open suspend fun saveDisplayName(packageName: String, name: String?) {
        ensureEntry(LibraryEntry(packageName))
        updateDisplayName(packageName, name)
    }

    @Transaction
    open suspend fun saveArtwork(packageName: String, kind: ArtworkKind?, file: String?) {
        ensureEntry(LibraryEntry(packageName))
        val entry = requireNotNull(find(packageName))
        val cover = if (kind == ArtworkKind.Background) entry.coverFile else file
        val background = if (kind == ArtworkKind.Cover) entry.backgroundFile else file
        updateArtwork(packageName, cover, background)
        if (file == null) {
            if (kind != ArtworkKind.Background) saveFrame(packageName, ArtworkKind.Cover, null)
            if (kind != ArtworkKind.Cover) saveFrame(packageName, ArtworkKind.Background, null)
        }
    }

    @Transaction
    open suspend fun setIncluded(packageName: String, included: Boolean) {
        ensureEntry(LibraryEntry(packageName))
        updateMembership(packageName, included)
    }

    @Transaction
    open suspend fun toggleFavorite(packageName: String) {
        ensureEntry(LibraryEntry(packageName))
        invertFavorite(packageName)
    }

    @Transaction
    open suspend fun recordLaunch(packageName: String, time: Long) {
        ensureEntry(LibraryEntry(packageName))
        updateLastOpened(packageName, time)
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library_entries ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE library_entries ADD COLUMN excluded INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE library_entries SET favorite = selected WHERE selected = 1")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE library_entries ADD COLUMN displayName TEXT")
        db.execSQL("ALTER TABLE library_entries ADD COLUMN coverFile TEXT")
        db.execSQL("ALTER TABLE library_entries ADD COLUMN backgroundFile TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (prefix in listOf("coverFrame", "backgroundFrame")) {
            db.execSQL("ALTER TABLE library_entries ADD COLUMN ${prefix}_mode TEXT")
            db.execSQL("ALTER TABLE library_entries ADD COLUMN ${prefix}_zoom REAL")
            db.execSQL("ALTER TABLE library_entries ADD COLUMN ${prefix}_x REAL")
            db.execSQL("ALTER TABLE library_entries ADD COLUMN ${prefix}_y REAL")
        }
    }
}

@Database(entities = [LibraryEntry::class], version = 4)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
