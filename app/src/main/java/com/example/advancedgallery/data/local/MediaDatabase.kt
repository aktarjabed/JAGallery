package com.example.advancedgallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MediaEntity::class, HiddenMediaEntity::class], version = 3, exportSchema = true)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites_new` (`uri` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, PRIMARY KEY(`uri`))"
                )

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `favorites_new` (`uri`, `isFavorite`, `dateAdded`)
                    SELECT `uri`, `isFavorite`, `dateAdded`
                    FROM `favorites`
                    WHERE `uri` IS NOT NULL AND `uri` != ''
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE IF EXISTS `favorites`")
                db.execSQL("ALTER TABLE `favorites_new` RENAME TO `favorites`")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hidden_media` (`uri` TEXT NOT NULL, `isHidden` INTEGER NOT NULL, `dateHidden` INTEGER NOT NULL, PRIMARY KEY(`uri`))"
                )
            }
        }
    }
}
