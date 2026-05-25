package com.polaralias.coupio

import android.app.Application
import com.polaralias.coupio.data.AppContainer

class CouponApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
