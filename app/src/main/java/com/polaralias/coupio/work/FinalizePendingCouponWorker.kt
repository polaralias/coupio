package com.polaralias.coupio.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.polaralias.coupio.CouponApp

class FinalizePendingCouponWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val couponId = inputData.getString(KEY_COUPON_ID) ?: return Result.failure()
        return try {
            (applicationContext as CouponApp).container.couponRepository.confirmPending(couponId)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_COUPON_ID = "coupon_id"
    }
}
