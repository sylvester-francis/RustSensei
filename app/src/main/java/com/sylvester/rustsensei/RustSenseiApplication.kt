package com.sylvester.rustsensei

import android.app.Application

class RustSenseiApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
    }
}
