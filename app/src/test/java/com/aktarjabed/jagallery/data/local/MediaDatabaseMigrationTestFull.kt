package com.aktarjabed.jagallery.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaDatabaseMigrationTestFull {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_migration_db_v1")
    }

    @Test
    fun migration1To5_runsSuccessfully() = runTest {
        val dbName = "test_migration_db_v1"

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
        val v1Db = helper.writableDatabase

        // Insert test data
        v1Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('content://media/1', 1, 1000)")

        v1Db.close()

        // Open with Room v5 using all migrations
        val roomDb = Room.databaseBuilder(context, MediaDatabase::class.java, dbName)
            .addMigrations(
                MediaDatabase.MIGRATION_1_2,
                MediaDatabase.MIGRATION_2_3,
                MediaDatabase.MIGRATION_3_4,
                MediaDatabase.MIGRATION_4_5
            )
            .build()

        roomDb.openHelper.writableDatabase

        // Verify existing data survived all the way
        val favorites = roomDb.mediaDao().getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/1", favorites[0].uri)

        roomDb.close()
    }
}
