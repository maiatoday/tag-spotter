package net.maiatoday.tagspotter.core.ui.image

import kotlin.test.Test
import kotlin.test.assertEquals

class ImageFormatTest {

    @Test
    fun thumbnailModePrefersExistingLocalThumbnailOverRemoteUrl() {
        val result = resolveImageSource(
            imagePath = "content://media/external/images/media/1",
            thumbnailPath = "/local/thumbs/thumb_1.jpg",
            remoteThumbnailUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/thumb.jpg?alt=media&token=abc",
            preferFullImage = false,
            resolveExistingLocalFile = { path -> if (path == "/local/thumbs/thumb_1.jpg") path else null }
        )

        assertEquals(ResolvedImageSource.LocalFile("/local/thumbs/thumb_1.jpg"), result)
    }

    @Test
    fun thumbnailModeFallsBackToRemoteUrlWhenOtherDeviceLocalPathsMissing() {
        val remote = "https://firebasestorage.googleapis.com/v0/b/bucket/o/thumb.jpg?alt=media&token=abc"
        val result = resolveImageSource(
            imagePath = "/data/user/0/net.maiatoday.tagspotter/files/images/img_1.jpg",
            thumbnailPath = "/data/user/0/net.maiatoday.tagspotter/files/thumbnails/thumb_1.jpg",
            remoteThumbnailUrl = remote,
            preferFullImage = false,
            resolveExistingLocalFile = { null }
        )

        assertEquals(ResolvedImageSource.RemoteOrDataUrl(remote), result)
    }

    @Test
    fun fullImageModePrefersPlatformContentUriOverLocalThumbnail() {
        val result = resolveImageSource(
            imagePath = "content://media/external/images/media/42",
            thumbnailPath = "/local/thumbs/thumb_42.jpg",
            remoteThumbnailUrl = "https://example.com/thumb.jpg",
            preferFullImage = true,
            resolveExistingLocalFile = { path -> if (path == "/local/thumbs/thumb_42.jpg") path else null }
        )

        assertEquals(ResolvedImageSource.PlatformUri("content://media/external/images/media/42"), result)
    }

    @Test
    fun fullImageModeFallsBackToLocalThumbnailThenRemoteWhenOriginalBlank() {
        val remote = "https://example.com/thumb.jpg"

        val withLocalThumb = resolveImageSource(
            imagePath = "",
            thumbnailPath = "/local/thumbs/thumb_42.jpg",
            remoteThumbnailUrl = remote,
            preferFullImage = true,
            resolveExistingLocalFile = { path -> if (path == "/local/thumbs/thumb_42.jpg") path else null }
        )
        assertEquals(ResolvedImageSource.LocalFile("/local/thumbs/thumb_42.jpg"), withLocalThumb)

        val withoutLocalThumb = resolveImageSource(
            imagePath = "",
            thumbnailPath = "/missing/thumb_42.jpg",
            remoteThumbnailUrl = remote,
            preferFullImage = true,
            resolveExistingLocalFile = { null }
        )
        assertEquals(ResolvedImageSource.RemoteOrDataUrl(remote), withoutLocalThumb)
    }

    @Test
    fun dataUriInThumbnailPathWorksWhenRemoteUrlBlank() {
        val dataUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
        val result = resolveImageSource(
            imagePath = "",
            thumbnailPath = dataUri,
            remoteThumbnailUrl = "",
            preferFullImage = false,
            resolveExistingLocalFile = { null }
        )

        assertEquals(ResolvedImageSource.RemoteOrDataUrl(dataUri), result)
    }
}
