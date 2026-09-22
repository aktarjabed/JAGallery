package com.aktarjabed.jagallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MediaEntity::class, HiddenMediaEntity::class, TrashMediaEntity::class, VaultMediaEntity::class], version = 5, exportSchema = true)
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trash_media` (`uri` TEXT NOT NULL, `dateTrashed` INTEGER NOT NULL, PRIMARY KEY(`uri`))"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `vault_media` (`id` TEXT NOT NULL, `originalUriStr` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `encryptedFilePath` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `originalName` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }
    }
}
