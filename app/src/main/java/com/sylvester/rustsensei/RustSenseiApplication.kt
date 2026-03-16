package com.sylvester.rustsensei

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RustSenseiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
    }
}
