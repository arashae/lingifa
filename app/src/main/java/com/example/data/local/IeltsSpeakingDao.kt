package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.IeltsSpeakingSessionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface IeltsSpeakingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: IeltsSpeakingSessionRecord): Long

    @Query("SELECT * FROM ielts_speaking_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<IeltsSpeakingSessionRecord>>

    @Query("SELECT * FROM ielts_speaking_sessions ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<IeltsSpeakingSessionRecord>>

    @Query("SELECT * FROM ielts_speaking_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): IeltsSpeakingSessionRecord?

    @Query("SELECT COUNT(*) FROM ielts_speaking_sessions")
    fun getTotalSessionsCount(): Flow<Int>

    @Query("SELECT AVG(overallBand) FROM ielts_speaking_sessions")
    fun getAverageBand(): Flow<Float?>

    @Query("DELETE FROM ielts_speaking_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)
}
