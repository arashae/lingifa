package com.example.ui.screens.review

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPackItem
import com.example.srs.ReviewRating
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import kotlinx.coroutines.flow.first
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
class ReviewSubmissionStoreTest {
    private lateinit var db: AppDatabase
    private lateinit var store: ReviewSubmissionStore
    private val now = 1_700_000_000_000L

    @Before fun setup() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        store = ReviewSubmissionStore(db)
        db.userProfileDao().insertOrUpdate(UserProfile(id = 1, xp = 100))
    }
    @After fun close() = db.close()

    private suspend fun task(): ReviewTask {
        val item = VocabularyItem(id = 1, word = "retain", persianMeaning = "حفظ کردن",
            correctCount = 2, nextReview = now, lastReview = now - 86_400_000)
        db.vocabularyDao().insert(item)
        return ReviewTask(item, ReviewMode.WORD_RECALL, item.persianMeaning, item.word)
    }

    @Test fun `first card persists activity before the session finishes`() = runBlocking {
        val task = task()
        store.save(task, "retain", ReviewRating.GOOD, now, 1, 10)
        assertEquals(3, db.vocabularyDao().getByIdSync(1)!!.correctCount)
        assertEquals(110, db.userProfileDao().getProfileSync()!!.xp)
        assertEquals(1, db.dailyStreakDao().getAllRecordsSync().single().itemsPracticed)
    }

    @Test fun `failure in activity persistence rolls back word and mistake together`() = runBlocking {
        val task = task()
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER reject_activity BEFORE INSERT ON daily_streak_records
            BEGIN SELECT RAISE(ABORT, 'simulated storage failure'); END
        """)
        try {
            store.save(task, "", ReviewRating.AGAIN, now, 1, 2)
            fail("Expected storage failure")
        } catch (_: Exception) { }
        assertEquals(task.item, db.vocabularyDao().getByIdSync(1))
        assertTrue(db.mistakeDao().getAllMistakes().first().isEmpty())
        assertTrue(db.dailyStreakDao().getAllRecordsSync().isEmpty())
        assertEquals(100, db.userProfileDao().getProfileSync()!!.xp)
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_activity")
        store.save(task, "", ReviewRating.AGAIN, now, 1, 2)
        assertEquals(1, db.vocabularyDao().getByIdSync(1)!!.incorrectCount)
        assertEquals(1, db.mistakeDao().getAllMistakes().first().size)
        assertEquals(102, db.userProfileDao().getProfileSync()!!.xp)
    }

    @Test fun `alternative answer does not punish or certify target recall`() = runBlocking {
        val task = task()
        store.save(task, "keep", null, now, 1, 2)
        val after = db.vocabularyDao().getByIdSync(1)!!
        assertEquals(task.item.correctCount, after.correctCount)
        assertEquals(task.item.incorrectCount, after.incorrectCount)
        assertEquals(task.item.nextReview, after.nextReview)
        assertEquals(0, VocabularyStudyPolicy.skillMastery(after, VocabularySkillAxis.RETRIEVAL))
        assertTrue(db.mistakeDao().getAllMistakes().first().isEmpty())
    }

    @Test fun `fresh progress and unrelated edits survive submission from a queued snapshot`() = runBlocking {
        val task = task()
        db.vocabularyDao().update(task.item.copy(correctCount = 4, isFavorite = true, tags = listOf("custom")))
        store.save(task, "retain", ReviewRating.GOOD, now, 1, 10)
        val after = db.vocabularyDao().getByIdSync(1)!!
        assertEquals(5, after.correctCount)
        assertTrue(after.isFavorite)
        assertTrue("custom" in after.tags)
    }

    @Test fun `changed card content cannot be graded using an obsolete prompt`() = runBlocking {
        val task = task()
        db.vocabularyDao().update(task.item.copy(word = "release"))
        try {
            store.save(task, "retain", ReviewRating.GOOD, now, 1, 10)
            fail("Expected obsolete card rejection")
        } catch (_: IllegalArgumentException) { }
        assertEquals(2, db.vocabularyDao().getByIdSync(1)!!.correctCount)
        assertEquals(100, db.userProfileDao().getProfileSync()!!.xp)
    }

    @Test fun `learning and new filters partition attempted and unseen cards in library and packs`() = runBlocking {
        val items = listOf(
            VocabularyItem(id = 1, word = "unseen", persianMeaning = "جدید"),
            VocabularyItem(id = 2, word = "failed", persianMeaning = "خطا", mastery = 0, incorrectCount = 1),
            VocabularyItem(id = 3, word = "immature", persianMeaning = "نوپا", mastery = 80, correctCount = 2, intervalDays = 2),
            VocabularyItem(id = 4, word = "mastered", persianMeaning = "مسلط", mastery = 80, correctCount = 5, intervalDays = 10)
        )
        db.vocabularyDao().insertAll(items)
        db.vocabularyPackItemDao().insertAll(items.map { VocabularyPackItem(packId = "test", vocabularyId = it.id) })
        for ((status, ids) in listOf("New" to listOf(1L), "Learning" to listOf(2L, 3L), "Mastered" to listOf(4L))) {
            assertEquals(ids, db.vocabularyDao().getFilteredVocabularies("", "All", status, now).first().map { it.id }.sorted())
            assertEquals(ids, db.vocabularyDao().getFilteredVocabulariesByPack("test", "", "All", status, now).first().map { it.id }.sorted())
        }
    }
}
