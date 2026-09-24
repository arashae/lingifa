package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DailyStreakRecord
import com.example.data.model.IeltsFlashcard
import com.example.data.model.IeltsSpeakingSessionRecord
import com.example.data.model.IeltsVocabularyDeck
import com.example.data.model.MistakeRecord
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack
import com.example.data.seed.IeltsDeckSeed
import com.example.data.seed.InitialDataSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        VocabularyItem::class,
        VocabularyPack::class,
        MistakeRecord::class,
        UserProfile::class,
        IeltsVocabularyDeck::class,
        IeltsFlashcard::class,
        DailyStreakRecord::class,
        IeltsSpeakingSessionRecord::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun vocabularyPackDao(): VocabularyPackDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun ieltsFlashcardDao(): IeltsFlashcardDao
    abstract fun dailyStreakDao(): DailyStreakDao
    abstract fun ieltsSpeakingDao(): IeltsSpeakingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "linguafa_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Ensure initial seed runs if empty
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        if (database.vocabularyPackDao().getPackCount() == 0) {
                            populateDatabase(database)
                        } else {
                            if (database.ieltsFlashcardDao().getDeckCountSync() == 0) {
                                populateIeltsDecks(database)
                            }
                            if (database.dailyStreakDao().getTotalDaysCountSync() == 0) {
                                populateStreakRecords(database)
                            }
                        }
                    }
                }
            }

            private suspend fun populateDatabase(database: AppDatabase) {
                val packDao = database.vocabularyPackDao()
                val vocabDao = database.vocabularyDao()
                val profileDao = database.userProfileDao()

                // Insert User Profile default
                if (profileDao.getProfileSync() == null) {
                    profileDao.insertOrUpdate(UserProfile())
                }

                // Insert Packs
                packDao.insertAll(InitialDataSeed.getDefaultPacks())

                // Insert Vocabulary Seed
                vocabDao.insertAll(InitialDataSeed.getSeedVocabulary())

                // Insert IELTS Flashcard Decks and Cards
                populateIeltsDecks(database)

                // Insert Initial Daily Streak Records
                populateStreakRecords(database)
            }

            private suspend fun populateIeltsDecks(database: AppDatabase) {
                val ieltsDao = database.ieltsFlashcardDao()
                if (ieltsDao.getDeckCountSync() == 0) {
                    val defaultDecks = IeltsDeckSeed.getDefaultDecks()
                    ieltsDao.insertDecks(defaultDecks)
                    val defaultCards = IeltsDeckSeed.getDefaultFlashcards()
                    ieltsDao.insertCards(defaultCards)
                }
            }

            private suspend fun populateStreakRecords(database: AppDatabase) {
                val streakDao = database.dailyStreakDao()
                if (streakDao.getTotalDaysCountSync() == 0) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val cal = Calendar.getInstance()
                    val activities = listOf("VOCABULARY", "IELTS_FLASHCARDS", "REVIEW", "AI_CARD")

                    // Seed past 3 days + today (4-day active streak)
                    for (daysAgo in 3 downTo 0) {
                        val checkCal = Calendar.getInstance()
                        checkCal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                        val dateStr = dateFormat.format(checkCal.time)
                        val activity = activities[daysAgo % activities.size]
                        val items = 5 + (daysAgo * 3)
                        streakDao.insertOrUpdate(
                            DailyStreakRecord(
                                date = dateStr,
                                timestamp = checkCal.timeInMillis,
                                itemsPracticed = items,
                                minutesSpent = 15 + daysAgo * 5,
                                xpEarned = 25 + daysAgo * 10,
                                activityType = activity,
                                isGoalMet = true
                            )
                        )
                    }
                }
            }
        }
    }
}
