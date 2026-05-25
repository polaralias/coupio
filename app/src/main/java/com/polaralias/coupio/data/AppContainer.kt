package com.polaralias.coupio.data

import android.content.Context
import androidx.room.Room
import com.polaralias.coupio.data.local.AppDatabase

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "coupon-glass.db",
    ).build()
    private val lifecycleManager = CouponLifecycleManager()
    private val scheduler = WorkManagerCouponScheduler(appContext)
    private val mediaStorage = AndroidCouponMediaStorage(appContext)

    val couponRepository = CouponRepository(
        couponDao = database.couponDao(),
        lifecycleManager = lifecycleManager,
        scheduler = scheduler,
        mediaStorage = mediaStorage,
    )
    val adminPinRepository = AdminPinRepository(appContext)
    val appPreferencesRepository = AppPreferencesRepository(appContext)
}
