package com.aktarjabed.jagallery.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileHashDaoLargeQueryTest {
    private lateinit var database: MediaDatabase
    private lateinit var dao: FileHashDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.fileHashDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getHashesForUris_handlesMoreThan999Uris() = runBlocking {
        val entities = (0 until 1_205).map { index ->
            FileHashEntity(
                uriStr = "content://media/external/images/media/$index",
                sha256Hash = "sha-$index",
                perceptualHash = index.toLong(),
                lastModifiedTime = index.toLong()
            )
        }
        dao.insertHashes(entities)

        val requested = entities.map { it.uriStr }
        val result = dao.getHashesForUris(requested)

        assertEquals(1_205, result.size)
        assertEquals(requested.toSet(), result.map { it.uriStr }.toSet())
    }
}
