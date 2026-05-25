package com.polaralias.coupio.data

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.polaralias.coupio.data.local.CouponDao
import com.polaralias.coupio.data.local.CouponEntity
import com.polaralias.coupio.data.local.displayTitle
import com.polaralias.coupio.data.local.isExpired
import com.polaralias.coupio.data.model.CouponDraftInput
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.data.model.CouponMetadataUpdate
import com.polaralias.coupio.data.model.PreparedShare
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import com.polaralias.coupio.work.FinalizePendingCouponWorker
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CouponRepository(
    private val couponDao: CouponDao,
    private val lifecycleManager: CouponLifecycleManager,
    private val scheduler: CouponScheduler,
    private val mediaStorage: CouponMediaStorage,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeCoupons(): Flow<List<CouponEntity>> = couponDao.observeAll()

    suspend fun getCoupon(couponId: String): CouponEntity? = couponDao.getById(couponId)

    suspend fun importCoupon(input: CouponDraftInput) {
        val now = nowEpochMillis()
        val mediaFile = mediaStorage.persist(input)
        val coupon = CouponEntity(
            id = UUID.randomUUID().toString(),
            mediaPath = mediaFile.absolutePath,
            mediaType = input.mediaType,
            mediaMimeType = input.mediaMimeType,
            mediaDisplayName = input.mediaDisplayName,
            title = input.title.clean(),
            description = input.description.clean(),
            category = input.category.clean(),
            expiryEpochDay = input.expiryDate?.toEpochDay(),
            reusePolicy = input.reusePolicy,
            state = CouponState.AVAILABLE,
            shareRequestedAtEpochMillis = null,
            pendingUntilEpochMillis = null,
            availableAgainAtEpochMillis = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        couponDao.insert(coupon)
    }

    suspend fun updateCouponMetadata(input: CouponMetadataUpdate) {
        val now = nowEpochMillis()
        val current = couponDao.getById(input.couponId) ?: return
        couponDao.update(
            current.copy(
                title = input.title.clean(),
                description = input.description.clean(),
                category = input.category.clean(),
                expiryEpochDay = input.expiryDate?.toEpochDay(),
                reusePolicy = input.reusePolicy,
                updatedAtEpochMillis = now,
            ),
        )
    }

    suspend fun prepareShare(couponId: String): PreparedShare? {
        val now = nowEpochMillis()
        val current = getFreshCoupon(couponId, now) ?: return null
        if (current.state != CouponState.AVAILABLE || current.isExpired(now)) return null

        val pending = lifecycleManager.markShared(current, now)
        couponDao.update(pending)
        scheduler.scheduleFinalize(pending.id, pending.pendingUntilEpochMillis ?: now)

        return PreparedShare(
            mediaPath = pending.mediaPath,
            mimeType = pending.mediaMimeType,
            subject = pending.displayTitle(),
        )
    }

    suspend fun confirmPending(couponId: String) {
        val now = nowEpochMillis()
        val current = couponDao.getById(couponId) ?: return
        if (current.state != CouponState.PENDING) return
        scheduler.cancelFinalize(couponId)
        couponDao.update(lifecycleManager.confirmPending(current, now))
    }

    suspend fun revertPending(couponId: String) {
        val now = nowEpochMillis()
        val current = couponDao.getById(couponId) ?: return
        if (current.state != CouponState.PENDING) return
        scheduler.cancelFinalize(couponId)
        couponDao.update(lifecycleManager.revertPending(current, now))
    }

    suspend fun reissueCoupon(couponId: String) {
        val now = nowEpochMillis()
        val current = couponDao.getById(couponId) ?: return
        scheduler.cancelFinalize(couponId)
        couponDao.update(lifecycleManager.reissue(current, now))
    }

    suspend fun reconcileDueCoupons(nowEpochMillis: Long = System.currentTimeMillis()) {
        couponDao.getPendingDue(nowEpochMillis).forEach { coupon ->
            couponDao.update(lifecycleManager.confirmPending(coupon, nowEpochMillis))
        }
        couponDao.getUnlocksDue(nowEpochMillis).forEach { coupon ->
            couponDao.update(lifecycleManager.reissue(coupon, nowEpochMillis))
        }
    }

    private suspend fun getFreshCoupon(couponId: String, nowEpochMillis: Long): CouponEntity? {
        val current = couponDao.getById(couponId) ?: return null
        val reconciled = lifecycleManager.reconcile(current, nowEpochMillis) ?: return current
        couponDao.update(reconciled)
        return reconciled
    }
}

interface CouponMediaStorage {
    suspend fun persist(input: CouponDraftInput): File
}

class AndroidCouponMediaStorage(private val context: Context) : CouponMediaStorage {
    override suspend fun persist(input: CouponDraftInput): File = withContext(Dispatchers.IO) {
        val couponsDirectory = File(context.filesDir, "coupons").apply { mkdirs() }
        val extension = resolveExtension(
            mimeType = input.mediaMimeType,
            displayName = input.mediaDisplayName,
            mediaType = input.mediaType,
        )
        val destination = File(couponsDirectory, "${UUID.randomUUID()}.$extension")

        when {
            input.tempFilePath != null -> {
                val sourceFile = File(input.tempFilePath)
                sourceFile.copyTo(destination, overwrite = true)
                sourceFile.delete()
            }

            input.sourceUri != null -> {
                val sourceUri = Uri.parse(input.sourceUri)
                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    destination.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: error("Unable to open source URI: $sourceUri")
            }

            else -> error("No media source supplied")
        }

        destination
    }
}

interface CouponScheduler {
    fun scheduleFinalize(couponId: String, pendingUntilEpochMillis: Long)

    fun cancelFinalize(couponId: String)
}

class WorkManagerCouponScheduler(
    private val context: Context,
    private val nowEpochMillis: () -> Long = System::currentTimeMillis,
) : CouponScheduler {
    override fun scheduleFinalize(couponId: String, pendingUntilEpochMillis: Long) {
        val delay = (pendingUntilEpochMillis - nowEpochMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<FinalizePendingCouponWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(FinalizePendingCouponWorker.KEY_COUPON_ID, couponId).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkName(couponId),
            androidx.work.ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun cancelFinalize(couponId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(couponId))
    }

    private fun uniqueWorkName(couponId: String): String = "coupon-finalize-$couponId"
}

private fun resolveExtension(
    mimeType: String,
    displayName: String,
    mediaType: CouponMediaType,
): String {
    val explicitExtension = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (explicitExtension.isNotBlank()) return explicitExtension

    return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.lowercase(Locale.ROOT)
        ?: when (mediaType) {
            CouponMediaType.PDF -> "pdf"
            CouponMediaType.IMAGE -> "jpg"
        }
}

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
