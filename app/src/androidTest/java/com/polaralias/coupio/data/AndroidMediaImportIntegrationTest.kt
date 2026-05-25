package com.polaralias.coupio.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.polaralias.coupio.createCameraCaptureTarget
import com.polaralias.coupio.createShareUri
import com.polaralias.coupio.data.local.AppDatabase
import com.polaralias.coupio.data.model.CouponDraftInput
import com.polaralias.coupio.data.model.CouponMediaType
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMediaImportIntegrationTest {
    private lateinit var appContext: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: CouponRepository

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        File(appContext.filesDir, "coupons").deleteRecursively()
        File(appContext.cacheDir, "captures").deleteRecursively()

        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = CouponRepository(
            couponDao = database.couponDao(),
            lifecycleManager = CouponLifecycleManager(),
            scheduler = NoopCouponScheduler,
            mediaStorage = AndroidCouponMediaStorage(appContext),
        )
    }

    @After
    fun tearDown() {
        database.close()
        File(appContext.filesDir, "coupons").deleteRecursively()
        File(appContext.cacheDir, "captures").deleteRecursively()
    }

    @Test
    fun importCoupon_persistsImageFromTempFileIntoAppStorage() = runBlocking {
        val sourceFile = createSourceFile("picker-image.png", byteArrayOf(1, 2, 3, 4))

        repository.importCoupon(
            CouponDraftInput(
                tempFilePath = sourceFile.absolutePath,
                mediaMimeType = "image/png",
                mediaType = CouponMediaType.IMAGE,
                mediaDisplayName = "picker-image.png",
                title = "Image Import Test",
                description = null,
                category = null,
                expiryDate = null,
            ),
        )

        val imported = database.couponDao().observeAll().first().single()
        assertEquals("Image Import Test", imported.title)
        assertEquals(CouponMediaType.IMAGE, imported.mediaType)
        assertEquals("image/png", imported.mediaMimeType)
        assertTrue(imported.mediaPath.startsWith(File(appContext.filesDir, "coupons").absolutePath))
        assertTrue(File(imported.mediaPath).exists())
        assertFalse(sourceFile.exists())
    }

    @Test
    fun importCoupon_persistsPdfFromFileProviderSourceUri() = runBlocking {
        val sourceFile = createSourceFile("picker.pdf", "%PDF-debug".toByteArray())
        val sourceUri = createShareUri(appContext, sourceFile.absolutePath)

        repository.importCoupon(
            CouponDraftInput(
                sourceUri = sourceUri.toString(),
                mediaMimeType = "application/pdf",
                mediaType = CouponMediaType.PDF,
                mediaDisplayName = "picker.pdf",
                title = "PDF Import Test",
                description = null,
                category = null,
                expiryDate = null,
            ),
        )

        val imported = database.couponDao().observeAll().first().single()
        assertEquals("PDF Import Test", imported.title)
        assertEquals(CouponMediaType.PDF, imported.mediaType)
        assertEquals("application/pdf", imported.mediaMimeType)
        assertTrue(imported.mediaPath.endsWith(".pdf"))
        assertTrue(File(imported.mediaPath).exists())
        assertTrue(sourceFile.exists())
    }

    @Test
    fun createCameraCaptureTarget_createsFileProviderUriInCaptureCache() {
        val captureTarget = createCameraCaptureTarget(appContext)

        assertTrue(captureTarget.filePath.startsWith(File(appContext.cacheDir, "captures").absolutePath))
        assertTrue(captureTarget.displayName.endsWith(".jpg"))
        assertEquals(
            "${appContext.packageName}.fileprovider",
            captureTarget.uri.authority,
        )
        assertNotNull(captureTarget.uri.path)
    }

    private fun createSourceFile(fileName: String, bytes: ByteArray): File {
        val directory = File(appContext.cacheDir, "captures").apply { mkdirs() }
        return File(directory, fileName).apply {
            writeBytes(bytes)
        }
    }
}

private object NoopCouponScheduler : CouponScheduler {
    override fun scheduleFinalize(couponId: String, pendingUntilEpochMillis: Long) = Unit

    override fun cancelFinalize(couponId: String) = Unit
}
