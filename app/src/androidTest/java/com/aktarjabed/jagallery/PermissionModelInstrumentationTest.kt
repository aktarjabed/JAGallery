package com.aktarjabed.jagallery

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aktarjabed.jagallery.ui.common.components.PermissionAccessMode
import com.aktarjabed.jagallery.ui.common.components.checkGalleryPermissionAccessMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionModelInstrumentationTest {

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    private class PermissionTestContext(
        base: Context,
        private val grantedPermissions: Set<String>
    ) : ContextWrapper(base) {
        override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
            return if (grantedPermissions.contains(permission)) {
                PackageManager.PERMISSION_GRANTED
            } else {
                PackageManager.PERMISSION_DENIED
            }
        }

        override fun checkSelfPermission(permission: String): Int {
            return checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid())
        }
    }

    @Test
    fun checkGalleryPermissionAccessMode_evaluatesWithoutExceptions() {
        val mode = checkGalleryPermissionAccessMode(targetContext)
        assertNotNull(mode)
        assertTrue("PermissionAccessMode must be a valid enum constant", mode in PermissionAccessMode.values())
    }

    @Test
    fun permissionMapping_fullAccess_returnsFull() {
        val granted = setOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val context = PermissionTestContext(targetContext, granted)
        val mode = checkGalleryPermissionAccessMode(context)
        assertEquals("Full granted permissions must map to PermissionAccessMode.FULL", PermissionAccessMode.FULL, mode)
    }

    @Test
    fun permissionMapping_deniedAccess_returnsDenied() {
        val context = PermissionTestContext(targetContext, emptySet())
        val mode = checkGalleryPermissionAccessMode(context)
        assertEquals("No granted permissions must map to PermissionAccessMode.DENIED", PermissionAccessMode.DENIED, mode)
    }

    @Test
    fun permissionMapping_selectedPhotosVideos_returnsSelectedOnApi34Plus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val granted = setOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            val context = PermissionTestContext(targetContext, granted)
            val mode = checkGalleryPermissionAccessMode(context)
            assertEquals("READ_MEDIA_VISUAL_USER_SELECTED must map to SELECTED_PHOTOS_VIDEOS on API 34+", PermissionAccessMode.SELECTED_PHOTOS_VIDEOS, mode)
        }
    }

    @Test
    fun permissionMapping_imagesOnly_returnsImagesOnly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = setOf(Manifest.permission.READ_MEDIA_IMAGES)
            val context = PermissionTestContext(targetContext, granted)
            val mode = checkGalleryPermissionAccessMode(context)
            assertEquals("READ_MEDIA_IMAGES alone must map to IMAGES_ONLY", PermissionAccessMode.IMAGES_ONLY, mode)
        }
    }

    @Test
    fun permissionMapping_videosOnly_returnsVideosOnly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = setOf(Manifest.permission.READ_MEDIA_VIDEO)
            val context = PermissionTestContext(targetContext, granted)
            val mode = checkGalleryPermissionAccessMode(context)
            assertEquals("READ_MEDIA_VIDEO alone must map to VIDEOS_ONLY", PermissionAccessMode.VIDEOS_ONLY, mode)
        }
    }
}
