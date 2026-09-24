package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyStreakRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyStreakRecord)

    @Update
    suspend fun update(record: DailyStreakRecord)

    @Query("SELECT * FROM daily_streak_records WHERE date = :date LIMIT 1")
    fun getRecordForDate(date: String): Flow<DailyStreakRecord?>

    @Query("SELECT * FROM daily_streak_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDateSync(date: String): DailyStreakRecord?

    @Query("SELECT * FROM daily_streak_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<DailyStreakRecord>>

    @Query("SELECT * FROM daily_streak_records ORDER BY date DESC")
    suspend fun getAllRecordsSync(): List<DailyStreakRecord>

    @Query("SELECT * FROM daily_streak_records ORDER BY date DESC LIMIT :limit")
    fun getRecentDays(limit: Int): Flow<List<DailyStreakRecord>>

    @Query("SELECT * FROM daily_streak_records ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDaysSync(limit: Int): List<DailyStreakRecord>

    @Query("SELECT COUNT(*) FROM daily_streak_records")
    fun getTotalDaysCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM daily_streak_records")
    suspend fun getTotalDaysCountSync(): Int

    @Query("SELECT SUM(xpEarned) FROM daily_streak_records")
    fun getTotalXp(): Flow<Int?>

    @Query("DELETE FROM daily_streak_records WHERE date = :date")
    suspend fun deleteRecord(date: String)
}
