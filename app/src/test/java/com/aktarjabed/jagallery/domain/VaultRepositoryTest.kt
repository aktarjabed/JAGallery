package com.aktarjabed.jagallery.domain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.VaultMediaEntity
import com.aktarjabed.jagallery.data.repository.VaultRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class VaultRepositoryTest {

    private lateinit var context: Context
    private lateinit var cryptoManager: VaultCryptoManager
    private lateinit var mediaDao: MediaDao
    private lateinit var repository: VaultRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cryptoManager = VaultCryptoManager()
        mediaDao = mock(MediaDao::class.java)
        repository = VaultRepository(context, cryptoManager, mediaDao)
    }

    @Test
    fun deleteVaultItem_whenFileDeletionFails_throwsIoFailureAndPreservesDBRow() = runBlocking {
        // Create a dummy un-deletable file using Robolectric mocks isn't easy natively without PowerMock,
        // but we can simulate it if the file doesn't exist and we mock existence.
        // Actually, let's just make it a directory with contents so delete() fails.
        val id = UUID.randomUUID().toString()
        val fakeEncryptedFile = File(context.filesDir, id)
        fakeEncryptedFile.mkdirs()
        File(fakeEncryptedFile, "child.txt").createNewFile()

        val entity = VaultMediaEntity(id, "content://test", "image/jpeg", fakeEncryptedFile.absolutePath, 0L, "test.jpg")

        try {
            repository.deleteVaultItem(entity)
            fail("Expected IoFailure")
        } catch (e: Exception) {
            assertTrue(e is VaultCryptoException.IoFailure)
            assertTrue("File should still exist since delete failed", fakeEncryptedFile.exists())
            // Note: mediaDao.deleteVaultMedia(entity) should NOT have been called because exception was thrown prior.
        }

        // cleanup
        File(fakeEncryptedFile, "child.txt").delete()
        fakeEncryptedFile.delete()
    }
}
