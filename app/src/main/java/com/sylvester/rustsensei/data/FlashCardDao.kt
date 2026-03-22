package com.sylvester.rustsensei.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashCardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(card: FlashCard): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(cards: List<FlashCard>)

    @Update
    suspend fun update(card: FlashCard)

    @Query("SELECT * FROM flash_cards WHERE nextReviewAt <= :now ORDER BY nextReviewAt ASC LIMIT :limit")
    suspend fun getDueCards(now: Long = System.currentTimeMillis(), limit: Int = 20): List<FlashCard>

    @Query("SELECT COUNT(*) FROM flash_cards WHERE nextReviewAt <= :now")
    fun getDueCardCount(now: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT COUNT(*) FROM flash_cards WHERE nextReviewAt <= :now")
    suspend fun getDueCardCountSync(now: Long): Int

    @Query("SELECT COUNT(*) FROM flash_cards")
    fun getTotalCardCount(): Flow<Int>

    @Query("SELECT * FROM flash_cards WHERE category = :category")
    suspend fun getCardsByCategory(category: String): List<FlashCard>

    @Query("SELECT DISTINCT category FROM flash_cards ORDER BY category")
    suspend fun getCategories(): List<String>

    @Query("DELETE FROM flash_cards")
    suspend fun deleteAll()
}
