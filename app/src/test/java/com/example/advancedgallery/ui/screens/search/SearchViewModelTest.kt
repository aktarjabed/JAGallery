package com.example.advancedgallery.ui.screens.search

import android.content.ContentResolver
import androidx.lifecycle.SavedStateHandle
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.fakes.FakeMediaDao
import com.example.advancedgallery.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)

    @Before
    fun setUp() {
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun updateSearchQuery_updatesQueryState() = runTest {
        val viewModel = SearchViewModel(repository, SavedStateHandle())
        assertEquals("", viewModel.searchQuery.value)

        viewModel.updateSearchQuery("vacation")
        assertEquals("vacation", viewModel.searchQuery.value)
    }
}
