package com.sylvester.rustsensei.content

import android.content.Context
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sylvester.rustsensei.data.LearningPath
import com.sylvester.rustsensei.data.PathStep
import com.sylvester.rustsensei.data.Quiz
import com.sylvester.rustsensei.data.QuizIndexEntry
import com.sylvester.rustsensei.data.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

data class BookChapter(
    val id: String,
    val title: String,
    val sections: List<BookSection>
)

data class BookSection(
    val id: String,
    val title: String,
    val content: String,
    val codeExamples: List<CodeExample>,
    val keyTerms: List<String>,
    val relatedExercises: List<String>
)

data class CodeExample(
    val description: String,
    val code: String
)

data class ExerciseCategory(
    val id: String,
    val title: String,
    val description: String,
    val exercises: List<String>
)

data class ExerciseData(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: String,
    val description: String,
    val instructions: String,
    val starterCode: String,
    val hints: List<String>,
    val solution: String,
    val explanation: String,
    val relatedBookSection: String
)

// Reference content models
data class ReferenceIndex(
    val sections: List<ReferenceSectionInfo>
)

data class ReferenceSectionInfo(
    val id: String,
    val title: String,
    val description: String,
    val items: List<String>
)

data class BookIndex(
    val chapters: List<BookIndexEntry>
)

data class BookIndexEntry(
    val id: String,
    val title: String,
    val sectionIds: List<String>,
    val sectionTitles: List<String>
)

class ContentRepository(private val context: Context) {

    companion object {
        private const val TAG = "ContentRepository"
    }

    private val chapterCache = LruCache<String, BookChapter>(5)
    private val exerciseCache = LruCache<String, ExerciseData>(10)

    private var bookIndex: BookIndex? = null
    private var exerciseCategories: List<ExerciseCategory>? = null

    suspend fun getBookIndex(): BookIndex {
        bookIndex?.let { return it }

        return try {
            val json = loadAssetJson("book/index.json")
            val chapters = mutableListOf<BookIndexEntry>()

            val chaptersArray = json.getJSONArray("chapters")
            for (i in 0 until chaptersArray.length()) {
                val ch = chaptersArray.getJSONObject(i)
                val sectionIds = mutableListOf<String>()
                val sectionTitles = mutableListOf<String>()
                val sectionsArray = ch.getJSONArray("sections")
                for (j in 0 until sectionsArray.length()) {
                    val sec = sectionsArray.getJSONObject(j)
                    sectionIds.add(sec.getString("id"))
                    sectionTitles.add(sec.getString("title"))
                }
                chapters.add(BookIndexEntry(
                    id = ch.getString("id"),
                    title = ch.getString("title"),
                    sectionIds = sectionIds,
                    sectionTitles = sectionTitles
                ))
            }

            val index = BookIndex(chapters)
            bookIndex = index
            index
        } catch (e: Exception) {
            Log.e(TAG, "Error loading book index: ${e.message}", e)
            BookIndex(emptyList())
        }
    }

    suspend fun getChapter(chapterId: String): BookChapter? {
        chapterCache.get(chapterId)?.let { return it }

        return try {
            val json = loadAssetJson("book/chapters/$chapterId.json")
            val chapter = parseChapter(json)
            chapterCache.put(chapterId, chapter)
            chapter
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSection(chapterId: String, sectionId: String): BookSection? {
        val chapter = getChapter(chapterId) ?: return null
        return chapter.sections.find { it.id == sectionId }
    }

    suspend fun getExerciseCategories(): List<ExerciseCategory> {
        exerciseCategories?.let { return it }

        return try {
            val json = loadAssetJson("exercises/index.json")
            val categories = mutableListOf<ExerciseCategory>()

            val categoriesArray = json.getJSONArray("categories")
            for (i in 0 until categoriesArray.length()) {
                val cat = categoriesArray.getJSONObject(i)
                val exercises = mutableListOf<String>()
                val exercisesArray = cat.getJSONArray("exercises")
                for (j in 0 until exercisesArray.length()) {
                    exercises.add(exercisesArray.getString(j))
                }
                categories.add(ExerciseCategory(
                    id = cat.getString("id"),
                    title = cat.getString("title"),
                    description = cat.getString("description"),
                    exercises = exercises
                ))
            }

            exerciseCategories = categories
            categories
        } catch (e: Exception) {
            Log.e(TAG, "Error loading exercise categories: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getExercise(exerciseId: String): ExerciseData? {
        exerciseCache.get(exerciseId)?.let { return it }

        return try {
            val json = loadAssetJson("exercises/exercises/$exerciseId.json")
            val exercise = parseExercise(json)
            exerciseCache.put(exerciseId, exercise)
            exercise
        } catch (e: Exception) {
            null
        }
    }

    // Reference content
    private var referenceIndex: ReferenceIndex? = null

    suspend fun getReferenceIndex(): ReferenceIndex {
        referenceIndex?.let { return it }

        return try {
            val json = loadAssetJson("reference/index.json")
            val sections = mutableListOf<ReferenceSectionInfo>()

            val sectionsArray = json.getJSONArray("sections")
            for (i in 0 until sectionsArray.length()) {
                val sec = sectionsArray.getJSONObject(i)
                sections.add(ReferenceSectionInfo(
                    id = sec.getString("id"),
                    title = sec.getString("title"),
                    description = sec.getString("description"),
                    items = jsonArrayToStringList(sec.getJSONArray("items"))
                ))
            }

            val index = ReferenceIndex(sections)
            referenceIndex = index
            index
        } catch (e: Exception) {
            Log.e(TAG, "Error loading reference index: ${e.message}", e)
            ReferenceIndex(emptyList())
        }
    }

    suspend fun getReferenceItem(sectionId: String, itemId: String): JSONObject? {
        return try {
            loadAssetJson("reference/$sectionId/$itemId.json")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTotalSectionsCount(): Int {
        return try {
            getBookIndex().chapters.sumOf { it.sectionIds.size }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total sections count: ${e.message}", e)
            0
        }
    }

    suspend fun getTotalExercisesCount(): Int {
        return try {
            getExerciseCategories().sumOf { it.exercises.size }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total exercises count: ${e.message}", e)
            0
        }
    }

    // Learning Paths
    private var learningPaths: List<LearningPath>? = null

    suspend fun getLearningPaths(): List<LearningPath> {
        learningPaths?.let { return it }

        return try {
            val json = loadAssetJson("paths/index.json")
            val paths = mutableListOf<LearningPath>()

            val pathsArray = json.getJSONArray("paths")
            for (i in 0 until pathsArray.length()) {
                val pathObj = pathsArray.getJSONObject(i)
                val steps = mutableListOf<PathStep>()
                val stepsArray = pathObj.getJSONArray("steps")
                for (j in 0 until stepsArray.length()) {
                    val stepObj = stepsArray.getJSONObject(j)
                    steps.add(PathStep(
                        id = stepObj.getString("id"),
                        type = stepObj.getString("type"),
                        targetId = stepObj.getString("targetId"),
                        title = stepObj.getString("title"),
                        description = stepObj.getString("description")
                    ))
                }
                paths.add(LearningPath(
                    id = pathObj.getString("id"),
                    title = pathObj.getString("title"),
                    description = pathObj.getString("description"),
                    estimatedDays = pathObj.getInt("estimatedDays"),
                    steps = steps
                ))
            }

            learningPaths = paths
            paths
        } catch (e: Exception) {
            Log.e(TAG, "Error loading learning paths: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getLearningPath(pathId: String): LearningPath? {
        return getLearningPaths().find { it.id == pathId }
    }

    // Quiz content
    private var quizIndex: List<QuizIndexEntry>? = null

    suspend fun getQuizIndex(): List<QuizIndexEntry> {
        quizIndex?.let { return it }

        return try {
            val json = loadAssetJson("quizzes/index.json")
            val entries = mutableListOf<QuizIndexEntry>()

            val quizzesArray = json.getJSONArray("quizzes")
            for (i in 0 until quizzesArray.length()) {
                val q = quizzesArray.getJSONObject(i)
                entries.add(QuizIndexEntry(
                    id = q.getString("id"),
                    title = q.getString("title"),
                    chapterId = q.getString("chapterId"),
                    questionCount = q.getInt("questionCount")
                ))
            }

            quizIndex = entries
            entries
        } catch (e: Exception) {
            Log.e(TAG, "Error loading quiz index: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getQuiz(quizId: String): Quiz? {
        return try {
            val json = loadAssetJson("quizzes/$quizId.json")
            parseQuiz(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading quiz $quizId: ${e.message}", e)
            null
        }
    }

    private fun parseQuiz(json: JSONObject): Quiz {
        val questions = mutableListOf<QuizQuestion>()
        val questionsArray = json.getJSONArray("questions")

        for (i in 0 until questionsArray.length()) {
            val q = questionsArray.getJSONObject(i)
            val type = q.getString("type")
            val id = q.getString("id")
            val question = q.getString("question")
            val explanation = q.getString("explanation")

            when (type) {
                "multiple_choice" -> {
                    questions.add(QuizQuestion.MultipleChoice(
                        id = id,
                        question = question,
                        options = jsonArrayToStringList(q.getJSONArray("options")),
                        correctIndex = q.getInt("correctIndex"),
                        explanation = explanation
                    ))
                }
                "true_false" -> {
                    questions.add(QuizQuestion.TrueFalse(
                        id = id,
                        question = question,
                        correctAnswer = q.getBoolean("correctAnswer"),
                        explanation = explanation
                    ))
                }
                "code_completion" -> {
                    questions.add(QuizQuestion.CodeCompletion(
                        id = id,
                        question = question,
                        code = q.getString("code"),
                        correctAnswer = q.getString("correctAnswer"),
                        acceptableAnswers = jsonArrayToStringList(q.getJSONArray("acceptableAnswers")),
                        explanation = explanation
                    ))
                }
            }
        }

        return Quiz(
            id = json.getString("id"),
            title = json.getString("title"),
            questions = questions
        )
    }

    private fun parseChapter(json: JSONObject): BookChapter {
        val sections = mutableListOf<BookSection>()
        val sectionsArray = json.getJSONArray("sections")

        for (i in 0 until sectionsArray.length()) {
            val sec = sectionsArray.getJSONObject(i)

            val codeExamples = mutableListOf<CodeExample>()
            if (sec.has("codeExamples")) {
                val examplesArray = sec.getJSONArray("codeExamples")
                for (j in 0 until examplesArray.length()) {
                    val ex = examplesArray.getJSONObject(j)
                    codeExamples.add(CodeExample(
                        description = ex.getString("description"),
                        code = ex.getString("code")
                    ))
                }
            }

            val keyTerms = jsonArrayToStringList(sec.optJSONArray("keyTerms"))
            val relatedExercises = jsonArrayToStringList(sec.optJSONArray("relatedExercises"))

            sections.add(BookSection(
                id = sec.getString("id"),
                title = sec.getString("title"),
                content = sec.getString("content"),
                codeExamples = codeExamples,
                keyTerms = keyTerms,
                relatedExercises = relatedExercises
            ))
        }

        return BookChapter(
            id = json.getString("id"),
            title = json.getString("title"),
            sections = sections
        )
    }

    private fun parseExercise(json: JSONObject): ExerciseData {
        return ExerciseData(
            id = json.getString("id"),
            title = json.getString("title"),
            category = json.getString("category"),
            difficulty = json.getString("difficulty"),
            description = json.getString("description"),
            instructions = json.getString("instructions"),
            starterCode = json.getString("starterCode"),
            hints = jsonArrayToStringList(json.getJSONArray("hints")),
            solution = json.getString("solution"),
            explanation = json.getString("explanation"),
            relatedBookSection = json.optString("relatedBookSection", "")
        )
    }

    private fun jsonArrayToStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private suspend fun loadAssetJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val inputStream = context.assets.open(path)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        JSONObject(jsonString)
    }
}
