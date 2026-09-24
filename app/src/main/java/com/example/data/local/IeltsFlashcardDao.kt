package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.IeltsDeckWithFlashcards
import com.example.data.model.IeltsFlashcard
import com.example.data.model.IeltsVocabularyDeck
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing IELTS vocabulary decks and flashcards.
 * Provides full reactive Flow queries, Leitner/SRS filtering, bilingual search,
 * and review progress tracking.
 */
@Dao
interface IeltsFlashcardDao {

    // ==========================================
    // DECK OPERATIONS
    // ==========================================

    @Query("SELECT * FROM ielts_vocabulary_decks ORDER BY targetBand DESC, titleFa ASC")
    fun getAllDecks(): Flow<List<IeltsVocabularyDeck>>

    @Query("SELECT * FROM ielts_vocabulary_decks WHERE id = :deckId LIMIT 1")
    fun getDeckById(deckId: Long): Flow<IeltsVocabularyDeck?>

    @Query("SELECT * FROM ielts_vocabulary_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckByIdSync(deckId: Long): IeltsVocabularyDeck?

    @Query("SELECT * FROM ielts_vocabulary_decks WHERE topic = :topic ORDER BY targetBand DESC")
    fun getDecksByTopic(topic: String): Flow<List<IeltsVocabularyDeck>>

    @Query("SELECT * FROM ielts_vocabulary_decks WHERE targetBand = :targetBand ORDER BY titleFa ASC")
    fun getDecksByBand(targetBand: String): Flow<List<IeltsVocabularyDeck>>

    @Query("SELECT COUNT(*) FROM ielts_vocabulary_decks")
    fun getDeckCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ielts_vocabulary_decks")
    suspend fun getDeckCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: IeltsVocabularyDeck): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<IeltsVocabularyDeck>): List<Long>

    @Update
    suspend fun updateDeck(deck: IeltsVocabularyDeck)

    @Delete
    suspend fun deleteDeck(deck: IeltsVocabularyDeck)

    @Query("DELETE FROM ielts_vocabulary_decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Long)

    @Transaction
    @Query("SELECT * FROM ielts_vocabulary_decks WHERE id = :deckId LIMIT 1")
    fun getDeckWithFlashcards(deckId: Long): Flow<IeltsDeckWithFlashcards?>

    @Transaction
    @Query("SELECT * FROM ielts_vocabulary_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckWithFlashcardsSync(deckId: Long): IeltsDeckWithFlashcards?

    @Transaction
    @Query("SELECT * FROM ielts_vocabulary_decks ORDER BY targetBand DESC, titleFa ASC")
    fun getAllDecksWithFlashcards(): Flow<List<IeltsDeckWithFlashcards>>

    // ==========================================
    // FLASHCARD OPERATIONS
    // ==========================================

    @Query("SELECT * FROM ielts_flashcards WHERE deckId = :deckId ORDER BY id ASC")
    fun getCardsForDeck(deckId: Long): Flow<List<IeltsFlashcard>>

    @Query("SELECT * FROM ielts_flashcards WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getCardsForDeckSync(deckId: Long): List<IeltsFlashcard>

    @Query("SELECT * FROM ielts_flashcards WHERE id = :cardId LIMIT 1")
    fun getCardById(cardId: Long): Flow<IeltsFlashcard?>

    @Query("SELECT * FROM ielts_flashcards WHERE id = :cardId LIMIT 1")
    suspend fun getCardByIdSync(cardId: Long): IeltsFlashcard?

    @Query("SELECT * FROM ielts_flashcards WHERE normalizedWord = :normalizedWord LIMIT 1")
    suspend fun getCardByNormalizedWord(normalizedWord: String): IeltsFlashcard?

    @Query("SELECT * FROM ielts_flashcards ORDER BY id DESC")
    fun getAllCards(): Flow<List<IeltsFlashcard>>

    // ==========================================
    // SPACED REPETITION & LEITNER QUERIES
    // ==========================================

    @Query("SELECT * FROM ielts_flashcards WHERE deckId = :deckId AND nextReviewDueAt <= :currentTime ORDER BY nextReviewDueAt ASC")
    fun getDueCardsForDeck(deckId: Long, currentTime: Long = System.currentTimeMillis()): Flow<List<IeltsFlashcard>>

    @Query("SELECT * FROM ielts_flashcards WHERE nextReviewDueAt <= :currentTime ORDER BY nextReviewDueAt ASC")
    fun getAllDueCards(currentTime: Long = System.currentTimeMillis()): Flow<List<IeltsFlashcard>>

    @Query("SELECT COUNT(*) FROM ielts_flashcards WHERE deckId = :deckId AND nextReviewDueAt <= :currentTime")
    fun getDueCardsCountForDeck(deckId: Long, currentTime: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT COUNT(*) FROM ielts_flashcards WHERE nextReviewDueAt <= :currentTime")
    fun getTotalDueCardsCount(currentTime: Long = System.currentTimeMillis()): Flow<Int>

    @Query("SELECT * FROM ielts_flashcards WHERE deckId = :deckId AND leitnerBox = :box ORDER BY id ASC")
    fun getCardsByLeitnerBox(deckId: Long, box: Int): Flow<List<IeltsFlashcard>>

    // ==========================================
    // SEARCH & FILTERING (BILINGUAL)
    // ==========================================

    @Query("""
        SELECT * FROM ielts_flashcards 
        WHERE deckId = :deckId 
        AND (
            word LIKE '%' || :query || '%' 
            OR persianTranslation LIKE '%' || :query || '%' 
            OR exampleSentenceEn LIKE '%' || :query || '%' 
            OR exampleSentenceFa LIKE '%' || :query || '%'
            OR englishDefinition LIKE '%' || :query || '%'
        ) 
        ORDER BY word ASC
    """)
    fun searchCardsInDeck(deckId: Long, query: String): Flow<List<IeltsFlashcard>>

    @Query("""
        SELECT * FROM ielts_flashcards 
        WHERE (
            word LIKE '%' || :query || '%' 
            OR persianTranslation LIKE '%' || :query || '%' 
            OR exampleSentenceEn LIKE '%' || :query || '%' 
            OR exampleSentenceFa LIKE '%' || :query || '%'
            OR englishDefinition LIKE '%' || :query || '%'
        ) 
        ORDER BY word ASC
    """)
    fun searchAllCards(query: String): Flow<List<IeltsFlashcard>>

    @Query("SELECT * FROM ielts_flashcards WHERE ieltsTopic = :topic ORDER BY word ASC")
    fun getCardsByTopic(topic: String): Flow<List<IeltsFlashcard>>

    @Query("SELECT * FROM ielts_flashcards WHERE targetBand = :band ORDER BY word ASC")
    fun getCardsByTargetBand(band: String): Flow<List<IeltsFlashcard>>

    // ==========================================
    // BOOKMARKS / FAVORITES
    // ==========================================

    @Query("SELECT * FROM ielts_flashcards WHERE isBookmarked = 1 ORDER BY lastReviewedAt DESC")
    fun getBookmarkedCards(): Flow<List<IeltsFlashcard>>

    @Query("SELECT * FROM ielts_flashcards WHERE deckId = :deckId AND isBookmarked = 1 ORDER BY word ASC")
    fun getBookmarkedCardsForDeck(deckId: Long): Flow<List<IeltsFlashcard>>

    @Query("UPDATE ielts_flashcards SET isBookmarked = CASE WHEN isBookmarked = 1 THEN 0 ELSE 1 END WHERE id = :cardId")
    suspend fun toggleBookmark(cardId: Long)

    // ==========================================
    // PRACTICE & RANDOM SHUFFLE
    // ==========================================

    @Query("SELECT * FROM ielts_flashcards WHERE deckId = :deckId ORDER BY RANDOM() LIMIT :limit")
    fun getRandomCardsForDeck(deckId: Long, limit: Int): Flow<List<IeltsFlashcard>>

    @Query("SELECT * FROM ielts_flashcards ORDER BY RANDOM() LIMIT :limit")
    fun getRandomCards(limit: Int): Flow<List<IeltsFlashcard>>

    // ==========================================
    // PROGRESS & SRS REVIEWS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: IeltsFlashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<IeltsFlashcard>): List<Long>

    @Update
    suspend fun updateCard(card: IeltsFlashcard)

    @Delete
    suspend fun deleteCard(card: IeltsFlashcard)

    @Query("DELETE FROM ielts_flashcards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Long)

    @Query("DELETE FROM ielts_flashcards WHERE deckId = :deckId")
    suspend fun deleteAllCardsForDeck(deckId: Long)

    @Query("""
        UPDATE ielts_flashcards 
        SET 
            leitnerBox = :leitnerBox,
            intervalDays = :intervalDays,
            easeFactor = :easeFactor,
            reviewCount = reviewCount + 1,
            correctCount = correctCount + :correctIncrement,
            incorrectCount = incorrectCount + :incorrectIncrement,
            consecutiveCorrectStreak = :streak,
            masteryPercentage = :mastery,
            isMastered = :isMastered,
            lastReviewedAt = :reviewTime,
            nextReviewDueAt = :nextDueTime
        WHERE id = :cardId
    """)
    suspend fun updateReviewResult(
        cardId: Long,
        leitnerBox: Int,
        intervalDays: Int,
        easeFactor: Float,
        correctIncrement: Int,
        incorrectIncrement: Int,
        streak: Int,
        mastery: Int,
        isMastered: Boolean,
        reviewTime: Long,
        nextDueTime: Long
    )

    @Query("UPDATE ielts_flashcards SET isMastered = :isMastered, masteryPercentage = CASE WHEN :isMastered = 1 THEN 100 ELSE masteryPercentage END WHERE id = :cardId")
    suspend fun setMasteredStatus(cardId: Long, isMastered: Boolean)

    @Query("""
        UPDATE ielts_flashcards 
        SET 
            leitnerBox = 1,
            intervalDays = 1,
            easeFactor = 2.5,
            reviewCount = 0,
            correctCount = 0,
            incorrectCount = 0,
            consecutiveCorrectStreak = 0,
            masteryPercentage = 0,
            isMastered = 0,
            lastReviewedAt = 0,
            nextReviewDueAt = :resetDueTime
        WHERE deckId = :deckId
    """)
    suspend fun resetDeckProgress(deckId: Long, resetDueTime: Long = System.currentTimeMillis())

    // ==========================================
    // STATISTICS & COUNTS
    // ==========================================

    @Query("SELECT COUNT(*) FROM ielts_flashcards WHERE deckId = :deckId")
    fun getCardCountForDeck(deckId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM ielts_flashcards WHERE deckId = :deckId AND isMastered = 1")
    fun getMasteredCountForDeck(deckId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM ielts_flashcards")
    fun getTotalCardsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM ielts_flashcards WHERE isMastered = 1")
    fun getTotalMasteredCardsCount(): Flow<Int>
}
