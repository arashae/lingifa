package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Entity representing an IELTS vocabulary deck (e.g. topic-specific or band-specific collection).
 */
@Entity(tableName = "ielts_vocabulary_decks")
data class IeltsVocabularyDeck(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String = "",
    val descriptionFa: String = "",
    val targetBand: String = "7.5", // e.g. "6.5", "7.0", "7.5", "8.0+"
    val topic: String = "General Academic", // Environment, Technology, Education, Society, Health, Economy, Art
    val examModule: String = "Academic", // Academic, General Training, Both
    val colorHex: String = "#1E88E5",
    val iconName: String = "school",
    val totalCardsCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing an individual flashcard in an IELTS vocabulary deck.
 * Includes detailed Persian translations, phonetic transcriptions, IELTS context example sentences,
 * Persian sentence translations, collocations, synonyms/antonyms, and Leitner/SRS progress.
 */
@Entity(
    tableName = "ielts_flashcards",
    foreignKeys = [
        ForeignKey(
            entity = IeltsVocabularyDeck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["normalizedWord"]),
        Index(value = ["nextReviewDueAt"]),
        Index(value = ["ieltsTopic"]),
        Index(value = ["targetBand"]),
        Index(value = ["isMastered"]),
        Index(value = ["isBookmarked"])
    ]
)
data class IeltsFlashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "deckId")
    val deckId: Long,
    val word: String,
    val normalizedWord: String = word.lowercase().trim(),
    val phonetic: String = "", // IPA notation, e.g. "/ˈmɪt.ɪ.ɡeɪt/"
    val partOfSpeech: String = "verb", // noun, verb, adjective, adverb, collocation, idiom
    val persianTranslation: String, // Persian meaning/translation (e.g. "کاهش دادن، تعدیل کردن، تسکین دادن")
    val englishDefinition: String = "",
    val exampleSentenceEn: String, // Primary IELTS exam example sentence in English
    val exampleSentenceFa: String, // Persian translation of the primary example sentence
    val secondaryExampleEn: String = "", // Secondary academic context (e.g. Task 2 or Speaking Part 3)
    val secondaryExampleFa: String = "", // Persian translation of secondary example
    val ieltsTopic: String = "General Academic",
    val targetBand: String = "7.5",
    val skillFocus: String = "Writing Task 2 & Speaking", // "Writing Task 1", "Writing Task 2", "Speaking Part 2/3", "Reading"
    val collocations: List<String> = emptyList(), // e.g. ["mitigate climate change", "take steps to mitigate", "mitigate risks"]
    val synonyms: List<String> = emptyList(), // e.g. ["alleviate", "lessen", "attenuate", "diminish"]
    val antonyms: List<String> = emptyList(), // e.g. ["exacerbate", "aggravate", "worsen"]
    val usageNoteFa: String = "", // Persian practical advice & IELTS tips for Iranian candidates
    val commonMistakesFa: String = "", // Common errors to avoid in IELTS writing/speaking

    // Spaced Repetition System (SRS) & Leitner Flashcard Tracking
    val leitnerBox: Int = 1, // Leitner Box 1 (daily) to 5 (mastered/monthly)
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val consecutiveCorrectStreak: Int = 0,
    val masteryPercentage: Int = 0, // 0 to 100%
    val isMastered: Boolean = false,
    val isBookmarked: Boolean = false,
    val lastReviewedAt: Long = 0L,
    val nextReviewDueAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * One-to-many relationship class between an IELTS deck and its flashcards.
 */
data class IeltsDeckWithFlashcards(
    @Embedded
    val deck: IeltsVocabularyDeck,
    @Relation(
        parentColumn = "id",
        entityColumn = "deckId"
    )
    val flashcards: List<IeltsFlashcard>
)

/**
 * Data projection for deck study progress summary.
 */
data class IeltsDeckProgressSummary(
    val deckId: Long,
    val totalCards: Int,
    val masteredCards: Int,
    val learningCards: Int,
    val dueCards: Int,
    val bookmarkedCards: Int
)
