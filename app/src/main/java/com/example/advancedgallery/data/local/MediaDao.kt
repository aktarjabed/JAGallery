package com.example.advancedgallery.data.local

import androidx.room.Dao
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
}
