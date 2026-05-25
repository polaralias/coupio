package com.polaralias.coupio.data

import com.polaralias.coupio.data.local.CouponDao
import com.polaralias.coupio.data.local.CouponEntity
import com.polaralias.coupio.data.model.CouponDraftInput
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import com.polaralias.coupio.data.model.DEFAULT_PENDING_WINDOW_MILLIS
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CouponRepositoryTest {
    @Test
    fun `importCoupon persists stored media and trims optional metadata`() = runBlocking {
        val dao = FakeCouponDao()
        val repository = repositoryFor(
            dao = dao,
            mediaStorage = FakeCouponMediaStorage(File("C:\\coupons\\stored.pdf")),
        )

        repository.importCoupon(
            CouponDraftInput(
                tempFilePath = "C:\\temp\\incoming.pdf",
                mediaMimeType = "application/pdf",
                mediaType = CouponMediaType.PDF,
                mediaDisplayName = "incoming.pdf",
                title = "  Friday Treat  ",
                description = "   ",
                category = "  Drinks ",
                expiryDate = LocalDate.of(2026, 5, 31),
                reusePolicy = CouponReusePolicy.MONTHLY,
            ),
        )

        val inserted = dao.singleCoupon()

        assertEquals("C:\\coupons\\stored.pdf", inserted.mediaPath)
        assertEquals(CouponMediaType.PDF, inserted.mediaType)
        assertEquals("application/pdf", inserted.mediaMimeType)
        assertEquals("incoming.pdf", inserted.mediaDisplayName)
        assertEquals("Friday Treat", inserted.title)
        assertNull(inserted.description)
        assertEquals("Drinks", inserted.category)
        assertEquals(LocalDate.of(2026, 5, 31).toEpochDay(), inserted.expiryEpochDay)
        assertEquals(CouponReusePolicy.MONTHLY, inserted.reusePolicy)
        assertEquals(CouponState.AVAILABLE, inserted.state)
    }

    @Test
    fun `updateCouponMetadata trims values and preserves stored media details`() = runBlocking {
        val original = baseCoupon(
            id = "coupon-1",
            title = "Old title",
        ).copy(
            description = "Old description",
            category = "Old category",
            expiryEpochDay = LocalDate.of(2026, 5, 20).toEpochDay(),
            reusePolicy = CouponReusePolicy.DAILY,
            updatedAtEpochMillis = NOW - 20_000,
        )
        val dao = FakeCouponDao(original)
        val repository = repositoryFor(dao = dao)

        repository.updateCouponMetadata(
            com.polaralias.coupio.data.model.CouponMetadataUpdate(
                couponId = "coupon-1",
                title = "  New title  ",
                description = "",
                category = "  New category ",
                expiryDate = LocalDate.of(2026, 6, 15),
                reusePolicy = CouponReusePolicy.WEEKLY,
            ),
        )

        val updated = dao.requireCoupon("coupon-1")

        assertEquals("New title", updated.title)
        assertNull(updated.description)
        assertEquals("New category", updated.category)
        assertEquals(LocalDate.of(2026, 6, 15).toEpochDay(), updated.expiryEpochDay)
        assertEquals(CouponReusePolicy.WEEKLY, updated.reusePolicy)
        assertEquals("C:\\coupons\\coupon.jpg", updated.mediaPath)
        assertEquals("coupon.jpg", updated.mediaDisplayName)
        assertEquals(NOW, updated.updatedAtEpochMillis)
    }

    @Test
    fun `prepareShare marks available coupon pending and schedules finalization`() = runBlocking {
        val dao = FakeCouponDao(
            baseCoupon(
                id = "coupon-1",
                state = CouponState.AVAILABLE,
                title = "Free Coffee",
            ),
        )
        val scheduler = FakeCouponScheduler()
        val repository = repositoryFor(
            dao = dao,
            scheduler = scheduler,
        )

        val payload = repository.prepareShare("coupon-1")
        val updated = dao.requireCoupon("coupon-1")

        assertEquals("Free Coffee", payload?.subject)
        assertEquals("C:\\coupons\\coupon.jpg", payload?.mediaPath)
        assertEquals("image/jpeg", payload?.mimeType)
        assertEquals(CouponState.PENDING, updated.state)
        assertEquals(NOW, updated.shareRequestedAtEpochMillis)
        assertEquals(NOW + DEFAULT_PENDING_WINDOW_MILLIS, updated.pendingUntilEpochMillis)
        assertEquals(listOf("coupon-1" to NOW + DEFAULT_PENDING_WINDOW_MILLIS), scheduler.scheduled)
    }

    @Test
    fun `prepareShare returns null for expired coupon without scheduling`() = runBlocking {
        val dao = FakeCouponDao(
            baseCoupon(
                id = "coupon-1",
                state = CouponState.AVAILABLE,
                expiryEpochDay = LocalDate.ofEpochDay(0).toEpochDay(),
            ),
        )
        val scheduler = FakeCouponScheduler()
        val repository = repositoryFor(dao = dao, scheduler = scheduler)

        val payload = repository.prepareShare("coupon-1")
        val current = dao.requireCoupon("coupon-1")

        assertNull(payload)
        assertEquals(CouponState.AVAILABLE, current.state)
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `prepareShare reissues due locked coupon before marking it pending`() = runBlocking {
        val dao = FakeCouponDao(
            baseCoupon(
                id = "coupon-1",
                state = CouponState.LOCKED,
                availableAgainAtEpochMillis = NOW - 1_000,
            ),
        )
        val scheduler = FakeCouponScheduler()
        val repository = repositoryFor(dao = dao, scheduler = scheduler)

        val payload = repository.prepareShare("coupon-1")
        val updated = dao.requireCoupon("coupon-1")

        assertEquals("Test", payload?.subject)
        assertEquals(CouponState.PENDING, updated.state)
        assertEquals(NOW, updated.shareRequestedAtEpochMillis)
        assertEquals(listOf("coupon-1" to NOW + DEFAULT_PENDING_WINDOW_MILLIS), scheduler.scheduled)
    }

    @Test
    fun `confirmPending finalizes coupon and cancels scheduled work`() = runBlocking {
        val dao = FakeCouponDao(
            baseCoupon(
                id = "coupon-1",
                state = CouponState.PENDING,
                reusePolicy = CouponReusePolicy.DAILY,
                shareRequestedAtEpochMillis = NOW - 5_000,
                pendingUntilEpochMillis = NOW + 10_000,
            ),
        )
        val scheduler = FakeCouponScheduler()
        val repository = repositoryFor(dao = dao, scheduler = scheduler)

        repository.confirmPending("coupon-1")
        val updated = dao.requireCoupon("coupon-1")

        assertEquals(CouponState.LOCKED, updated.state)
        assertEquals(listOf("coupon-1"), scheduler.cancelled)
    }

    @Test
    fun `revertPending returns pending coupon to available and cancels scheduled work`() = runBlocking {
        val dao = FakeCouponDao(
            baseCoupon(
                id = "coupon-1",
                state = CouponState.PENDING,
                shareRequestedAtEpochMillis = NOW - 5_000,
                pendingUntilEpochMillis = NOW + 10_000,
            ),
        )
        val scheduler = FakeCouponScheduler()
        val repository = repositoryFor(dao = dao, scheduler = scheduler)

        repository.revertPending("coupon-1")
        val updated = dao.requireCoupon("coupon-1")

        assertEquals(CouponState.AVAILABLE, updated.state)
        assertNull(updated.shareRequestedAtEpochMillis)
        assertNull(updated.pendingUntilEpochMillis)
        assertEquals(listOf("coupon-1"), scheduler.cancelled)
    }

    @Test
    fun `reissueCoupon returns locked coupon to available and cancels scheduled work`() = runBlocking {
        val dao = FakeCouponDao(
            baseCoupon(
                id = "coupon-1",
                state = CouponState.LOCKED,
                availableAgainAtEpochMillis = NOW + 10_000,
            ),
        )
        val scheduler = FakeCouponScheduler()
        val repository = repositoryFor(dao = dao, scheduler = scheduler)

        repository.reissueCoupon("coupon-1")
        val updated = dao.requireCoupon("coupon-1")

        assertEquals(CouponState.AVAILABLE, updated.state)
        assertNull(updated.availableAgainAtEpochMillis)
        assertEquals(listOf("coupon-1"), scheduler.cancelled)
    }

    @Test
    fun `reconcileDueCoupons confirms due pending coupons and reissues due locked coupons`() = runBlocking {
        val pendingCoupon = baseCoupon(
            id = "pending",
            state = CouponState.PENDING,
            reusePolicy = CouponReusePolicy.SINGLE_USE,
            shareRequestedAtEpochMillis = NOW - 20_000,
            pendingUntilEpochMillis = NOW - 1_000,
        )
        val lockedCoupon = baseCoupon(
            id = "locked",
            state = CouponState.LOCKED,
            availableAgainAtEpochMillis = NOW - 1_000,
        )
        val dao = FakeCouponDao(pendingCoupon, lockedCoupon)
        val repository = repositoryFor(dao = dao)

        repository.reconcileDueCoupons(NOW)

        assertEquals(CouponState.LOCKED, dao.requireCoupon("pending").state)
        assertEquals(CouponState.AVAILABLE, dao.requireCoupon("locked").state)
    }

    private fun repositoryFor(
        dao: FakeCouponDao,
        scheduler: FakeCouponScheduler = FakeCouponScheduler(),
        mediaStorage: CouponMediaStorage = FakeCouponMediaStorage(),
    ): CouponRepository = CouponRepository(
        couponDao = dao,
        lifecycleManager = CouponLifecycleManager(),
        scheduler = scheduler,
        mediaStorage = mediaStorage,
        nowEpochMillis = { NOW },
    )

    private fun baseCoupon(
        id: String = "coupon-id",
        title: String? = "Test",
        state: CouponState = CouponState.AVAILABLE,
        reusePolicy: CouponReusePolicy = CouponReusePolicy.SINGLE_USE,
        expiryEpochDay: Long? = null,
        shareRequestedAtEpochMillis: Long? = null,
        pendingUntilEpochMillis: Long? = null,
        availableAgainAtEpochMillis: Long? = null,
    ) = CouponEntity(
        id = id,
        mediaPath = "C:\\coupons\\coupon.jpg",
        mediaType = CouponMediaType.IMAGE,
        mediaMimeType = "image/jpeg",
        mediaDisplayName = "coupon.jpg",
        title = title,
        description = null,
        category = null,
        expiryEpochDay = expiryEpochDay,
        reusePolicy = reusePolicy,
        state = state,
        shareRequestedAtEpochMillis = shareRequestedAtEpochMillis,
        pendingUntilEpochMillis = pendingUntilEpochMillis,
        availableAgainAtEpochMillis = availableAgainAtEpochMillis,
        createdAtEpochMillis = NOW - 10_000,
        updatedAtEpochMillis = NOW - 10_000,
    )

    private class FakeCouponDao(vararg coupons: CouponEntity) : CouponDao {
        private val couponsById = coupons.associateBy { it.id }.toMutableMap()

        override fun observeAll(): Flow<List<CouponEntity>> = flowOf(couponsById.values.toList())

        override suspend fun getById(couponId: String): CouponEntity? = couponsById[couponId]

        override suspend fun insert(coupon: CouponEntity) {
            couponsById[coupon.id] = coupon
        }

        override suspend fun update(coupon: CouponEntity) {
            couponsById[coupon.id] = coupon
        }

        override suspend fun getPendingDue(nowEpochMillis: Long): List<CouponEntity> = couponsById.values.filter {
            it.state == CouponState.PENDING &&
                it.pendingUntilEpochMillis != null &&
                it.pendingUntilEpochMillis <= nowEpochMillis
        }

        override suspend fun getUnlocksDue(nowEpochMillis: Long): List<CouponEntity> = couponsById.values.filter {
            it.state == CouponState.LOCKED &&
                it.availableAgainAtEpochMillis != null &&
                it.availableAgainAtEpochMillis <= nowEpochMillis
        }

        fun requireCoupon(couponId: String): CouponEntity = checkNotNull(couponsById[couponId])

        fun singleCoupon(): CouponEntity = couponsById.values.single()
    }

    private class FakeCouponScheduler : CouponScheduler {
        val scheduled = mutableListOf<Pair<String, Long>>()
        val cancelled = mutableListOf<String>()

        override fun scheduleFinalize(couponId: String, pendingUntilEpochMillis: Long) {
            scheduled += couponId to pendingUntilEpochMillis
        }

        override fun cancelFinalize(couponId: String) {
            cancelled += couponId
        }
    }

    private class FakeCouponMediaStorage(
        private val storedFile: File = File("C:\\coupons\\stored.jpg"),
    ) : CouponMediaStorage {
        override suspend fun persist(input: CouponDraftInput): File = storedFile
    }

    companion object {
        private const val NOW = 1_700_000_000_000L
    }
}
