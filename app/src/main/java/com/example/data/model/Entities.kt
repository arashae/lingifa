package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "vocabulary_items",
    indices = [
        Index(value = ["normalizedWord"]),
        Index(value = ["word"]),
        Index(value = ["nextReview"]),
        Index(value = ["cefrLevel"]),
        Index(value = ["mastery"]),
        Index(value = ["isFavorite"])
    ]
)
data class VocabularyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val normalizedWord: String = word.lowercase().trim(),
    val ipa: String = "",
    val persianMeaning: String,
    val englishDefinition: String = "",
    val partOfSpeech: String = "word",
    val example: String = "",
    val examplePersian: String = "",
    val cefrLevel: String = "B2",
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val wordFamily: List<String> = emptyList(),
    val commonMistakes: String = "",
    val ieltsRelevance: String = "Medium",
    val toeflRelevance: String = "Medium",
    @ColumnInfo(defaultValue = "'Medium'")
    val greRelevance: String = "Medium",
    val tags: List<String> = emptyList(),
    val source: String = "Default",
    @ColumnInfo(defaultValue = "''")
    val sourceLicense: String = "",
    @ColumnInfo(defaultValue = "'1'")
    val datasetVersion: String = "1",
    @ColumnInfo(defaultValue = "0")
    val frequencyRank: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val examPriority: Int = 0,
    /** Stable position in the CEFR curriculum; lower values are introduced first. */
    @ColumnInfo(defaultValue = "0")
    val learningOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val mastery: Int = 0, // 0 to 100
    val difficulty: Float = 2.5f,
    val stability: Float = 1.0f,
    val intervalDays: Int = 0,
    val lastReview: Long = 0L,
    val nextReview: Long = System.currentTimeMillis(),
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val isFavorite: Boolean = false,
    /**
     * Legacy single-pack field kept temporarily for backward compatibility with older seed/import code.
     * New code should use VocabularyPackItem so one word can belong to many packs.
     */
    val packName: String = ""
)

@Entity(
    tableName = "vocabulary_senses",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyItem::class,
            parentColumns = ["id"],
            childColumns = ["vocabularyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vocabularyId"]),
        Index(value = ["vocabularyId", "senseIndex"], unique = true)
    ]
)
data class VocabularySense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vocabularyId: Long,
    val senseIndex: Int = 1,
    val partOfSpeech: String = "",
    val cefrLevel: String = "",
    val englishDefinition: String = "",
    val persianMeaning: String = "",
    val exampleSentence: String = "",
    val exampleTranslation: String = "",
    val collocations: List<String> = emptyList(),
    val isPrimary: Boolean = true
)

data class VocabularyWithSenses(
    @Embedded val item: VocabularyItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "vocabularyId"
    )
    val senses: List<VocabularySense> = emptyList()
)

@Entity(tableName = "vocabulary_packs")
data class VocabularyPack(
    @PrimaryKey
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descriptionFa: String,
    val level: String,
    val exam: String,
    val wordCount: Int,
    val isDownloaded: Boolean = true,
    val category: String,
    val iconName: String,
    @ColumnInfo(defaultValue = "1")
    val version: Int = 1,
    @ColumnInfo(defaultValue = "'LinguaFa'")
    val source: String = "LinguaFa",
    @ColumnInfo(defaultValue = "0")
    val targetWordCount: Int = wordCount,
    @ColumnInfo(defaultValue = "0")
    val installedWordCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isCorePack: Boolean = false
)

/**
 * Many-to-many membership between vocabulary items and vocabulary packs.
 *
 * A word such as "allocate" can simultaneously belong to IELTS, TOEFL,
 * GRE, Academic English and a user-created pack without duplicating the word row.
 */
@Entity(
    tableName = "vocabulary_pack_items",
    primaryKeys = ["packId", "vocabularyId"],
    indices = [Index(value = ["vocabularyId"])]
)
data class VocabularyPackItem(
    val packId: String,
    val vocabularyId: Long,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mistake_records")
data class MistakeRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val question: String,
    val myAnswer: String,
    val correctAnswer: String,
    val explanationFa: String,
    val whyWrongFa: String = "",
    val relatedConcept: String = "",
    val skillType: String = "VOCABULARY", // VOCABULARY, GRAMMAR, READING, LISTENING, SPEAKING, WRITING
    val createdAt: Long = System.currentTimeMillis(),
    val isReviewed: Boolean = false
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "Learner",
    val targetGoal: String = "IELTS",
    val currentLevel: String = "B2",
    val dailyMinutes: Int = 35,
    val weakestSkill: String = "Speaking",
    val targetBandOrScore: String = "7.5",
    val testDateFa: String = "",
    val xp: Int = 0,
    val streakDays: Int = 0,
    val wordsLearnedCount: Int = 0,
    val isOnboardingCompleted: Boolean = false
)

data class GrammarQuizQuestion(
    val questionEn: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationFa: String
)

data class GrammarTopic(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val level: String,
    val descriptionFa: String,
    val rulesFa: String,
    val examplesEn: List<String>,
    val examplesFa: List<String>,
    val iranianCommonMistakesFa: String,
    val quizQuestions: List<GrammarQuizQuestion>
)

data class ReadingQuestion(
    val id: String,
    val questionEn: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationFa: String,
    val questionType: String = "Inference" // Gist, Detail, Inference, Vocabulary
)

data class ReadingPassage(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val topic: String,
    val level: String,
    val exam: String,
    val contentEn: String,
    val summaryFa: String,
    val questions: List<ReadingQuestion>
)

data class ListeningQuestion(
    val id: String,
    val questionEn: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanationFa: String
)

data class ListeningExercise(
    val id: String,
    val titleEn: String,
    val titleFa: String,
    val topic: String,
    val level: String,
    val exam: String,
    val audioScriptEn: String,
    val transcriptFa: String,
    val questions: List<ListeningQuestion>
)
