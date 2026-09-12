package com.aktarjabed.jagallery.ui.common

import android.content.ContentResolver
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperationsImpl
import com.aktarjabed.jagallery.fakes.FakeMediaDao
import com.aktarjabed.jagallery.rules.MainDispatcherRule
import org.junit.Before
import org.junit.Rule
import org.mockito.Mockito.mock

open class BaseMediaViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected lateinit var fakeDao: FakeMediaDao
    protected lateinit var repository: MediaRepository
    protected lateinit var mediaOperations: MediaOperationsImpl
    protected val contentResolver: ContentResolver = mock(ContentResolver::class.java)

    @Before
    open fun setUp() {
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
        mediaOperations = MediaOperationsImpl(repository)
    }
}
