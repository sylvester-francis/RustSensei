package com.sylvester.rustsensei

import android.app.Application
import com.sylvester.rustsensei.data.AppDatabase
import com.sylvester.rustsensei.data.ChatRepository

class RustSenseiApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: ChatRepository by lazy { ChatRepository(database.chatDao()) }
}
