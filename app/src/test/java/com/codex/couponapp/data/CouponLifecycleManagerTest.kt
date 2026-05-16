package com.polaralias.coupio.data

import com.polaralias.coupio.data.local.CouponEntity
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CouponLifecycleManagerTest {
    private val lifecycleManager = CouponLifecycleManager()

    @Test
    fun `single use locks permanently after confirmation`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.SINGLE_USE), NOW)

        val confirmed = lifecycleManager.confirmPending(shared, NOW + 1_000)

        assertEquals(CouponState.LOCKED, confirmed.state)
        assertNull(confirmed.availableAgainAtEpochMillis)
        assertNull(confirmed.pendingUntilEpochMillis)
    }

    @Test
    fun `always policy returns to available after confirmation`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.ALWAYS), NOW)

        val confirmed = lifecycleManager.confirmPending(shared, NOW + 1_000)

        assertEquals(CouponState.AVAILABLE, confirmed.state)
        assertNull(confirmed.availableAgainAtEpochMillis)
    }

    @Test
    fun `daily policy unlocks after cooldown`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.DAILY), NOW)
        val confirmed = lifecycleManager.confirmPending(shared, NOW + 1_000)

        val unlocked = lifecycleManager.reconcile(confirmed, NOW + DAY_MILLIS + 5_000)

        requireNotNull(unlocked)
        assertEquals(CouponState.AVAILABLE, unlocked.state)
        assertNull(unlocked.availableAgainAtEpochMillis)
    }

    private fun baseCoupon(reusePolicy: CouponReusePolicy) = CouponEntity(
        id = "coupon-id",
        mediaPath = "coupon.jpg",
        mediaType = CouponMediaType.IMAGE,
        mediaMimeType = "image/jpeg",
        mediaDisplayName = "coupon.jpg",
        title = "Test",
        description = null,
        category = null,
        expiryEpochDay = null,
        reusePolicy = reusePolicy,
        state = CouponState.AVAILABLE,
        shareRequestedAtEpochMillis = null,
        pendingUntilEpochMillis = null,
        availableAgainAtEpochMillis = null,
        createdAtEpochMillis = NOW,
        updatedAtEpochMillis = NOW,
    )

    companion object {
        private const val NOW = 1_700_000_000_000L
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
