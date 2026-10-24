package com.aktarjabed.jagallery.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmartTagDao {
    @Query("SELECT * FROM smart_tags WHERE uriStr = :uriStr")
    suspend fun getTagForUri(uriStr: String): SmartTagEntity?

    @Query("SELECT * FROM smart_tags")
    fun getAllTagsFlow(): Flow<List<SmartTagEntity>>

    @Query(
        """
        SELECT * FROM smart_tags 
        WHERE primaryLabel LIKE '%' || :query || '%' 
           OR locationName LIKE '%' || :query || '%' 
           OR extractedText LIKE '%' || :query || '%'
        """
    )
    suspend fun searchTags(query: String): List<SmartTagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(entity: SmartTagEntity)
}
