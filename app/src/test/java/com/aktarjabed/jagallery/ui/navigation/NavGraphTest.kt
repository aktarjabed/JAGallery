package com.aktarjabed.jagallery.ui.navigation

import com.aktarjabed.jagallery.data.model.AlbumKey
import com.aktarjabed.jagallery.data.model.MediaSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavGraphTest {

    @Test
    fun parseMediaSource_validAlbumRoute_returnsAlbumSourceWithVolumeName() {
        val source = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = "external_primary",
            bucketId = 123L,
            searchQuery = null
        )
        assertEquals(MediaSource.Album(AlbumKey("external_primary", 123L)), source)
    }

    @Test
    fun parseMediaSource_albumRouteMissingVolumeOrBucket_returnsNull() {
        val missingVolume = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = null,
            bucketId = 123L,
            searchQuery = null
        )
        assertNull(missingVolume)

        val missingBucket = parseMediaSource(
            sourceStr = "ALBUM",
            volumeName = "external_primary",
            bucketId = null,
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
            searchQuery = "vacation"
        )
        assertEquals(MediaSource.Search("vacation"), search)
    }
}
