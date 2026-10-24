package com.aktarjabed.jagallery.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FileHashDao {
    @Query("SELECT * FROM file_hashes WHERE uriStr = :uriStr")
    suspend fun getHashForUri(uriStr: String): FileHashEntity?

    /**
     * Keep each SQL IN clause below SQLite's conservative 999-variable limit.
     * Room binds one parameter for every URI, so large gallery scans are chunked here.
     */
    suspend fun getHashesForUris(uris: List<String>): List<FileHashEntity> {
        if (uris.isEmpty()) return emptyList()
        return uris.distinct().chunked(500).flatMap { chunk ->
            getHashesForUriChunk(chunk)
        }
    }

    @Query("SELECT * FROM file_hashes WHERE uriStr IN (:uris)")
    suspend fun getHashesForUriChunk(uris: List<String>): List<FileHashEntity>

    @Query("SELECT * FROM file_hashes")
    suspend fun getAllHashes(): List<FileHashEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHash(entity: FileHashEntity)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHashes(entities: List<FileHashEntity>)

    @Query("DELETE FROM file_hashes WHERE uriStr = :uriStr")
    suspend fun deleteHash(uriStr: String)
    
    @Query("DELETE FROM file_hashes WHERE lastModifiedTime < :threshold")
    suspend fun cleanupOldHashes(threshold: Long)
}
