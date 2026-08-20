package com.example.advancedgallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MediaEntity::class], version = 2, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favorites_new` (`uri` TEXT NOT NULL, `isFavorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL, PRIMARY KEY(`uri`))"
                )

                val cursor = db.query("PRAGMA table_info(`favorites`)")
                val columns = mutableListOf<String>()
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex("name")
                    if (nameIndex != -1) {
                        columns.add(cursor.getString(nameIndex))
                    }
                }
                cursor.close()

                val uriColumn = when {
                    columns.contains("uri") -> "uri"
                    columns.contains("id") -> "id"
                    else -> null
                }

                val isFavColumn = if (columns.contains("isFavorite")) "isFavorite" else "1"
                val dateAddedColumn = if (columns.contains("dateAdded")) "dateAdded" else "0"

                if (uriColumn != null) {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO `favorites_new` (`uri`, `isFavorite`, `dateAdded`)
                        SELECT CAST($uriColumn AS TEXT), $isFavColumn, $dateAddedColumn
                        FROM `favorites`
                        WHERE $uriColumn IS NOT NULL AND CAST($uriColumn AS TEXT) != ''
                        """.trimIndent()
                    )
                }

                db.execSQL("DROP TABLE IF EXISTS `favorites`")
                db.execSQL("ALTER TABLE `favorites_new` RENAME TO `favorites`")
            }
        }
    }
}
