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
class MediaDatabaseMigrationTestV2toV3 {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_migration_db_v2")
    }

    @Test
    fun migration2To3_createsHiddenMediaTableAndPreservesData() = runTest {
        val dbName = "test_migration_db_v2"

        // Create v2 database
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`uri` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = factory.create(config)
        val v2Db = helper.writableDatabase

        // Insert test data
        v2Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('content://media/1', 1, 1000)")

        v2Db.close()

        // Open with Room v3 using MIGRATION_2_3
        val roomDb = Room.databaseBuilder(context, MediaDatabase::class.java, dbName)
            .addMigrations(MediaDatabase.MIGRATION_2_3, MediaDatabase.MIGRATION_3_4, MediaDatabase.MIGRATION_4_5)
            .build()

        roomDb.openHelper.writableDatabase

        // Verify existing data
        val favorites = roomDb.mediaDao().getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/1", favorites[0].uri)

        // Verify new table works
        roomDb.mediaDao().hideMedia(HiddenMediaEntity("content://media/2", true, 2000))

        val hidden = roomDb.mediaDao().getHiddenMedia().first()
        assertEquals(1, hidden.size)
        assertEquals("content://media/2", hidden[0].uri)

        roomDb.close()
    }
}
