package com.example.advancedgallery.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MediaEntity::class], version = 1, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
}
