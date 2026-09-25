package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiVocabCardGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.DailyStreakDao
import com.example.data.local.UserProfileDao
import com.example.data.model.DailyStreakRecord
import com.example.data.model.UserProfile
import com.example.data.repository.DailyStreakRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyStreakAndAiCardTest {

    private lateinit var database: AppDatabase
    private lateinit var streakDao: DailyStreakDao
    private lateinit var profileDao: UserProfileDao
    private lateinit var repository: DailyStreakRepository
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        streakDao = database.dailyStreakDao()
        profileDao = database.userProfileDao()
        profileDao.insertOrUpdate(UserProfile(id = 1, streakDays = 0, xp = 100))
        repository = DailyStreakRepository(streakDao, profileDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun dailyStreakTracker_recordsPracticeAndCalculatesActiveStreak() = runBlocking {
        val cal = Calendar.getInstance()
        val today = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = dateFormat.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgo = dateFormat.format(cal.time)

        // Insert practice for two days ago and yesterday
        streakDao.insertOrUpdate(DailyStreakRecord(date = twoDaysAgo, itemsPracticed = 5, xpEarned = 20))
        streakDao.insertOrUpdate(DailyStreakRecord(date = yesterday, itemsPracticed = 10, xpEarned = 30))

        // Before today's practice: streak is 2 (yesterday + two days ago)
        val recordsBeforeToday = streakDao.getAllRecordsSync()
        val streakBefore = repository.calculateCurrentStreak(recordsBeforeToday)
        assertEquals(2, streakBefore)

        // Record practice today via repository
        val updateResult = repository.recordPracticeActivity(
            itemsCount = 8,
            minutesSpent = 12,
            xpEarned = 25,
            activityType = "AI_CARD"
        )

        assertEquals(3, updateResult.newStreak)
        assertEquals(25, updateResult.xpEarned)

        // Verify record in Room database
        val todayRecord = streakDao.getRecordForDateSync(today)
        assertNotNull(todayRecord)
        assertEquals(8, todayRecord!!.itemsPracticed)
        assertEquals(12, todayRecord.minutesSpent)
        assertTrue(todayRecord.isGoalMet)

        // Verify streakInfo Flow
        val streakInfo = repository.streakInfo.first()
        assertEquals(3, streakInfo.currentStreak)
        assertTrue(streakInfo.isTodayCompleted)
        assertEquals(3, streakInfo.totalDaysPracticed)
        assertTrue(streakInfo.totalXp >= 125)
    }

    @Test
    fun streakMilestone_unlocksWhenThresholdReached() = runBlocking {
        // Record 3 consecutive days of practice
        val cal = Calendar.getInstance()
        for (i in 2 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = dateFormat.format(dayCal.time)
            streakDao.insertOrUpdate(
                DailyStreakRecord(
                    date = dateStr,
                    itemsPracticed = 6,
                    xpEarned = 20,
                    activityType = "VOCABULARY"
                )
            )
        }

        val streakInfo = repository.streakInfo.first()
        assertEquals(3, streakInfo.currentStreak)
        assertTrue(streakInfo.unlockedMilestones.any { it.requiredDays == 3 })
        assertEquals("Learning Spark", streakInfo.unlockedMilestones.first { it.requiredDays == 3 }.title)
    }

    @Test
    fun aiVocabCardGenerator_generatesRichVocabularyWithPersianAndPhonetics() = runBlocking {
        // Test high-yield academic vocabulary word
        val card = AiVocabCardGenerator.generateVocabularyCard("Mitigate")

        assertEquals("Mitigate", card.word)
        assertEquals("/ˈmɪt.ɪ.ɡeɪt/", card.phonetic)
        assertEquals("verb", card.partOfSpeech)
        assertTrue(card.persianTranslation.contains("کاهش دادن"))
        assertTrue(card.exampleSentenceEn.isNotBlank())
        assertTrue(card.exampleSentenceFa.isNotBlank())
        assertTrue(card.collocations.isNotEmpty())
        assertTrue(card.synonyms.isNotEmpty())
        assertTrue(card.ieltsTipFa.isNotBlank())
    }

    @Test
    fun aiVocabCardGenerator_handlesCustomWordGracefully() = runBlocking {
        // Test custom word
        val card = AiVocabCardGenerator.generateVocabularyCard("collaboration")

        assertEquals("Collaboration", card.word)
        assertEquals("noun", card.partOfSpeech)
        assertTrue(card.persianTranslation.isNotBlank())
        assertTrue(card.exampleSentenceEn.isNotBlank())
        assertTrue(card.exampleSentenceFa.isNotBlank())
    }
}
