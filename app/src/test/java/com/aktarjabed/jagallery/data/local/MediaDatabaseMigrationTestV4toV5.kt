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
class MediaDatabaseMigrationTestV4toV5 {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("test_migration_db_v4")
    }

    @Test
    fun migration4To5_createsVaultMediaTableAndPreservesData() = runTest {
        val dbName = "test_migration_db_v4"

        // Create v4 database
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`uri` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `hidden_media` (`uri` TEXT NOT NULL, `isHidden` INTEGER NOT NULL, `dateHidden` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `trash_media` (`uri` TEXT NOT NULL, `dateTrashed` INTEGER NOT NULL, PRIMARY KEY(`uri`))")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = factory.create(config)
        val v4Db = helper.writableDatabase

        // Insert test data
        v4Db.execSQL("INSERT INTO `favorites` (`uri`, `isFavorite`, `dateAdded`) VALUES ('content://media/1', 1, 1000)")
        v4Db.execSQL("INSERT INTO `hidden_media` (`uri`, `isHidden`, `dateHidden`) VALUES ('content://media/2', 1, 2000)")
        v4Db.execSQL("INSERT INTO `trash_media` (`uri`, `dateTrashed`) VALUES ('content://media/3', 3000)")

        v4Db.close()

        // Open with Room v5 using MIGRATION_4_5
        val roomDb = Room.databaseBuilder(context, MediaDatabase::class.java, dbName)
            .addMigrations(MediaDatabase.MIGRATION_4_5)
            .build()

        roomDb.openHelper.writableDatabase

        // Verify existing data
        val favorites = roomDb.mediaDao().getFavorites().first()
        assertEquals(1, favorites.size)

        val hidden = roomDb.mediaDao().getHiddenMedia().first()
        assertEquals(1, hidden.size)

        val trash = roomDb.mediaDao().getAllTrashMediaSync()
        assertEquals(1, trash.size)

        // Verify new table works
        val vaultEntity = VaultMediaEntity(
            "uuid-1", "content://media/4", "image/jpeg", "/path/to/encrypted", 4000, "secret.jpg"
        )
        roomDb.mediaDao().insertVaultMedia(vaultEntity)

        val vaultItems = roomDb.mediaDao().getVaultMedia().first()
        assertEquals(1, vaultItems.size)
        assertEquals("uuid-1", vaultItems[0].id)

        roomDb.close()
    }
}
