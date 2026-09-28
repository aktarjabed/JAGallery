package com.aktarjabed.jagallery.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM favorites WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaEntity: MediaEntity)

    @Query("DELETE FROM favorites WHERE uri = :uri")
    suspend fun removeFavorite(uri: String)

    @Query("SELECT * FROM favorites WHERE uri = :uri")
    suspend fun getFavoriteById(uri: String): MediaEntity?

    @Query("DELETE FROM favorites WHERE uri IN (:uris)")
    suspend fun removeFavorites(uris: List<String>)

    @Query("SELECT * FROM hidden_media WHERE isHidden = 1")
    fun getHiddenMedia(): Flow<List<HiddenMediaEntity>>

    @Query("SELECT * FROM hidden_media WHERE uri = :uri")
    suspend fun getHiddenMediaById(uri: String): HiddenMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideMedia(hiddenEntity: HiddenMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hideMediaBatch(hiddenEntities: List<HiddenMediaEntity>)

    @Query("DELETE FROM hidden_media WHERE uri = :uri")
    suspend fun unhideMedia(uri: String)

    @Query("DELETE FROM hidden_media WHERE uri IN (:uris)")
    suspend fun unhideMediaBatch(uris: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashMediaBatch(trashEntities: List<TrashMediaEntity>)

    @Query("DELETE FROM trash_media WHERE uri IN (:uris)")
    suspend fun removeTrashMediaBatch(uris: List<String>)

    @Query("SELECT * FROM trash_media")
    suspend fun getAllTrashMediaSync(): List<TrashMediaEntity>

    @Query("SELECT * FROM vault_media")
    fun getVaultMedia(): Flow<List<VaultMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultMedia(vaultEntity: VaultMediaEntity)

    @Delete
    suspend fun deleteVaultMedia(vaultEntity: VaultMediaEntity)
}
