package com.aktarjabed.jagallery.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaDatabaseMigrationTestV3toV4 {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_migration_db_v3")
    }

    @Test
    fun migration3To4_createsTrashMediaTableAndPreservesData() = runTest {
        val dbName = "test_migration_db_v3"

        // Create v3 database
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`uri` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `hidden_media` (`uri` TEXT NOT NULL, `isHidden` INTEGER NOT NULL, `dateHidden` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = factory.create(config)
        val v3Db = helper.writableDatabase

        // Insert test data
        v3Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('content://media/1', 1, 1000)")
        v3Db.execSQL("INSERT INTO `hidden_media` (`uri`, `isHidden`, `dateHidden`) VALUES ('content://media/2', 1, 2000)")

        v3Db.close()

        // Open with Room v4 using MIGRATION_3_4
        val roomDb = Room.databaseBuilder(context, MediaDatabase::class.java, dbName)
            .addMigrations(MediaDatabase.MIGRATION_3_4, MediaDatabase.MIGRATION_4_5)
            .build()

        roomDb.openHelper.writableDatabase

        // Verify existing data
        val favorites = roomDb.mediaDao().getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/1", favorites[0].uri)

        val hidden = roomDb.mediaDao().getHiddenMedia().first()
        assertEquals(1, hidden.size)
        assertEquals("content://media/2", hidden[0].uri)

        // Verify new table works
        roomDb.mediaDao().insertTrashMediaBatch(listOf(
            TrashMediaEntity("content://media/3", 3000)
        ))

        val trash = roomDb.mediaDao().getAllTrashMediaSync()
        assertEquals(1, trash.size)
        assertEquals("content://media/3", trash[0].uri)

        roomDb.close()
    }
}
