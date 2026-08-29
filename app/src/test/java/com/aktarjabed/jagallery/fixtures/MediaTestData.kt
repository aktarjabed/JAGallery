package com.aktarjabed.jagallery.fixtures

import android.net.Uri
import com.aktarjabed.jagallery.data.local.MediaEntity
import com.aktarjabed.jagallery.data.model.Album
import com.aktarjabed.jagallery.data.model.AlbumKey
import com.aktarjabed.jagallery.data.model.MediaItem
import org.mockito.Mockito.mock

object MediaTestData {

    fun mockUri(uriString: String = "content://media/external/images/media/1"): Uri {
        val uri = mock(Uri::class.java)
        org.mockito.Mockito.`when`(uri.toString()).thenReturn(uriString)
        return uri
    }

    fun image(
        id: Long = 1L,
        uriString: String = "content://media/external/images/media/$id",
        name: String = "image_$id.jpg",
        dateAdded: Long = 1000L,
        mimeType: String = "image/jpeg",
        bucketId: Long = 10L,
        bucketName: String = "Camera",
        isFavorite: Boolean = false,
        volumeName: String = "external_primary"
    ): MediaItem {
        return MediaItem(
            uri = mockUri(uriString),
            mediaStoreId = id,
            name = name,
            dateAdded = dateAdded,
            mimeType = mimeType,
            bucketId = bucketId,
            bucketName = bucketName,
            isVideo = false,
            isFavorite = isFavorite,
            volumeName = volumeName
        )
    }

    fun video(
        id: Long = 2L,
        uriString: String = "content://media/external/video/media/$id",
        name: String = "video_$id.mp4",
        dateAdded: Long = 2000L,
        mimeType: String = "video/mp4",
        bucketId: Long = 10L,
        bucketName: String = "Camera",
        isFavorite: Boolean = false,
        volumeName: String = "external_primary"
    ): MediaItem {
        return MediaItem(
            uri = mockUri(uriString),
            mediaStoreId = id,
            name = name,
            dateAdded = dateAdded,
            mimeType = mimeType,
            bucketId = bucketId,
            bucketName = bucketName,
            isVideo = true,
            isFavorite = isFavorite,
            volumeName = volumeName
        )
    }

    fun collisionFixtures(sharedStoreId: Long = 123L): Pair<MediaItem, MediaItem> {
        val imageItem = image(
            id = sharedStoreId,
            uriString = "content://media/external/images/media/$sharedStoreId",
            name = "image_$sharedStoreId.jpg"
        )
        val videoItem = video(
            id = sharedStoreId,
            uriString = "content://media/external/video/media/$sharedStoreId",
            name = "video_$sharedStoreId.mp4"
        )
        return Pair(imageItem, videoItem)
    }

    fun album(
        bucketId: Long = 10L,
        bucketName: String = "Camera",
        mediaCount: Int = 1,
        coverUri: Uri = mockUri("content://media/external/images/media/1"),
        volumeName: String = "external_primary"
    ): Album {
        return Album(
            key = AlbumKey(volumeName = volumeName, bucketId = bucketId),
            name = bucketName,
            mediaCount = mediaCount,
            coverUri = coverUri
        )
    }

    fun favorite(
        uriString: String = "content://media/external/images/media/1",
        dateAdded: Long = System.currentTimeMillis()
    ): MediaEntity {
        return MediaEntity(
            uri = uriString,
            isFavorite = true,
            dateAdded = dateAdded
        )
    }
}
