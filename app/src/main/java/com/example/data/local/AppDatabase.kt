package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.importer.BundledVocabularyImporter
import com.example.data.model.DailyStreakRecord
import com.example.data.model.ExamTrackSettingsRecord
import com.example.data.model.ExamWordProgressRecord
import com.example.data.model.IeltsFlashcard
import com.example.data.model.IeltsSpeakingSessionRecord
import com.example.data.model.IeltsVocabularyDeck
import com.example.data.model.MistakeRecord
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyDatasetChunk
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack
import com.example.data.model.VocabularyPackItem
import com.example.data.seed.IeltsDeckSeed
import com.example.data.seed.InitialDataSeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Database(
    entities = [
        VocabularyItem::class,
        VocabularyPack::class,
        VocabularyPackItem::class,
        VocabularyDatasetChunk::class,
        MistakeRecord::class,
        UserProfile::class,
        IeltsVocabularyDeck::class,
        IeltsFlashcard::class,
        DailyStreakRecord::class,
        IeltsSpeakingSessionRecord::class,
        ExamWordProgressRecord::class,
        ExamTrackSettingsRecord::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun vocabularyPackDao(): VocabularyPackDao
    abstract fun vocabularyPackItemDao(): VocabularyPackItemDao
    abstract fun vocabularyDatasetChunkDao(): VocabularyDatasetChunkDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun ieltsFlashcardDao(): IeltsFlashcardDao
    abstract fun dailyStreakDao(): DailyStreakDao
    abstract fun ieltsSpeakingDao(): IeltsSpeakingDao
    abstract fun examTrackDao(): ExamTrackDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Preserve existing vocabulary/progress while upgrading the old one-pack-per-word
         * model to a many-to-many vocabulary catalog. Older migrations may still use the
         * existing destructive fallback, but the common v5 -> v6 upgrade is non-destructive.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary_items ADD COLUMN greRelevance TEXT NOT NULL DEFAULT 'Medium'")
                db.execSQL("ALTER TABLE vocabulary_items ADD COLUMN sourceLicense TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE vocabulary_items ADD COLUMN datasetVersion TEXT NOT NULL DEFAULT '1'")
                db.execSQL("ALTER TABLE vocabulary_items ADD COLUMN frequencyRank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vocabulary_items ADD COLUMN examPriority INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE vocabulary_packs ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE vocabulary_packs ADD COLUMN source TEXT NOT NULL DEFAULT 'LinguaFa'")
                db.execSQL("ALTER TABLE vocabulary_packs ADD COLUMN targetWordCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vocabulary_packs ADD COLUMN installedWordCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE vocabulary_packs ADD COLUMN isCorePack INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vocabulary_pack_items (
                        packId TEXT NOT NULL,
                        vocabularyId INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(packId, vocabularyId)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_vocabulary_pack_items_vocabularyId " +
                        "ON vocabulary_pack_items(vocabularyId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vocabulary_dataset_chunks (
                        chunkId TEXT NOT NULL,
                        packId TEXT NOT NULL,
                        version TEXT NOT NULL,
                        itemCount INTEGER NOT NULL,
                        importedAt INTEGER NOT NULL,
                        PRIMARY KEY(chunkId)
                    )
                    """.trimIndent()
                )

                // Preserve every legacy pack assignment before new master memberships are added.
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO vocabulary_pack_items(packId, vocabularyId, addedAt)
                    SELECT packName, id, updatedAt
                    FROM vocabulary_items
                    WHERE packName IS NOT NULL AND packName != ''
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE vocabulary_items ADD COLUMN learningOrder INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "linguafa_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope, appContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope,
            private val appContext: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Initial population is intentionally centralized in onOpen to avoid
                // concurrent duplicate seed jobs on a brand-new database.
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        ensureVocabularyCatalog(database)

                        if (database.vocabularyDao().getCountSync() == 0) {
                            populateDatabase(database)
                        } else {
                            ensureSeedMemberships(database)
                            ensureDefaultProfile(database)

                            if (database.ieltsFlashcardDao().getDeckCountSync() == 0) {
                                populateIeltsDecks(database)
                            }
                            if (database.dailyStreakDao().getTotalDaysCountSync() == 0) {
                                populateStreakRecords(database)
                            }
                        }

                        BundledVocabularyImporter.importBundledCatalog(
                            context = appContext,
                            vocabularyDao = database.vocabularyDao(),
                            packItemDao = database.vocabularyPackItemDao(),
                            chunkDao = database.vocabularyDatasetChunkDao()
                        )
                        ensureCefrMemberships(database)
                        refreshInstalledCounts(database)
                    }
                }
            }

            private suspend fun ensureDefaultProfile(database: AppDatabase) {
                val profileDao = database.userProfileDao()
                if (profileDao.getProfileSync() == null) {
                    profileDao.insertOrUpdate(UserProfile())
                }
            }

            private suspend fun ensureVocabularyCatalog(database: AppDatabase) {
                database.vocabularyPackDao().insertAllIfMissing(InitialDataSeed.getDefaultPacks())
            }

            private suspend fun populateDatabase(database: AppDatabase) {
                val packDao = database.vocabularyPackDao()
                val vocabDao = database.vocabularyDao()

                ensureDefaultProfile(database)

                // Insert built-in pack metadata, including IELTS/TOEFL/GRE master banks.
                packDao.insertAllIfMissing(InitialDataSeed.getDefaultPacks())

                // Insert bootstrap vocabulary and build many-to-many memberships.
                val seedItems = InitialDataSeed.getSeedVocabulary()
                val insertedIds = vocabDao.insertAll(seedItems)
                val memberships = buildMemberships(seedItems, insertedIds)
                database.vocabularyPackItemDao().insertAll(memberships)
                refreshInstalledCounts(database)

                populateIeltsDecks(database)
                populateStreakRecords(database)
            }

            /**
             * Existing v5 installs already have the 350-word bootstrap. After migration,
             * attach those rows to the new master banks without duplicating vocabulary rows.
             */
            private suspend fun ensureSeedMemberships(database: AppDatabase) {
                val membershipDao = database.vocabularyPackItemDao()
                val needsBackfill =
                    membershipDao.getPackItemCount(InitialDataSeed.IELTS_MASTER_PACK_ID) == 0 &&
                    membershipDao.getPackItemCount(InitialDataSeed.TOEFL_MASTER_PACK_ID) == 0 &&
                    membershipDao.getPackItemCount(InitialDataSeed.GRE_MASTER_PACK_ID) == 0

                if (!needsBackfill) {
                    refreshInstalledCounts(database)
                    return
                }

                val memberships = mutableListOf<VocabularyPackItem>()
                for (seedItem in InitialDataSeed.getSeedVocabulary()) {
                    val stored = database.vocabularyDao().getByNormalizedWord(seedItem.normalizedWord) ?: continue
                    InitialDataSeed.getPackIdsFor(seedItem).forEach { packId ->
                        memberships += VocabularyPackItem(packId = packId, vocabularyId = stored.id)
                    }
                }
                membershipDao.insertAll(memberships)
                refreshInstalledCounts(database)
            }

            /** Attach every existing and newly imported word to its general CEFR path. */
            private suspend fun ensureCefrMemberships(database: AppDatabase) {
                val memberships = database.vocabularyDao().getAllVocabulariesSync().flatMap { item ->
                    val id = item.id
                    InitialDataSeed.getPackIdsFor(item)
                        .filter { it.startsWith("pack_cefr_") }
                        .map { packId -> VocabularyPackItem(packId = packId, vocabularyId = id) }
                }
                database.vocabularyPackItemDao().insertAll(memberships)
            }

            private fun buildMemberships(
                items: List<VocabularyItem>,
                ids: List<Long>
            ): List<VocabularyPackItem> {
                return items.zip(ids).flatMap { (item, id) ->
                    InitialDataSeed.getPackIdsFor(item).map { packId ->
                        VocabularyPackItem(packId = packId, vocabularyId = id)
                    }
                }
            }

            private suspend fun refreshInstalledCounts(database: AppDatabase) {
                val packDao = database.vocabularyPackDao()
                val membershipDao = database.vocabularyPackItemDao()
                for (pack in InitialDataSeed.getDefaultPacks()) {
                    packDao.updateInstalledWordCount(
                        packId = pack.id,
                        count = membershipDao.getPackItemCount(pack.id)
                    )
                }
            }

            private suspend fun populateIeltsDecks(database: AppDatabase) {
                val ieltsDao = database.ieltsFlashcardDao()
                if (ieltsDao.getDeckCountSync() == 0) {
                    ieltsDao.insertDecks(IeltsDeckSeed.getDefaultDecks())
                    ieltsDao.insertCards(IeltsDeckSeed.getDefaultFlashcards())
                }
            }

            private suspend fun populateStreakRecords(database: AppDatabase) {
                val streakDao = database.dailyStreakDao()
                if (streakDao.getTotalDaysCountSync() == 0) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
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
