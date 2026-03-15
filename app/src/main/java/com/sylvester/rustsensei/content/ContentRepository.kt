package com.sylvester.rustsensei.content

import android.content.Context
import android.util.LruCache
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

    private val chapterCache = LruCache<String, BookChapter>(5)
    private val exerciseCache = LruCache<String, ExerciseData>(10)

    private var bookIndex: BookIndex? = null
    private var exerciseCategories: List<ExerciseCategory>? = null

    fun getBookIndex(): BookIndex {
        bookIndex?.let { return it }

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
        return index
    }

    fun getChapter(chapterId: String): BookChapter? {
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

    fun getSection(chapterId: String, sectionId: String): BookSection? {
        val chapter = getChapter(chapterId) ?: return null
        return chapter.sections.find { it.id == sectionId }
    }

    fun getExerciseCategories(): List<ExerciseCategory> {
        exerciseCategories?.let { return it }

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
        return categories
    }

    fun getExercise(exerciseId: String): ExerciseData? {
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

    fun getTotalSectionsCount(): Int {
        return getBookIndex().chapters.sumOf { it.sectionIds.size }
    }

    fun getTotalExercisesCount(): Int {
        return getExerciseCategories().sumOf { it.exercises.size }
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

    private fun loadAssetJson(path: String): JSONObject {
        val inputStream = context.assets.open(path)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return JSONObject(jsonString)
    }
}
