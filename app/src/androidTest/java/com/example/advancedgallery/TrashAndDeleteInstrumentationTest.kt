package com.example.advancedgallery

import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.advancedgallery.util.FileUtils
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashAndDeleteInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver

    @Test
    fun createTrashRequest_returnsPendingIntentOnSupportedApi() {
        val testUris = listOf(Uri.parse("content://media/external/images/media/999999"))
        val pendingIntent = FileUtils.createTrashRequest(resolver, testUris, true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertNotNull("createTrashRequest must return non-null PendingIntent on API 30+", pendingIntent)
        } else {
            assertNull(pendingIntent)
        }
    }

    @Test
    fun createDeleteRequest_returnsPendingIntentOnSupportedApi() {
        val testUris = listOf(Uri.parse("content://media/external/images/media/999999"))
        val pendingIntent = FileUtils.createDeleteRequest(resolver, testUris)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertNotNull("createDeleteRequest must return non-null PendingIntent on API 30+", pendingIntent)
        } else {
            assertNull(pendingIntent)
        }
    }
}
