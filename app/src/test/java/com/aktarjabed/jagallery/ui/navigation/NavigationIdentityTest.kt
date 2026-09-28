package com.aktarjabed.jagallery.ui.navigation

import com.aktarjabed.jagallery.data.model.AlbumKey
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.util.NavCodec
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.net.Uri

@RunWith(RobolectricTestRunner::class)
class NavigationIdentityTest {

    @Test
    fun gridRoute_withAlbumSource_preservesIdentity() {
        val originalVolume = "external_primary"
        val originalRelativePath = "Pictures/My %25 Album+Test/"
        val bucketId = 12345L

        val albumSource = MediaSource.Album(AlbumKey(originalVolume, bucketId, originalRelativePath))
        val route = Screen.Grid.createRoute(albumSource)

        // Simulating extraction from route string, finding the encoded parameters:
        val encodedVolume = NavCodec.encode(originalVolume)
        val encodedRelativePath = NavCodec.encode(originalRelativePath)

        // Ensure the route contains the encoded parts
        assert(Uri.decode(route).contains(encodedVolume))
        assert(Uri.decode(route).contains(encodedRelativePath))

        // Parse back mimicking NavGraph arguments
        val parsedSource = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = encodedVolume,
            bucketId = bucketId,
            relativePath = encodedRelativePath,
            searchQuery = null
        )

        assertEquals(albumSource, parsedSource)
    }

    @Test
    fun viewerRoute_withSearchSource_preservesIdentity() {
        val originalQuery = "My Query % + # ?"
        val searchSource = MediaSource.Search(originalQuery)
        val mediaId = "100"

        val route = Screen.Viewer.createRoute(mediaId, searchSource)
        val encodedQuery = NavCodec.encode(originalQuery)
        val encodedMediaId = NavCodec.encode(mediaId)

        assert(Uri.decode(route).contains(encodedQuery))
        assert(Uri.decode(route).contains(encodedMediaId))

        val parsedSource = parseMediaSource(
            sourceStr = "SEARCH",
            volumeName = null,
            bucketId = null,
            relativePath = null,
            searchQuery = encodedQuery
        )

        assertEquals(searchSource, parsedSource)
    }
}
