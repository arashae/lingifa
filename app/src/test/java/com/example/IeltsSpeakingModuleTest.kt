package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.IeltsSpeakingPromptGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.DailyStreakDao
import com.example.data.local.IeltsSpeakingDao
import com.example.data.local.UserProfileDao
import com.example.data.model.UserProfile
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.IeltsSpeakingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IeltsSpeakingModuleTest {

    private lateinit var database: AppDatabase
    private lateinit var speakingDao: IeltsSpeakingDao
    private lateinit var streakDao: DailyStreakDao
    private lateinit var profileDao: UserProfileDao
    private lateinit var repository: IeltsSpeakingRepository
    private lateinit var streakRepository: DailyStreakRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        speakingDao = database.ieltsSpeakingDao()
        streakDao = database.dailyStreakDao()
        profileDao = database.userProfileDao()
        profileDao.insertOrUpdate(UserProfile(id = 1, streakDays = 1, xp = 50))

        repository = IeltsSpeakingRepository(speakingDao, profileDao, streakDao)
        streakRepository = DailyStreakRepository(streakDao, profileDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun promptGenerator_providesOfficialPartsAndCueCardStructure() {
        val prompts = IeltsSpeakingPromptGenerator.DEFAULT_PROMPTS
        assertTrue(prompts.isNotEmpty())

        // Verify Part 2 Cue Cards have bullet points & prep timers
        val part2Prompts = prompts.filter { it.part == 2 }
        assertTrue(part2Prompts.isNotEmpty())
        val cueCard = part2Prompts.first()
        assertEquals(60, cueCard.prepTimeSeconds)
        assertEquals(120, cueCard.speakingTimeSeconds)
        assertTrue(cueCard.cueCardBulletPoints.size >= 3)
        assertTrue(cueCard.recommendedVocab.isNotEmpty())
        assertTrue(cueCard.questionFa.isNotBlank())

        // Verify Part 1
        val part1 = prompts.first { it.part == 1 }
        assertEquals(0, part1.prepTimeSeconds)
        assertEquals(45, part1.speakingTimeSeconds)
        assertTrue(part1.topicFa.isNotBlank())
    }

    @Test
    fun realTimeEvaluation_assessesFourCriteriaAndPersianNuances() = runBlocking {
        val prompt = IeltsSpeakingPromptGenerator.DEFAULT_PROMPTS.first { it.part == 2 }

        val sampleTranscript = """
            Well, I would like to talk about an essential environmental project in my hometown.
            The government decided to mitigate air pollution by subsidizing renewable energy.
            Although traffic congestion was severe, this laudable initiative has significantly
            improved public health. Furthermore, from my perspective, community resilience is vital.
        """.trimIndent()

        val feedback = IeltsSpeakingPromptGenerator.evaluateSpeakingResponse(prompt, sampleTranscript)

        // Verify scores
        assertTrue(feedback.overallBand in 6.0f..9.0f)
        assertTrue(feedback.fluencyScore >= 6.0f)
        assertTrue(feedback.lexicalScore >= 6.5f)
        assertTrue(feedback.grammarScore >= 6.0f)
        assertTrue(feedback.pronunciationScore >= 6.5f)

        // Verify Persian explanations & guidance
        assertTrue(feedback.fluencyFeedbackFa.isNotBlank())
        assertTrue(feedback.lexicalFeedbackFa.isNotBlank())
        assertTrue(feedback.grammarFeedbackFa.isNotBlank())
        assertTrue(feedback.pronunciationHintsFa.isNotBlank())
        assertTrue(feedback.persianLearnerMistakes.isNotEmpty())

        // Verify Model Band 8 Answer in English and Persian
        assertTrue(feedback.band8ModelResponseEn.isNotBlank())
        assertTrue(feedback.band8ModelResponseFa.isNotBlank())
    }

    @Test
    fun speakingSession_savesToRoomAndUpdatesDailyStreak() = runBlocking {
        val prompt = IeltsSpeakingPromptGenerator.DEFAULT_PROMPTS[0]
        val transcript = "I strongly believe that artificial intelligence has become ubiquitous in our lives."

        val (feedback, streakResult) = repository.evaluateAndSaveSession(
            prompt = prompt,
            userTranscript = transcript,
            durationSeconds = 90
        )

        assertNotNull(feedback)
        assertNotNull(streakResult)
        assertEquals(35, streakResult.xpEarned)

        // Verify Room persistence
        val savedSessions = repository.allSessions.first()
        assertEquals(1, savedSessions.size)
        val session = savedSessions[0]
        assertEquals(prompt.part, session.part)
        assertEquals(prompt.topicEn, session.topicEn)
        assertEquals(transcript, session.userTranscript)
        assertEquals(feedback.overallBand, session.overallBand)

        // Verify average band score query
        val avgBand = repository.averageBandScore.first()
        assertNotNull(avgBand)
        assertEquals(feedback.overallBand, avgBand!!, 0.01f)

        // Verify Daily Streak updated in Room
        val streakInfo = streakRepository.streakInfo.first()
        assertTrue(streakInfo.isTodayCompleted)
        assertTrue(streakInfo.totalXp >= 85)
    }
}
