package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DailyStreakDao
import com.example.data.local.ExamTrackDao
import com.example.data.local.UserProfileDao
import com.example.data.model.ExamTrackType
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPackItem
import com.example.data.repository.DailyStreakRepository
import com.example.data.repository.ExamTrackRepository
import com.example.data.seed.ExamTrackDataSeed
import com.example.data.seed.InitialDataSeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExamTrackModuleTest {

    private lateinit var database: AppDatabase
    private lateinit var examTrackDao: ExamTrackDao
    private lateinit var streakDao: DailyStreakDao
    private lateinit var profileDao: UserProfileDao
    private lateinit var repository: ExamTrackRepository
    private lateinit var streakRepository: DailyStreakRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        examTrackDao = database.examTrackDao()
        streakDao = database.dailyStreakDao()
        profileDao = database.userProfileDao()
        profileDao.insertOrUpdate(UserProfile(id = 1, streakDays = 2, xp = 100))

        repository = ExamTrackRepository(
            examTrackDao,
            profileDao,
            streakDao,
            database.vocabularyDao()
        )
        streakRepository = DailyStreakRepository(streakDao, profileDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun examTrackSeeds_containCuratedWordsForIeltsToeflAndGre() {
        val ieltsWords = ExamTrackDataSeed.getIeltsWords()
        val toeflWords = ExamTrackDataSeed.getToeflWords()
        val greWords = ExamTrackDataSeed.getGreWords()

        assertTrue("IELTS words should not be empty", ieltsWords.size >= 10)
        assertTrue("TOEFL words should not be empty", toeflWords.size >= 10)
        assertTrue("GRE words should not be empty", greWords.size >= 10)

        ieltsWords.forEach { word ->
            assertTrue(word.word.isNotBlank())
            assertTrue(word.persianMeaning.isNotBlank())
            assertTrue(word.exampleEn.isNotBlank())
            assertTrue(word.exampleFa.isNotBlank())
            assertTrue(word.stageNumber in 1..4)
        }

        toeflWords.forEach { word ->
            assertTrue(word.word.isNotBlank())
            assertTrue(word.persianMeaning.isNotBlank())
            assertTrue(word.exampleEn.isNotBlank())
            assertTrue(word.exampleFa.isNotBlank())
            assertTrue(word.stageNumber in 1..4)
        }

        greWords.forEach { word ->
            assertTrue(word.word.isNotBlank())
            assertTrue(word.persianMeaning.isNotBlank())
            assertTrue(word.exampleEn.isNotBlank())
            assertTrue(word.exampleFa.isNotBlank())
            assertTrue(word.stageNumber in 1..4)
        }
    }

    @Test
    fun downloadedMasterVocabulary_appearsReactivelyInIeltsTrack() = runBlocking {
        val insertedId = database.vocabularyDao().insert(
            VocabularyItem(
                word = "institutionalize",
                persianMeaning = "نهادینه کردن",
                englishDefinition = "to establish something as a normal or accepted system",
                cefrLevel = "C1",
                ieltsRelevance = "High",
                tags = listOf("IELTS", "Academic"),
                source = "test",
                examPriority = 88
            )
        )
        database.vocabularyPackItemDao().insert(
            VocabularyPackItem(
                packId = InitialDataSeed.IELTS_MASTER_PACK_ID,
                vocabularyId = insertedId
            )
        )

        val state = repository.getTrackState(ExamTrackType.IELTS).first()
        val allWords = state.stages.flatMap { it.words }
        val downloaded = allWords.firstOrNull { it.word.equals("institutionalize", ignoreCase = true) }

        assertTrue("Downloaded master-bank word must enter IELTS track", downloaded != null)
        assertEquals("نهادینه کردن", downloaded?.persianMeaning)
        assertTrue(downloaded?.id?.startsWith("master_ielts_") == true)
        assertTrue(downloaded != null && downloaded.stageNumber in 1..4)
    }

    @Test
    fun examTracks_canBeProgressedIndependentlyStageByStage() = runBlocking {
        var ieltsState = repository.getTrackState(ExamTrackType.IELTS).first()
        assertEquals(0, ieltsState.totalWordsLearned)
        assertEquals(0, ieltsState.overallPercentage)
        assertEquals(4, ieltsState.stages.size)

        repository.updateWordMastery(
            trackType = ExamTrackType.IELTS,
            wordId = "ielts_s1_01",
            stageNumber = 1,
            isMastered = true
        )
        repository.updateWordMastery(
            trackType = ExamTrackType.IELTS,
            wordId = "ielts_s1_02",
            stageNumber = 1,
            isMastered = true
        )

        ieltsState = repository.getTrackState(ExamTrackType.IELTS).first()
        assertEquals(2, ieltsState.totalWordsLearned)
        assertTrue(ieltsState.overallPercentage > 0)
        assertEquals(2, ieltsState.wordsStudiedToday)

        val stage1 = ieltsState.stages.first { it.stageNumber == 1 }
        assertEquals(2, stage1.masteredCount)
        assertTrue(stage1.progressPercentage > 0)

        val toeflState = repository.getTrackState(ExamTrackType.TOEFL).first()
        assertEquals(0, toeflState.totalWordsLearned)
        assertEquals(0, toeflState.overallPercentage)

        val greState = repository.getTrackState(ExamTrackType.GRE).first()
        assertEquals(0, greState.totalWordsLearned)
        assertEquals(0, greState.overallPercentage)

        repository.updateWordMastery(
            trackType = ExamTrackType.GRE,
            wordId = "gre_s1_01",
            stageNumber = 1,
            isMastered = true
        )

        val updatedGreState = repository.getTrackState(ExamTrackType.GRE).first()
        assertEquals(1, updatedGreState.totalWordsLearned)
        assertEquals(1, updatedGreState.wordsStudiedToday)

        val streakInfo = streakRepository.streakInfo.first()
        assertTrue(streakInfo.isTodayCompleted)
        assertTrue(streakInfo.totalXp >= 145)
    }

    @Test
    fun dailyGoal_isConfigurableAndCalculatesDailyPercentage() = runBlocking {
        repository.updateDailyGoal(ExamTrackType.TOEFL, 5)

        var toeflState = repository.getTrackState(ExamTrackType.TOEFL).first()
        assertEquals(5, toeflState.dailyGoalWords)

        repository.updateWordMastery(ExamTrackType.TOEFL, "toefl_s1_01", 1, true)
        repository.updateWordMastery(ExamTrackType.TOEFL, "toefl_s1_02", 1, true)
        repository.updateWordMastery(ExamTrackType.TOEFL, "toefl_s1_03", 1, true)

        toeflState = repository.getTrackState(ExamTrackType.TOEFL).first()
        assertEquals(3, toeflState.wordsStudiedToday)
        assertEquals(60, toeflState.dailyProgressPercentage)
        assertFalse(toeflState.isDailyGoalMet)

        repository.updateWordMastery(ExamTrackType.TOEFL, "toefl_s1_04", 1, true)
        repository.updateWordMastery(ExamTrackType.TOEFL, "toefl_s1_05", 1, true)

        toeflState = repository.getTrackState(ExamTrackType.TOEFL).first()
        assertEquals(5, toeflState.wordsStudiedToday)
        assertEquals(100, toeflState.dailyProgressPercentage)
        assertTrue(toeflState.isDailyGoalMet)
    }
}
