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
    val relatedBookSection: String,
    val tests: String = ""
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

interface ContentProvider {
    suspend fun getBookIndex(): BookIndex
    suspend fun getChapter(chapterId: String): BookChapter?
    suspend fun getSection(chapterId: String, sectionId: String): BookSection?
    suspend fun getExerciseCategories(): List<ExerciseCategory>
    suspend fun getExercise(exerciseId: String): ExerciseData?
    suspend fun getReferenceIndex(): ReferenceIndex
    suspend fun getReferenceItem(sectionId: String, itemId: String): org.json.JSONObject?
    suspend fun getTotalSectionsCount(): Int
    suspend fun getTotalExercisesCount(): Int
    suspend fun getLearningPaths(): List<LearningPath>
    suspend fun getLearningPath(pathId: String): LearningPath?
    suspend fun getQuizIndex(): List<com.sylvester.rustsensei.data.QuizIndexEntry>
    suspend fun getQuiz(quizId: String): com.sylvester.rustsensei.data.Quiz?
    suspend fun getRefactoringChallenges(): List<RefactoringChallenge>
    suspend fun getRefactoringChallenge(id: String): RefactoringChallenge?
    suspend fun getDocIndex(): List<DocIndexEntry>
    suspend fun getDoc(typeId: String): DocEntry?
    suspend fun loadVisualizationJson(filename: String): org.json.JSONObject?
    suspend fun loadProjectJson(filename: String): org.json.JSONObject?
}

class ContentRepository(private val context: Context) : ContentProvider {

    companion object {
        private const val TAG = "ContentRepository"
    }

    // Optimization #10: Increase cache sizes — 19 chapters total, so cache them all
    // to avoid re-parsing JSON when users flip between chapters.
    private val chapterCache = LruCache<String, BookChapter>(19)
    private val exerciseCache = LruCache<String, ExerciseData>(30)

    private var bookIndex: BookIndex? = null
    private var exerciseCategories: List<ExerciseCategory>? = null

    override suspend fun getBookIndex(): BookIndex {
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

    override suspend fun getChapter(chapterId: String): BookChapter? {
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

    override suspend fun getSection(chapterId: String, sectionId: String): BookSection? {
        val chapter = getChapter(chapterId) ?: return null
        return chapter.sections.find { it.id == sectionId }
    }

    override suspend fun getExerciseCategories(): List<ExerciseCategory> {
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

    override suspend fun getExercise(exerciseId: String): ExerciseData? {
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

    override suspend fun getReferenceIndex(): ReferenceIndex {
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

    override suspend fun getReferenceItem(sectionId: String, itemId: String): JSONObject? {
        return try {
            loadAssetJson("reference/$sectionId/$itemId.json")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getTotalSectionsCount(): Int {
        return try {
            getBookIndex().chapters.sumOf { it.sectionIds.size }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total sections count: ${e.message}", e)
            0
        }
    }

    override suspend fun getTotalExercisesCount(): Int {
        return try {
            getExerciseCategories().sumOf { it.exercises.size }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting total exercises count: ${e.message}", e)
            0
        }
    }

    // Learning Paths
    private var learningPaths: List<LearningPath>? = null

    override suspend fun getLearningPaths(): List<LearningPath> {
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

    override suspend fun getLearningPath(pathId: String): LearningPath? {
        return getLearningPaths().find { it.id == pathId }
    }

    // Quiz content
    private var quizIndex: List<QuizIndexEntry>? = null

    override suspend fun getQuizIndex(): List<QuizIndexEntry> {
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

    override suspend fun getQuiz(quizId: String): Quiz? {
        return try {
            val json = loadAssetJson("quizzes/$quizId.json")
            parseQuiz(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading quiz $quizId: ${e.message}", e)
            null
        }
    }

    // Refactoring Challenges
    override suspend fun getRefactoringChallenges(): List<RefactoringChallenge> {
        return try {
            val json = loadAssetJson("refactoring/index.json")
            val arr = json.getJSONArray("challenges")
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                getRefactoringChallenge(obj.getString("id"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading refactoring challenges: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getRefactoringChallenge(id: String): RefactoringChallenge? {
        return try {
            val json = loadAssetJson("refactoring/$id.json")
            RefactoringChallenge(
                id = json.getString("id"),
                title = json.getString("title"),
                difficulty = json.getString("difficulty"),
                description = json.getString("description"),
                uglyCode = json.getString("uglyCode"),
                hints = (0 until json.getJSONArray("hints").length()).map { json.getJSONArray("hints").getString(it) },
                idiomaticSolution = json.getString("idiomaticSolution"),
                scoringCriteria = json.getString("scoringCriteria")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading refactoring challenge $id: ${e.message}", e)
            null
        }
    }

    // Docs Browser
    private var docIndex: List<DocIndexEntry>? = null

    override suspend fun getDocIndex(): List<DocIndexEntry> {
        docIndex?.let { return it }

        return try {
            val json = loadAssetJson("docs/index.json")
            val entries = mutableListOf<DocIndexEntry>()

            val typesArray = json.getJSONArray("types")
            for (i in 0 until typesArray.length()) {
                val t = typesArray.getJSONObject(i)
                entries.add(DocIndexEntry(
                    id = t.getString("id"),
                    name = t.getString("name"),
                    module = t.getString("module")
                ))
            }

            docIndex = entries
            entries
        } catch (e: Exception) {
            Log.e(TAG, "Error loading doc index: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getDoc(typeId: String): DocEntry? {
        return try {
            val json = loadAssetJson("docs/$typeId.json")
            val methods = mutableListOf<DocMethod>()
            val methodsArray = json.getJSONArray("methods")
            for (i in 0 until methodsArray.length()) {
                val m = methodsArray.getJSONObject(i)
                methods.add(DocMethod(
                    name = m.getString("name"),
                    signature = m.getString("signature"),
                    description = m.getString("description"),
                    example = m.getString("example")
                ))
            }
            DocEntry(
                id = json.getString("id"),
                typeName = json.getString("typeName"),
                module = json.getString("module"),
                signature = json.getString("signature"),
                description = json.getString("description"),
                methods = methods
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading doc $typeId: ${e.message}", e)
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
            relatedBookSection = json.optString("relatedBookSection", ""),
            tests = json.optString("tests", "")
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

    override suspend fun loadVisualizationJson(filename: String): JSONObject? {
        return try {
            loadAssetJson("visualizations/$filename.json")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading visualization $filename: ${e.message}", e)
            null
        }
    }

    override suspend fun loadProjectJson(filename: String): JSONObject? {
        return try {
            loadAssetJson("projects/$filename.json")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading project $filename: ${e.message}", e)
            null
        }
    }

    private suspend fun loadAssetJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val inputStream = context.assets.open(path)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        JSONObject(jsonString)
    }
}
