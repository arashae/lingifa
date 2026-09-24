package com.example.data.repository

import com.example.data.local.IeltsFlashcardDao
import com.example.data.model.IeltsDeckWithFlashcards
import com.example.data.model.IeltsFlashcard
import com.example.data.model.IeltsVocabularyDeck
import kotlinx.coroutines.flow.Flow
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Quality rating when reviewing an IELTS flashcard.
 */
enum class FlashcardRating {
    AGAIN, // Failed / complete blackout (0-1)
    HARD,  // Remembered with substantial effort (2)
    GOOD,  // Normal recall (3-4)
    EASY   // Instant effortless recall (5)
}

/**
 * Repository providing access to IELTS vocabulary decks and flashcards,
 * incorporating Leitner box progression and SM-2-inspired spaced repetition logic.
 */
class IeltsFlashcardRepository(
    private val ieltsDao: IeltsFlashcardDao
) {
    val allDecks: Flow<List<IeltsVocabularyDeck>> = ieltsDao.getAllDecks()
    val allDecksWithCards: Flow<List<IeltsDeckWithFlashcards>> = ieltsDao.getAllDecksWithFlashcards()
    val totalDecksCount: Flow<Int> = ieltsDao.getDeckCount()
    val totalCardsCount: Flow<Int> = ieltsDao.getTotalCardsCount()
    val totalMasteredCardsCount: Flow<Int> = ieltsDao.getTotalMasteredCardsCount()
    val totalDueCardsCount: Flow<Int> = ieltsDao.getTotalDueCardsCount()

    fun getDeckById(deckId: Long): Flow<IeltsVocabularyDeck?> = ieltsDao.getDeckById(deckId)

    fun getDeckWithFlashcards(deckId: Long): Flow<IeltsDeckWithFlashcards?> =
        ieltsDao.getDeckWithFlashcards(deckId)

    fun getCardsForDeck(deckId: Long): Flow<List<IeltsFlashcard>> =
        ieltsDao.getCardsForDeck(deckId)

    fun getDueCardsForDeck(deckId: Long): Flow<List<IeltsFlashcard>> =
        ieltsDao.getDueCardsForDeck(deckId)

    fun getDueCardsCountForDeck(deckId: Long): Flow<Int> =
        ieltsDao.getDueCardsCountForDeck(deckId)

    fun getAllDueCards(): Flow<List<IeltsFlashcard>> =
        ieltsDao.getAllDueCards()

    fun getCardById(cardId: Long): Flow<IeltsFlashcard?> =
        ieltsDao.getCardById(cardId)

    fun getCardsByTopic(topic: String): Flow<List<IeltsFlashcard>> =
        ieltsDao.getCardsByTopic(topic)

    fun getCardsByBand(targetBand: String): Flow<List<IeltsFlashcard>> =
        ieltsDao.getCardsByTargetBand(targetBand)

    fun getBookmarkedCards(): Flow<List<IeltsFlashcard>> =
        ieltsDao.getBookmarkedCards()

    fun getBookmarkedCardsForDeck(deckId: Long): Flow<List<IeltsFlashcard>> =
        ieltsDao.getBookmarkedCardsForDeck(deckId)

    fun searchCardsInDeck(deckId: Long, query: String): Flow<List<IeltsFlashcard>> =
        ieltsDao.searchCardsInDeck(deckId, query.trim())

    fun searchAllCards(query: String): Flow<List<IeltsFlashcard>> =
        ieltsDao.searchAllCards(query.trim())

    fun getRandomCardsForDeck(deckId: Long, limit: Int = 10): Flow<List<IeltsFlashcard>> =
        ieltsDao.getRandomCardsForDeck(deckId, limit)

    fun getRandomPracticeCards(limit: Int = 10): Flow<List<IeltsFlashcard>> =
        ieltsDao.getRandomCards(limit)

    suspend fun insertDeck(deck: IeltsVocabularyDeck): Long =
        ieltsDao.insertDeck(deck)

    suspend fun insertDecks(decks: List<IeltsVocabularyDeck>): List<Long> =
        ieltsDao.insertDecks(decks)

    suspend fun updateDeck(deck: IeltsVocabularyDeck) =
        ieltsDao.updateDeck(deck)

    suspend fun deleteDeckById(deckId: Long) =
        ieltsDao.deleteDeckById(deckId)

    suspend fun insertCard(card: IeltsFlashcard): Long {
        val id = ieltsDao.insertCard(card)
        updateDeckCount(card.deckId)
        return id
    }

    suspend fun insertCards(cards: List<IeltsFlashcard>): List<Long> {
        val ids = ieltsDao.insertCards(cards)
        val deckIds = cards.map { it.deckId }.distinct()
        deckIds.forEach { updateDeckCount(it) }
        return ids
    }

    suspend fun updateCard(card: IeltsFlashcard) =
        ieltsDao.updateCard(card)

    suspend fun deleteCardById(cardId: Long, deckId: Long? = null) {
        ieltsDao.deleteCardById(cardId)
        if (deckId != null) {
            updateDeckCount(deckId)
        }
    }

    suspend fun toggleBookmark(cardId: Long) =
        ieltsDao.toggleBookmark(cardId)

    suspend fun setMasteredStatus(cardId: Long, isMastered: Boolean) =
        ieltsDao.setMasteredStatus(cardId, isMastered)

    suspend fun resetDeckProgress(deckId: Long) =
        ieltsDao.resetDeckProgress(deckId)

    /**
     * Process a flashcard review according to Leitner Box and SM-2 Spaced Repetition algorithms.
     * Updates:
     * - Leitner Box (1 through 5)
     * - Ease factor and interval days
     * - Mastery percentage
     * - Next review due timestamp
     */
    suspend fun processFlashcardReview(cardId: Long, rating: FlashcardRating): IeltsFlashcard? {
        val card = ieltsDao.getCardByIdSync(cardId) ?: return null
        val now = System.currentTimeMillis()

        var newBox = card.leitnerBox
        var newIntervalDays = card.intervalDays
        var newEaseFactor = card.easeFactor
        var correctInc = 0
        var incorrectInc = 0
        var streak = card.consecutiveCorrectStreak
        var isMastered = card.isMastered

        when (rating) {
            FlashcardRating.AGAIN -> {
                // Demote back to Leitner Box 1
                newBox = 1
                newIntervalDays = 1
                newEaseFactor = max(1.3f, newEaseFactor - 0.2f)
                incorrectInc = 1
                streak = 0
                isMastered = false
            }
            FlashcardRating.HARD -> {
                // Keep in current box or slight progress
                newIntervalDays = max(1, (card.intervalDays * 1.2f).roundToInt())
                newEaseFactor = max(1.3f, newEaseFactor - 0.15f)
                correctInc = 1
                streak += 1
            }
            FlashcardRating.GOOD -> {
                // Promote to next Leitner Box
                newBox = min(5, card.leitnerBox + 1)
                newIntervalDays = when (newBox) {
                    1 -> 1
                    2 -> 3
                    3 -> 7
                    4 -> 14
                    else -> 30
                }
                newEaseFactor = min(3.0f, newEaseFactor + 0.05f)
                correctInc = 1
                streak += 1
                if (newBox >= 4) {
                    isMastered = true
                }
            }
            FlashcardRating.EASY -> {
                // Fast-track promotion
                newBox = min(5, card.leitnerBox + 2)
                newIntervalDays = when (newBox) {
                    1 -> 2
                    2 -> 5
                    3 -> 10
                    4 -> 21
                    else -> 45
                }
                newEaseFactor = min(3.0f, newEaseFactor + 0.15f)
                correctInc = 1
                streak += 1
                if (newBox >= 4) {
                    isMastered = true
                }
            }
        }

        val totalReviews = card.reviewCount + 1
        val totalCorrect = card.correctCount + correctInc
        val mastery = min(100, max(0, ((newBox * 15) + (totalCorrect.toFloat() / totalReviews * 25)).roundToInt()))
        val nextDue = now + (newIntervalDays.toLong() * 24L * 60L * 60L * 1000L)

        ieltsDao.updateReviewResult(
            cardId = cardId,
            leitnerBox = newBox,
            intervalDays = newIntervalDays,
            easeFactor = newEaseFactor,
            correctIncrement = correctInc,
            incorrectIncrement = incorrectInc,
            streak = streak,
            mastery = mastery,
            isMastered = isMastered,
            reviewTime = now,
            nextDueTime = nextDue
        )

        return ieltsDao.getCardByIdSync(cardId)
    }

    private suspend fun updateDeckCount(deckId: Long) {
        val deck = ieltsDao.getDeckByIdSync(deckId) ?: return
        val cards = ieltsDao.getCardsForDeckSync(deckId)
        ieltsDao.updateDeck(deck.copy(totalCardsCount = cards.size, updatedAt = System.currentTimeMillis()))
    }
}
