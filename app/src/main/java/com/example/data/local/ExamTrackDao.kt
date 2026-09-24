package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ExamTrackSettingsRecord
import com.example.data.model.ExamWordProgressRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamTrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWordProgress(progress: ExamWordProgressRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgressList(list: List<ExamWordProgressRecord>)

    @Query("SELECT * FROM exam_word_progress WHERE examTrack = :track")
    fun getProgressForTrack(track: String): Flow<List<ExamWordProgressRecord>>

    @Query("SELECT * FROM exam_word_progress WHERE examTrack = :track")
    suspend fun getProgressForTrackSync(track: String): List<ExamWordProgressRecord>

    @Query("SELECT * FROM exam_word_progress WHERE examTrack = :track AND wordId = :wordId LIMIT 1")
    suspend fun getWordProgress(track: String, wordId: String): ExamWordProgressRecord?

    @Query("SELECT COUNT(*) FROM exam_word_progress WHERE examTrack = :track AND isMastered = 1")
    fun getMasteredCountForTrack(track: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM exam_word_progress WHERE examTrack = :track AND isMastered = 1")
    suspend fun getMasteredCountForTrackSync(track: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: ExamTrackSettingsRecord)

    @Query("SELECT * FROM exam_track_settings WHERE examTrack = :track LIMIT 1")
    fun getSettingsForTrack(track: String): Flow<ExamTrackSettingsRecord?>

    @Query("SELECT * FROM exam_track_settings WHERE examTrack = :track LIMIT 1")
    suspend fun getSettingsForTrackSync(track: String): ExamTrackSettingsRecord?

    @Query("SELECT * FROM exam_track_settings")
    fun getAllTrackSettings(): Flow<List<ExamTrackSettingsRecord>>

    @Query("UPDATE exam_track_settings SET dailyGoalWords = :goal WHERE examTrack = :track")
    suspend fun updateDailyGoal(track: String, goal: Int)

    @Query("UPDATE exam_track_settings SET currentStageNumber = :stage WHERE examTrack = :track")
    suspend fun updateCurrentStage(track: String, stage: Int)

    @Query("DELETE FROM exam_word_progress WHERE examTrack = :track")
    suspend fun resetTrackProgress(track: String)
}
