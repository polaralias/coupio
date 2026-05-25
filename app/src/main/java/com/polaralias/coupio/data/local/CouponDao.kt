package com.polaralias.coupio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CouponDao {
    @Query("SELECT * FROM coupons ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE id = :couponId LIMIT 1")
    suspend fun getById(couponId: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(coupon: CouponEntity)

    @Update
    suspend fun update(coupon: CouponEntity)

    @Query(
        """
        SELECT * FROM coupons
        WHERE state = 'PENDING'
        AND pendingUntilEpochMillis IS NOT NULL
        AND pendingUntilEpochMillis <= :nowEpochMillis
        """,
    )
    suspend fun getPendingDue(nowEpochMillis: Long): List<CouponEntity>

    @Query(
        """
        SELECT * FROM coupons
        WHERE state = 'LOCKED'
        AND availableAgainAtEpochMillis IS NOT NULL
        AND availableAgainAtEpochMillis <= :nowEpochMillis
        """,
    )
    suspend fun getUnlocksDue(nowEpochMillis: Long): List<CouponEntity>
}
