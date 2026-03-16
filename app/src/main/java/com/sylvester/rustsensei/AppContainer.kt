package com.sylvester.rustsensei

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sylvester.rustsensei.content.ContentRepository
import com.sylvester.rustsensei.content.RagRetriever
import com.sylvester.rustsensei.data.AppDatabase
import com.sylvester.rustsensei.data.ChatRepository
import com.sylvester.rustsensei.data.PreferencesManager
import com.sylvester.rustsensei.data.ProgressRepository
import com.sylvester.rustsensei.llm.LiteRtEngine
import com.sylvester.rustsensei.llm.ModelManager
import com.sylvester.rustsensei.viewmodel.BookViewModel
import com.sylvester.rustsensei.viewmodel.ChatViewModel
import com.sylvester.rustsensei.viewmodel.ExerciseViewModel
import com.sylvester.rustsensei.viewmodel.LearningPathViewModel
import com.sylvester.rustsensei.viewmodel.ModelViewModel
import com.sylvester.rustsensei.viewmodel.ProgressViewModel
import com.sylvester.rustsensei.viewmodel.QuizViewModel
import com.sylvester.rustsensei.viewmodel.ReferenceViewModel
import com.sylvester.rustsensei.viewmodel.ReviewViewModel
import com.sylvester.rustsensei.viewmodel.SearchViewModel

class AppContainer(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    val chatDao = database.chatDao()
    val progressDao = database.progressDao()
    val flashCardDao = database.flashCardDao()

    val chatRepository = ChatRepository(chatDao)
    val progressRepository = ProgressRepository(progressDao)
    val contentRepository = ContentRepository(context)
    val ragRetriever = RagRetriever(context)
    val preferencesManager = PreferencesManager(context)

    val liteRtEngine = LiteRtEngine(context)
    val modelManager = ModelManager(context)
}

class AppViewModelFactory(
    private val app: Application,
    private val container: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(ChatViewModel::class.java) ->
            ChatViewModel(
                container.chatRepository,
                container.liteRtEngine,
                container.ragRetriever,
                container.preferencesManager
            )
        modelClass.isAssignableFrom(ModelViewModel::class.java) ->
            ModelViewModel(app, container.modelManager, container.preferencesManager, container.liteRtEngine)
        modelClass.isAssignableFrom(BookViewModel::class.java) ->
            BookViewModel(container.contentRepository, container.progressRepository)
        modelClass.isAssignableFrom(ExerciseViewModel::class.java) ->
            ExerciseViewModel(
                container.contentRepository,
                container.progressRepository,
                container.liteRtEngine,
                container.preferencesManager
            )
        modelClass.isAssignableFrom(ProgressViewModel::class.java) ->
            ProgressViewModel(container.progressRepository, container.contentRepository)
        modelClass.isAssignableFrom(ReferenceViewModel::class.java) ->
            ReferenceViewModel(container.contentRepository)
        modelClass.isAssignableFrom(ReviewViewModel::class.java) ->
            ReviewViewModel(container.flashCardDao, container.contentRepository)
        modelClass.isAssignableFrom(LearningPathViewModel::class.java) ->
            LearningPathViewModel(container.contentRepository, container.progressDao)
        modelClass.isAssignableFrom(QuizViewModel::class.java) ->
            QuizViewModel(container.contentRepository, container.progressRepository)
        modelClass.isAssignableFrom(SearchViewModel::class.java) ->
            SearchViewModel(
                container.contentRepository,
                container.progressRepository,
                container.preferencesManager
            )
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
