package com.polaralias.coupio.data

import com.polaralias.coupio.data.local.CouponEntity
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CouponLifecycleManagerTest {
    private val lifecycleManager = CouponLifecycleManager(zoneId = ZoneId.of("UTC"))

    @Test
    fun `single use locks after confirmation until admin reissues it`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.SINGLE_USE), NOW)

        val confirmed = lifecycleManager.confirmPending(shared, NOW + 1_000)

        assertEquals(CouponState.LOCKED, confirmed.state)
        assertNull(confirmed.availableAgainAtEpochMillis)
        assertNull(confirmed.pendingUntilEpochMillis)
    }

    @Test
    fun `single use can be manually reissued after confirmation`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.SINGLE_USE), NOW)
        val confirmed = lifecycleManager.confirmPending(shared, NOW + 1_000)

        val reissued = lifecycleManager.reissue(confirmed, NOW + 2_000)

        assertEquals(CouponState.AVAILABLE, reissued.state)
        assertNull(reissued.pendingUntilEpochMillis)
        assertNull(reissued.availableAgainAtEpochMillis)
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

    @Test
    fun `daily cooldown starts when pending coupon is confirmed`() {
        val sharedAt = NOW
        val confirmedAt = NOW + HOUR_MILLIS
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.DAILY), sharedAt)

        val confirmed = lifecycleManager.confirmPending(shared, confirmedAt)

        assertEquals(confirmedAt + DAY_MILLIS, confirmed.availableAgainAtEpochMillis)
    }

    @Test
    fun `weekly policy unlocks after cooldown`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.WEEKLY), NOW)
        val confirmed = lifecycleManager.confirmPending(shared, NOW + 1_000)

        val unlocked = lifecycleManager.reconcile(confirmed, NOW + WEEK_MILLIS + 5_000)

        requireNotNull(unlocked)
        assertEquals(CouponState.AVAILABLE, unlocked.state)
        assertNull(unlocked.availableAgainAtEpochMillis)
    }

    @Test
    fun `monthly policy unlocks after cooldown`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.MONTHLY), NOW)
        val confirmed = lifecycleManager.confirmPending(shared, NOW + 1_000)

        val unlocked = lifecycleManager.reconcile(confirmed, NOW + THIRTY_ONE_DAY_MILLIS)

        requireNotNull(unlocked)
        assertEquals(CouponState.AVAILABLE, unlocked.state)
        assertNull(unlocked.availableAgainAtEpochMillis)
    }

    @Test
    fun `revert pending returns coupon to available and clears timing fields`() {
        val shared = lifecycleManager.markShared(baseCoupon(reusePolicy = CouponReusePolicy.DAILY), NOW)

        val reverted = lifecycleManager.revertPending(shared, NOW + 1_000)

        assertEquals(CouponState.AVAILABLE, reverted.state)
        assertNull(reverted.shareRequestedAtEpochMillis)
        assertNull(reverted.pendingUntilEpochMillis)
        assertNull(reverted.availableAgainAtEpochMillis)
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
        private const val HOUR_MILLIS = 60 * 60 * 1000L
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
        private const val WEEK_MILLIS = 7 * DAY_MILLIS
        private const val THIRTY_ONE_DAY_MILLIS = 31 * DAY_MILLIS
    }
}
