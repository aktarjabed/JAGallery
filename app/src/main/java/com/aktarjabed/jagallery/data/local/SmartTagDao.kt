package com.aktarjabed.jagallery.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SmartTagDao {
    @Query("SELECT * FROM smart_tags WHERE uriStr = :uriStr")
    suspend fun getTagForUri(uriStr: String): SmartTagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(entity: SmartTagEntity)
}
