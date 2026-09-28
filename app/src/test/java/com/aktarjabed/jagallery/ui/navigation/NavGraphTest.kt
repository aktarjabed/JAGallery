package com.aktarjabed.jagallery.ui.navigation

import com.aktarjabed.jagallery.data.model.AlbumKey
import com.aktarjabed.jagallery.data.model.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.net.Uri

@RunWith(RobolectricTestRunner::class)
class NavGraphTest {

    @Test
    fun parseMediaSource_validAlbumRoute_returnsAlbumSourceWithVolumeName() {
        val source = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = com.aktarjabed.jagallery.util.NavCodec.encode("external_primary"),
            bucketId = 123L,
            relativePath = com.aktarjabed.jagallery.util.NavCodec.encode("Pictures/"),
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
            volumeName = com.aktarjabed.jagallery.util.NavCodec.encode("external_primary"),
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
            searchQuery = com.aktarjabed.jagallery.util.NavCodec.encode("vacation")
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

        // Decode full URL route because NavArguments are automatically internally decoded by NavHost components on Android
        val decodedRoute = Uri.decode(route)

        val params = decodedRoute.substringAfter("?").split("&").associate {
            val parts = it.split("=")
            parts[0] to (if (parts.size > 1) parts[1] else "")
        }

        val sourceStr = params["source"]
        val volumeNameStr = params["volumeName"]
        val bucketIdStr = params["bucketId"]
        val relativePathStr = params["relativePath"]

        val decodedBucketId = bucketIdStr?.toLongOrNull()

        // 3. Parse Source (decode happens inside parseMediaSource now natively via NavCodec)
        val parsedSource = parseMediaSource(
            sourceStr = sourceStr,
            volumeName = volumeNameStr,
            bucketId = decodedBucketId,
            relativePath = relativePathStr,
            searchQuery = null
        )
        assertEquals(originalSource, parsedSource)
    }
}
