package com.sylvester.rustsensei

import android.app.Application
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.content.RagRetriever
import com.sylvester.rustsensei.data.AppDatabase
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ProgressRepository
import com.sylvester.rustsensei.llm.LlamaEngine
import com.sylvester.rustsensei.llm.LiteRtEngine

class RustSenseiApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: ChatRepository by lazy { ChatRepository(database.chatDao()) }
    val progressRepository: ProgressRepository by lazy { ProgressRepository(database.progressDao()) }
    val contentRepository: ContentRepository by lazy { ContentRepository(this) }
    val ragRetriever: RagRetriever by lazy { RagRetriever(this) }
    val preferencesManager: PreferencesManager by lazy { PreferencesManager(this) }
    val llamaEngine: LlamaEngine by lazy { LlamaEngine() }
    val liteRtEngine: LiteRtEngine by lazy { LiteRtEngine(this) }
}
