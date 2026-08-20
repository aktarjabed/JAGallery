package com.example.advancedgallery.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaDatabaseMigrationTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_migration_db")
        context.deleteDatabase("test_missing_migration_db")
    }

    @Test
    fun migration1To2_preservesFavoritesAndHandlesDuplicatesNullsEmpty() = runTest {
        val dbName = "test_migration_db"

        // Step 1: Create v1 database and insert test data
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE `favorites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `uri` TEXT, `isFavorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = factory.create(config)
        val v1Db = helper.writableDatabase

        // Insert valid favorites, duplicates, null URI, and empty URI
        v1Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('content://media/1', 1, 1000)")
        v1Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('content://media/2', 1, 2000)")
        // Duplicate URI
        v1Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('content://media/1', 1, 1000)")
        // Null URI
        v1Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES (NULL, 1, 3000)")
        // Empty URI
        v1Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('', 1, 4000)")

        v1Db.close()

        // Step 2: Open with Room v3 using MIGRATION_1_2 and MIGRATION_2_3
        val roomDb = Room.databaseBuilder(context, MediaDatabase::class.java, dbName)
            .addMigrations(MediaDatabase.MIGRATION_1_2, MediaDatabase.MIGRATION_2_3)
            .build()

        val favorites = roomDb.mediaDao().getFavorites().first()
        val uris = favorites.map { it.uri }.toSet()

        assertEquals(2, favorites.size)
        assertTrue(uris.contains("content://media/1"))
        assertTrue(uris.contains("content://media/2"))
        assertFalse(uris.contains(""))

        roomDb.close()
    }

    @Test
    fun missingMigration_failsLoudlyWithoutDestructiveFallback() = runTest {
        val dbName = "test_missing_migration_db"

        // Create v1 database
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE `favorites` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `uri` TEXT, `isFavorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL)")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = factory.create(config)
        helper.writableDatabase.close()

        // Build Room without MIGRATION_1_2 or fallbackToDestructiveMigration
        val roomDb = Room.databaseBuilder(context, MediaDatabase::class.java, dbName)
            .build()

        try {
            roomDb.mediaDao().getFavorites().first()
            fail("Expected IllegalStateException due to missing migration")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("migration") == true || e.message?.contains("1 to") == true)
        } finally {
            roomDb.close()
        }
    }
}
