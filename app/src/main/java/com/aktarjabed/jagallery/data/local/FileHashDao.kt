package com.aktarjabed.jagallery.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileHashDao {
    @Query("SELECT * FROM file_hashes WHERE uriStr = :uriStr")
    suspend fun getHashForUri(uriStr: String): FileHashEntity?

    @Query("SELECT * FROM file_hashes")
    suspend fun getAllHashes(): List<FileHashEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHash(entity: FileHashEntity)

    @Query("DELETE FROM file_hashes WHERE uriStr = :uriStr")
    suspend fun deleteHash(uriStr: String)
    
    @Query("DELETE FROM file_hashes WHERE lastModifiedTime < :threshold")
    suspend fun cleanupOldHashes(threshold: Long)
}
