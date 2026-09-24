package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an official IELTS Speaking prompt (Part 1, 2, or 3).
 */
data class IeltsSpeakingPrompt(
    val id: String,
    val part: Int, // 1: Interview, 2: Long Turn (Cue Card), 3: Two-way Discussion
    val topicEn: String,
    val topicFa: String,
    val questionEn: String,
    val questionFa: String,
    val cueCardBulletPoints: List<String> = emptyList(), // For Part 2
    val recommendedVocab: List<IeltsSpeakingVocabHint> = emptyList(),
    val prepTimeSeconds: Int = if (part == 2) 60 else 0,
    val speakingTimeSeconds: Int = if (part == 2) 120 else if (part == 1) 45 else 60,
    val followUpQuestions: List<String> = emptyList(),
    val isAiGenerated: Boolean = false
)

data class IeltsSpeakingVocabHint(
    val word: String,
    val phonetic: String,
    val meaningFa: String,
    val bandTarget: String = "7.5+"
)

data class VocabUpgradeItem(
    val original: String,
    val upgraded: String,
    val explanationFa: String
)

/**
 * Detailed assessment across the four IELTS Speaking assessment criteria.
 */
data class IeltsSpeakingFeedback(
    val overallBand: Float = 7.0f,
    val fluencyScore: Float = 7.0f,
    val fluencyFeedbackFa: String = "",
    val lexicalScore: Float = 7.0f,
    val lexicalFeedbackFa: String = "",
    val grammarScore: Float = 7.0f,
    val grammarFeedbackFa: String = "",
    val pronunciationScore: Float = 7.0f,
    val pronunciationHintsFa: String = "",
    val persianLearnerMistakes: List<String> = emptyList(),
    val vocabUpgrades: List<VocabUpgradeItem> = emptyList(),
    val band8ModelResponseEn: String = "",
    val band8ModelResponseFa: String = "",
    val actionableAdviceFa: List<String> = emptyList()
)

/**
 * Room database record preserving user's speaking test attempts and scores.
 */
@Entity(tableName = "ielts_speaking_sessions")
data class IeltsSpeakingSessionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val promptId: String,
    val part: Int,
    val topicEn: String,
    val topicFa: String,
    val questionEn: String,
    val userTranscript: String,
    val overallBand: Float,
    val fluencyScore: Float,
    val lexicalScore: Float,
    val grammarScore: Float,
    val pronunciationScore: Float,
    val fluencyFeedbackFa: String,
    val lexicalFeedbackFa: String,
    val grammarFeedbackFa: String,
    val pronunciationHintsFa: String,
    val modelAnswerEn: String = "",
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
