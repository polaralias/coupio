package com.polaralias.coupio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.polaralias.coupio.data.model.CouponMediaType
import com.polaralias.coupio.data.model.CouponReusePolicy
import com.polaralias.coupio.data.model.CouponState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val id: String,
    val mediaPath: String,
    val mediaType: CouponMediaType,
    val mediaMimeType: String,
    val mediaDisplayName: String,
    val title: String?,
    val description: String?,
    val category: String?,
    val expiryEpochDay: Long?,
    val reusePolicy: CouponReusePolicy,
    val state: CouponState,
    val shareRequestedAtEpochMillis: Long?,
    val pendingUntilEpochMillis: Long?,
    val availableAgainAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

class CouponConverters {
    @TypeConverter
    fun fromMediaType(value: CouponMediaType): String = value.name

    @TypeConverter
    fun toMediaType(value: String): CouponMediaType = CouponMediaType.valueOf(value)

    @TypeConverter
    fun fromReusePolicy(value: CouponReusePolicy): String = value.name

    @TypeConverter
    fun toReusePolicy(value: String): CouponReusePolicy = CouponReusePolicy.valueOf(value)

    @TypeConverter
    fun fromState(value: CouponState): String = value.name

    @TypeConverter
    fun toState(value: String): CouponState = CouponState.valueOf(value)
}

fun CouponEntity.displayTitle(): String = title?.takeIf { it.isNotBlank() }
    ?: mediaDisplayName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ')

fun CouponEntity.expiryDate(): LocalDate? = expiryEpochDay?.let(LocalDate::ofEpochDay)

fun CouponEntity.isExpired(nowEpochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
    val expiryDate = expiryDate() ?: return false
    val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
    return today.isAfter(expiryDate)
}
