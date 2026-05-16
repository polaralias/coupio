package com.polaralias.coupio.data

import com.polaralias.coupio.data.local.CouponEntity
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import com.polaralias.coupio.data.model.DEFAULT_PENDING_WINDOW_MILLIS
import java.time.Instant
import java.time.ZoneId

class CouponLifecycleManager(
    private val pendingWindowMillis: Long = DEFAULT_PENDING_WINDOW_MILLIS,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun markShared(coupon: CouponEntity, nowEpochMillis: Long): CouponEntity = coupon.copy(
        state = CouponState.PENDING,
        shareRequestedAtEpochMillis = nowEpochMillis,
        pendingUntilEpochMillis = nowEpochMillis + pendingWindowMillis,
        availableAgainAtEpochMillis = null,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun confirmPending(coupon: CouponEntity, nowEpochMillis: Long): CouponEntity {
        val anchor = coupon.shareRequestedAtEpochMillis ?: nowEpochMillis
        val finalized = when (coupon.reusePolicy) {
            CouponReusePolicy.SINGLE_USE -> coupon.copy(
                state = CouponState.LOCKED,
                shareRequestedAtEpochMillis = null,
                pendingUntilEpochMillis = null,
                availableAgainAtEpochMillis = null,
                updatedAtEpochMillis = nowEpochMillis,
            )

            CouponReusePolicy.DAILY -> coupon.copy(
                state = CouponState.LOCKED,
                shareRequestedAtEpochMillis = null,
                pendingUntilEpochMillis = null,
                availableAgainAtEpochMillis = shift(anchor) { it.plusDays(1) },
                updatedAtEpochMillis = nowEpochMillis,
            )

            CouponReusePolicy.WEEKLY -> coupon.copy(
                state = CouponState.LOCKED,
                shareRequestedAtEpochMillis = null,
                pendingUntilEpochMillis = null,
                availableAgainAtEpochMillis = shift(anchor) { it.plusWeeks(1) },
                updatedAtEpochMillis = nowEpochMillis,
            )

            CouponReusePolicy.MONTHLY -> coupon.copy(
                state = CouponState.LOCKED,
                shareRequestedAtEpochMillis = null,
                pendingUntilEpochMillis = null,
                availableAgainAtEpochMillis = shift(anchor) { it.plusMonths(1) },
                updatedAtEpochMillis = nowEpochMillis,
            )

            CouponReusePolicy.ALWAYS -> coupon.copy(
                state = CouponState.AVAILABLE,
                shareRequestedAtEpochMillis = null,
                pendingUntilEpochMillis = null,
                availableAgainAtEpochMillis = null,
                updatedAtEpochMillis = nowEpochMillis,
            )
        }

        return reconcile(finalized, nowEpochMillis) ?: finalized
    }

    fun revertPending(coupon: CouponEntity, nowEpochMillis: Long): CouponEntity = coupon.copy(
        state = CouponState.AVAILABLE,
        shareRequestedAtEpochMillis = null,
        pendingUntilEpochMillis = null,
        availableAgainAtEpochMillis = null,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun reissue(coupon: CouponEntity, nowEpochMillis: Long): CouponEntity = coupon.copy(
        state = CouponState.AVAILABLE,
        shareRequestedAtEpochMillis = null,
        pendingUntilEpochMillis = null,
        availableAgainAtEpochMillis = null,
        updatedAtEpochMillis = nowEpochMillis,
    )

    fun reconcile(coupon: CouponEntity, nowEpochMillis: Long): CouponEntity? {
        return when {
            coupon.state == CouponState.PENDING &&
                coupon.pendingUntilEpochMillis != null &&
                nowEpochMillis >= coupon.pendingUntilEpochMillis -> confirmPending(coupon, nowEpochMillis)

            coupon.state == CouponState.LOCKED &&
                coupon.availableAgainAtEpochMillis != null &&
                nowEpochMillis >= coupon.availableAgainAtEpochMillis -> reissue(coupon, nowEpochMillis)

            else -> null
        }
    }

    private fun shift(epochMillis: Long, block: (java.time.ZonedDateTime) -> java.time.ZonedDateTime): Long {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        return block(zonedDateTime).toInstant().toEpochMilli()
    }
}
