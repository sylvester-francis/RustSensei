package com.sylvester.rustsensei

import android.util.Log

class CrashHandler : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e("RustSensei", "Uncaught exception on thread ${t.name}", e)
        // Let the default handler show the crash dialog
        defaultHandler?.uncaughtException(t, e)
    }
}
