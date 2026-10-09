package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VocabularySkillProgress
import com.example.data.model.VocabularyReviewEvent
import com.example.data.model.VocabularyReviewSettings

@Dao
interface ReviewLearningDao {
    @Query("SELECT * FROM vocabulary_skill_progress WHERE vocabularyId = :id")
    suspend fun skills(id: Long): List<VocabularySkillProgress>
    @Query("SELECT * FROM vocabulary_skill_progress WHERE vocabularyId IN (:ids)")
    suspend fun skillsFor(ids: List<Long>): List<VocabularySkillProgress>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSkill(progress: VocabularySkillProgress)
    @Insert
    suspend fun appendEvent(event: VocabularyReviewEvent): Long
    @Query("SELECT * FROM vocabulary_review_events ORDER BY reviewedAt DESC, id DESC LIMIT :limit")
    suspend fun recentEvents(limit: Int = 1000): List<VocabularyReviewEvent>
    @Query("SELECT * FROM vocabulary_skill_progress WHERE lastReview > 0")
    suspend fun initializedSkills(): List<VocabularySkillProgress>
    @Query("SELECT COUNT(*) FROM vocabulary_review_events WHERE reviewedAt >= :since")
    suspend fun eventCountSince(since: Long): Int
    @Query("SELECT * FROM vocabulary_review_settings WHERE id = 1")
    suspend fun settings(): VocabularyReviewSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: VocabularyReviewSettings)
}
