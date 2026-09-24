package com.example.data.repository

import com.example.data.ai.IeltsSpeakingPromptGenerator
import com.example.data.local.DailyStreakDao
import com.example.data.local.IeltsSpeakingDao
import com.example.data.local.UserProfileDao
import com.example.data.model.IeltsSpeakingFeedback
import com.example.data.model.IeltsSpeakingPrompt
import com.example.data.model.IeltsSpeakingSessionRecord
import com.example.data.model.StreakUpdateResult
import kotlinx.coroutines.flow.Flow

class IeltsSpeakingRepository(
    private val speakingDao: IeltsSpeakingDao,
    private val userProfileDao: UserProfileDao,
    private val streakDao: DailyStreakDao
) {
    private val streakRepository = DailyStreakRepository(streakDao, userProfileDao)

    val allSessions: Flow<List<IeltsSpeakingSessionRecord>> = speakingDao.getAllSessions()
    val totalSessionsCount: Flow<Int> = speakingDao.getTotalSessionsCount()
    val averageBandScore: Flow<Float?> = speakingDao.getAverageBand()

    fun getAvailablePrompts(): List<IeltsSpeakingPrompt> =
        IeltsSpeakingPromptGenerator.DEFAULT_PROMPTS

    suspend fun generatePrompt(part: Int, topic: String): IeltsSpeakingPrompt =
        IeltsSpeakingPromptGenerator.generateDynamicPrompt(part, topic)

    suspend fun evaluateAndSaveSession(
        prompt: IeltsSpeakingPrompt,
        userTranscript: String,
        durationSeconds: Int
    ): Pair<IeltsSpeakingFeedback, StreakUpdateResult> {
        val feedback = IeltsSpeakingPromptGenerator.evaluateSpeakingResponse(prompt, userTranscript)

        // Save to Room database
        val sessionRecord = IeltsSpeakingSessionRecord(
            promptId = prompt.id,
            part = prompt.part,
            topicEn = prompt.topicEn,
            topicFa = prompt.topicFa,
            questionEn = prompt.questionEn,
            userTranscript = userTranscript,
            overallBand = feedback.overallBand,
            fluencyScore = feedback.fluencyScore,
            lexicalScore = feedback.lexicalScore,
            grammarScore = feedback.grammarScore,
            pronunciationScore = feedback.pronunciationScore,
            fluencyFeedbackFa = feedback.fluencyFeedbackFa,
            lexicalFeedbackFa = feedback.lexicalFeedbackFa,
            grammarFeedbackFa = feedback.grammarFeedbackFa,
            pronunciationHintsFa = feedback.pronunciationHintsFa,
            modelAnswerEn = feedback.band8ModelResponseEn,
            durationSeconds = durationSeconds,
            timestamp = System.currentTimeMillis()
        )
        speakingDao.insertSession(sessionRecord)

        // Award gamified XP and record daily streak in Room
        val streakResult = streakRepository.recordPracticeActivity(
            itemsCount = 1,
            minutesSpent = (durationSeconds / 60).coerceAtLeast(3),
            xpEarned = 35,
            activityType = "IELTS_SPEAKING"
        )

        return Pair(feedback, streakResult)
    }

    suspend fun deleteSession(id: Long) = speakingDao.deleteSession(id)
}
