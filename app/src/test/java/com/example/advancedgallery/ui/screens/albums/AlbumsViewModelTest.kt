package com.example.advancedgallery.ui.screens.albums

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.fakes.FakeMediaDao
import com.example.advancedgallery.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AlbumsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun albums_initialStateIsEmptyOrLoading() = runTest {
        val viewModel = AlbumsViewModel(repository, context)
        val state = viewModel.uiState.value
        assertTrue(state is AlbumsUiState.Loading || state is AlbumsUiState.Empty)
    }
}
