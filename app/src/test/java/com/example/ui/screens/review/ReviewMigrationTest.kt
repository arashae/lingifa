package com.example.ui.screens.review

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReviewMigrationTest {
    @Test fun `version 11 upgrades with real Room schema validation and no invented history`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "review-migration-test.db"
        context.deleteDatabase(name)
        val item = VocabularyItem(id = 7, word = "retain", persianMeaning = "حفظ کردن",
            correctCount = 8, incorrectCount = 2, stability = 20f, intervalDays = 17,
            nextReview = 1_900_000_000_000L, lastReview = 1_800_000_000_000L,
            tags = listOf("custom", "__skill_retrieval:40"), isFavorite = true)
        val original = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        original.vocabularyDao().insert(item)
        original.vocabularySenseDao().insert(VocabularySense(vocabularyId = 7, englishDefinition = "keep something", isPrimary = true))
        original.vocabularyPackItemDao().insert(VocabularyPackItem("personal", 7, addedAt = 123))
        original.close()
        // Reconstruct the preceding schema: these are the only additions in v12.
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { sql ->
            sql.execSQL("DROP TABLE vocabulary_skill_progress")
            sql.execSQL("DROP TABLE vocabulary_review_events")
            sql.execSQL("DROP TABLE vocabulary_review_settings")
            sql.execSQL("ALTER TABLE vocabulary_items DROP COLUMN schedulerVersion")
            sql.execSQL("DROP TABLE room_master_table")
            sql.version = 11
        }
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_11_12).allowMainThreadQueries().build()
        try {
            assertEquals(item, upgraded.vocabularyDao().getByIdSync(7))
            assertEquals(1, upgraded.vocabularySenseDao().getSensesForWordSync(7).size)
            upgraded.openHelper.readableDatabase.query("SELECT addedAt FROM vocabulary_pack_items WHERE vocabularyId = 7").use {
                assertTrue(it.moveToFirst()); assertEquals(123L, it.getLong(0))
            }
            assertTrue(upgraded.reviewLearningDao().skills(7).isEmpty())
            assertTrue(upgraded.reviewLearningDao().recentEvents().isEmpty())
            assertEquals(.9, upgraded.reviewLearningDao().settings()!!.desiredRetention, 0.0)
            upgraded.reviewLearningDao().saveSkill(VocabularySkillProgress(7, axis = "RETRIEVAL"))
            assertEquals(1, upgraded.reviewLearningDao().skills(7).size)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
}
