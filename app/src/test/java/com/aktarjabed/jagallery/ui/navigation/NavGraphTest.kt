package com.aktarjabed.jagallery.ui.navigation

import com.aktarjabed.jagallery.data.model.AlbumKey
import com.aktarjabed.jagallery.data.model.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavGraphTest {

    @Test
    fun parseMediaSource_validAlbumRoute_returnsAlbumSourceWithVolumeName() {
        val source = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = "external_primary",
            bucketId = 123L,
            relativePath = "Pictures/",
            searchQuery = null
        )
        assertEquals(MediaSource.Album(AlbumKey("external_primary", 123L, "Pictures/")), source)
    }

    @Test
    fun parseMediaSource_albumRouteMissingVolumeOrBucket_returnsNull() {
        val missingVolume = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = null,
            bucketId = 123L,
            relativePath = null,
            searchQuery = null
        )
        assertNull(missingVolume)

        val missingBucket = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = "external_primary",
            bucketId = null,
            relativePath = null,
            searchQuery = null
        )
        assertNull(missingBucket)
    }

    @Test
    fun parseMediaSource_searchRouteBlankQuery_returnsNull() {
        val blankSearch = parseMediaSource(
            sourceStr = "SEARCH",
            volumeName = null,
            bucketId = null,
            relativePath = null,
            searchQuery = ""
        )
        assertNull(blankSearch)
    }

    @Test
    fun parseMediaSource_validSearchRoute_returnsSearchSource() {
        val search = parseMediaSource(
            sourceStr = "SEARCH",
            volumeName = null,
            bucketId = null,
            relativePath = null,
            searchQuery = "vacation"
        )
        assertEquals(MediaSource.Search("vacation"), search)
    }

    @Test
    fun roundTrip_albumWithSpecialCharacters_isDecodedCorrectly() {
        val rawRelativePath = "Pictures/My Vacation 100% #1?/Nested/"
        val rawVolumeName = "volume name with spaces"
        val bucketId = 555L

        val originalSource = MediaSource.Album(AlbumKey(rawVolumeName, bucketId, rawRelativePath))

        // 1. Create Route
        val route = Screen.Grid.createRoute(originalSource)

        // The route is something like "grid?source=ALBUM&volumeName=volume%20name%20with%20spaces&bucketId=555&relativePath=Pictures%2FMy%20Vacation%20100%25%20%231%3F%2FNested%2F"
        // We manually extract arguments to simulate NavGraph backStackEntry behavior
        val params = route.substringAfter("?").split("&").associate {
            val parts = it.split("=")
            parts[0] to (if (parts.size > 1) parts[1] else "")
        }

        val sourceStr = params["source"]
        val volumeNameStr = params["volumeName"]
        val bucketIdStr = params["bucketId"]
        val relativePathStr = params["relativePath"]

        // 2. Decode as done in NavGraph/ViewModels. Note: Compose Navigation uses Uri.decode internally.
        // We use java.net.URLDecoder.decode here as an approximation for the test.
        val decodedVolumeName = volumeNameStr?.let { java.net.URLDecoder.decode(it, "UTF-8") }
        val decodedBucketId = bucketIdStr?.toLongOrNull()
        val decodedRelativePath = relativePathStr?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""

        // 3. Parse Source
        val parsedSource = parseMediaSource(
            sourceStr = sourceStr,
            volumeName = decodedVolumeName,
            bucketId = decodedBucketId,
            relativePath = decodedRelativePath,
            searchQuery = null
        )
        assertEquals(originalSource, parsedSource)
    }
}
