package com.polaralias.coupio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [CouponEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(CouponConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun couponDao(): CouponDao
}
