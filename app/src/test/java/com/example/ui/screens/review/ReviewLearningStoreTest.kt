package com.example.ui.screens.review

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.srs.Fsrs6
import com.example.srs.ReviewRating
import com.example.vocab.ReviewEvidence
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReviewLearningStoreTest {
    private lateinit var db: AppDatabase
    private lateinit var store: ReviewLearningStore
    private val now = 1_700_000_000_000L
    private val item = VocabularyItem(id = 1, word = "buy", persianMeaning = "خریدن", correctCount = 2, nextReview = now)
    private fun task() = ReviewTask(item, ReviewMode.WORD_RECALL, item.persianMeaning, item.word)
    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java).allowMainThreadQueries().build()
        db.vocabularyDao().insert(item)
        store = ReviewLearningStore(db)
    }
    @After fun close() = db.close()

    @Test fun `one skill cannot postpone another due skill`() = runBlocking {
        db.reviewLearningDao().saveSkill(VocabularySkillProgress(1, axis = "MEANING", stability = 3.0,
            difficulty = 5.0, firstReview = now - 7 * Fsrs6.DAY_MS, lastReview = now - 3 * Fsrs6.DAY_MS, nextReview = now - 1))
        store.record(task(), "buy", ReviewRating.GOOD, now)
        assertEquals(now - 1, db.vocabularyDao().getByIdSync(1)!!.nextReview)
        assertEquals(2, db.reviewLearningDao().skills(1).size)
        assertEquals(ReviewMode.MEANING, ReviewTask.forItem(db.vocabularyDao().getByIdSync(1)!!,
            db.reviewLearningDao().skills(1), now = now).mode)
    }

    @Test fun `early successful practice keeps memory date and repetition counts`() = runBlocking {
        store.record(task(), "buy", ReviewRating.GOOD, now)
        val before = db.reviewLearningDao().skills(1).single()
        store.record(task(), "buy", ReviewRating.EASY, now + 60_000)
        val after = db.reviewLearningDao().skills(1).single()
        assertEquals(before.copy(lastPractice = now + 60_000), after)
        assertEquals("EARLY", db.reviewLearningDao().recentEvents().first().category)
    }

    @Test fun `early forgetting updates memory and relearning starts from the observed lapse`() = runBlocking {
        store.record(task(), "buy", ReviewRating.EASY, now)
        val before = db.reviewLearningDao().skills(1).single()
        val failureTime = now + Fsrs6.DAY_MS
        store.record(task(), "", ReviewRating.AGAIN, failureTime)
        val failed = db.reviewLearningDao().skills(1).single()
        assertTrue(failed.stability < before.stability)
        assertEquals(failureTime, failed.lastReview)
        assertEquals(1, failed.incorrectCount)
        assertEquals(failureTime + 30 * 60_000L, failed.nextReview)
        assertEquals("EARLY", db.reviewLearningDao().recentEvents().first().category)
        assertEquals(1, db.vocabularyDao().getByIdSync(1)!!.incorrectCount)
        store.record(task(), "buy", ReviewRating.GOOD, failed.nextReview)
        assertEquals(0.0, db.reviewLearningDao().recentEvents().first().elapsedDays, 0.0)
    }

    @Test fun `an incompatible typed scheduler version initializes from a real grade`() = runBlocking {
        db.reviewLearningDao().saveSkill(VocabularySkillProgress(1, axis = "RETRIEVAL", stability = 200.0,
            difficulty = 9.0, lastReview = now - Fsrs6.DAY_MS, firstReview = now - 100 * Fsrs6.DAY_MS,
            nextReview = now, independentSuccesses = 50, schedulerVersion = "other-algorithm"))
        store.record(task(), "buy", ReviewRating.GOOD, now)
        val state = db.reviewLearningDao().skills(1).single()
        assertEquals(Fsrs6.VERSION, state.schedulerVersion)
        assertEquals(2.3065, state.stability, 1e-10)
        assertEquals(now, state.firstReview)
        assertEquals(1, state.independentSuccesses)
        assertNull(db.reviewLearningDao().recentEvents().single().prediction)
    }

    @Test fun `study-page judgements initialize memory without claiming independent recall`() = runBlocking {
        store.recordBase(item, ReviewRating.GOOD, now)
        val state = db.reviewLearningDao().skills(1).single()
        assertEquals("MEANING", state.axis)
        assertTrue(state.stability > 0)
        assertEquals(0, state.independentSuccesses)
        assertEquals("LEARNING", db.reviewLearningDao().recentEvents().single().source)
    }

    @Test fun `a hint records guided Hard practice without independent success`() = runBlocking {
        store.record(task(), "buy", ReviewRating.EASY, now, hintUsed = true)
        val state = db.reviewLearningDao().skills(1).single()
        assertEquals(1.2931, state.stability, 1e-10)
        assertEquals(0, state.independentSuccesses)
        val event = db.reviewLearningDao().recentEvents().single()
        assertTrue(event.hintUsed)
        assertEquals(2, event.rating)
    }

    @Test fun `verified synonym preserves target memory counts and due date`() = runBlocking {
        val alternate = ReviewTask(item, ReviewMode.SYNONYM, "buy or purchase?", "buy",
            acceptedAnswers = setOf("buy", "purchase"), senseKey = "lexical:buy-purchase:buy")
        store.record(alternate, "purchase", ReviewRating.GOOD, now)
        val after = db.vocabularyDao().getByIdSync(1)!!
        assertEquals(item.correctCount, after.correctCount)
        assertEquals(item.nextReview, after.nextReview)
        assertEquals(0.0, db.reviewLearningDao().skills(1).single().stability, 0.0)
        assertEquals("VERIFIED_ALTERNATIVE", db.reviewLearningDao().recentEvents().single().category)
    }

    @Test fun `retention preference is applied and evidence survives deleting a word`() = runBlocking {
        db.reviewLearningDao().saveSettings(VocabularyReviewSettings(desiredRetention = .95))
        store.record(task(), "buy", ReviewRating.EASY, now)
        val state = db.reviewLearningDao().skills(1).single()
        assertEquals(now + Fsrs6.interval(state.stability, .95) * Fsrs6.DAY_MS, state.nextReview)
        store.record(task(), "buy", ReviewRating.GOOD, state.nextReview)
        val events = db.reviewLearningDao().recentEvents()
        assertEquals(1, ReviewEvidence.summarize(events).samples)
        assertEquals(1.0, ReviewEvidence.summarize(events).recallRate!!, 0.0)
        db.vocabularyDao().deleteById(1)
        assertTrue(db.reviewLearningDao().skills(1).isEmpty())
        assertEquals(2, db.reviewLearningDao().recentEvents().size)
    }

    @Test fun `an unavailable lexical counterpart cannot hold the due date in the past`() = runBlocking {
        db.reviewLearningDao().saveSkill(VocabularySkillProgress(1, "lexical:buy-purchase:buy", "SYNONYM",
            stability = 2.0, difficulty = 5.0, lastReview = now - 2 * Fsrs6.DAY_MS, nextReview = now - 1))
        store.record(task(), "buy", ReviewRating.GOOD, now)
        val skills = db.reviewLearningDao().skills(1)
        assertEquals(skills.single { it.axis == "RETRIEVAL" }.nextReview, db.vocabularyDao().getByIdSync(1)!!.nextReview)
    }

    @Test fun `single axis success does not certify multi-skill mastery`() = runBlocking {
        var time = now
        repeat(10) {
            store.record(task(), "buy", ReviewRating.EASY, time)
            time = db.reviewLearningDao().skills(1).single().nextReview
        }
        assertTrue(db.vocabularyDao().getByIdSync(1)!!.mastery < 70)
    }
}
