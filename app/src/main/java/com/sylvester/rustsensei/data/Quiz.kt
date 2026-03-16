package com.sylvester.rustsensei.data

data class Quiz(
    val id: String,
    val title: String,
    val questions: List<QuizQuestion>
)

sealed class QuizQuestion {
    abstract val id: String
    abstract val question: String
    abstract val explanation: String

    data class MultipleChoice(
        override val id: String,
        override val question: String,
        val options: List<String>,
        val correctIndex: Int,
        override val explanation: String
    ) : QuizQuestion()

    data class TrueFalse(
        override val id: String,
        override val question: String,
        val correctAnswer: Boolean,
        override val explanation: String
    ) : QuizQuestion()

    data class CodeCompletion(
        override val id: String,
        override val question: String,
        val code: String,
        val correctAnswer: String,
        val acceptableAnswers: List<String>,
        override val explanation: String
    ) : QuizQuestion()
}

data class QuizIndexEntry(
    val id: String,
    val title: String,
    val chapterId: String,
    val questionCount: Int
)
