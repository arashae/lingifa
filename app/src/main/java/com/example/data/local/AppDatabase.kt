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
import com.example.data.model.VocabularySense
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
        VocabularySense::class,
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
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun vocabularySenseDao(): VocabularySenseDao
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

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_items_normalizedWord ON vocabulary_items(normalizedWord)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_items_nextReview ON vocabulary_items(nextReview)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_items_cefrLevel ON vocabulary_items(cefrLevel)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_items_mastery ON vocabulary_items(mastery)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_items_isFavorite ON vocabulary_items(isFavorite)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_items_word ON vocabulary_items(word)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exam_word_progress_track_mastered ON exam_word_progress(examTrack, isMastered)")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vocabulary_senses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        vocabularyId INTEGER NOT NULL,
                        senseIndex INTEGER NOT NULL DEFAULT 1,
                        partOfSpeech TEXT NOT NULL DEFAULT '',
                        cefrLevel TEXT NOT NULL DEFAULT '',
                        englishDefinition TEXT NOT NULL DEFAULT '',
                        persianMeaning TEXT NOT NULL DEFAULT '',
                        exampleSentence TEXT NOT NULL DEFAULT '',
                        exampleTranslation TEXT NOT NULL DEFAULT '',
                        collocations TEXT NOT NULL DEFAULT '[]',
                        isPrimary INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(vocabularyId) REFERENCES vocabulary_items(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_vocabulary_senses_vocabularyId ON vocabulary_senses(vocabularyId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_vocabulary_senses_vocabularyId_senseIndex ON vocabulary_senses(vocabularyId, senseIndex)")
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO vocabulary_senses (
                        vocabularyId, senseIndex, partOfSpeech, cefrLevel,
                        englishDefinition, persianMeaning, exampleSentence, exampleTranslation,
                        collocations, isPrimary
                    )
                    SELECT id, 1, partOfSpeech, cefrLevel, englishDefinition, persianMeaning, example, examplePersian, collocations, 1
                    FROM vocabulary_items
                    """.trimIndent()
                )
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
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
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
                        }

                        val summary = BundledVocabularyImporter.importBundledCatalog(
                            context = appContext,
                            database = database,
                            vocabularyDao = database.vocabularyDao(),
                            packItemDao = database.vocabularyPackItemDao(),
                            chunkDao = database.vocabularyDatasetChunkDao()
                        )
                        val needsCefrBackfill = database.vocabularyPackItemDao().getPackItemCount("pack_cefr_b2") == 0
                        if (summary.insertedWords > 0 || summary.updatedWords > 0 || needsCefrBackfill) {
                            ensureCefrMemberships(database)
                            refreshInstalledCounts(database)
                        }

                        ensureVocabularySenses(database)
                    }
                }
            }

            private suspend fun ensureVocabularySenses(database: AppDatabase) {
                val senseDao = database.vocabularySenseDao()
                if (senseDao.getSenseCount() == 0) {
                    val vocabDao = database.vocabularyDao()
                    val allWords = vocabDao.getAllVocabulariesSync()
                    if (allWords.isNotEmpty()) {
                        val senses = allWords.map { word ->
                            VocabularySense(
                                vocabularyId = word.id,
                                senseIndex = 1,
                                partOfSpeech = word.partOfSpeech,
                                cefrLevel = word.cefrLevel,
                                englishDefinition = word.englishDefinition,
                                persianMeaning = word.persianMeaning,
                                exampleSentence = word.example,
                                exampleTranslation = word.examplePersian,
                                collocations = word.collocations,
                                isPrimary = true
                            )
                        }
                        senseDao.insertAll(senses)
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
                val packDao = database.vocabularyPackDao()
                val packs = InitialDataSeed.getDefaultPacks()
                packDao.insertAllIfMissing(packs)

                // Refresh metadata for every built-in pack on app updates while preserving the
                // user's local install state. This fixes stale historical targets (for example
                // old IELTS/GRE 9k/5k metadata) without deleting vocabulary or study progress.
                packs.forEach { fresh ->
                    val existing = packDao.getPackById(fresh.id) ?: return@forEach
                    packDao.update(
                        fresh.copy(
                            installedWordCount = existing.installedWordCount,
                            isDownloaded = existing.isDownloaded
                        )
                    )
                }
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

            /** Attach every existing and newly imported word to its general CEFR path using instant SQLite batch copy. */
            private suspend fun ensureCefrMemberships(database: AppDatabase) {
                database.vocabularyPackItemDao().populateCefrMemberships()
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
                database.vocabularyPackDao().refreshAllInstalledCounts()
            }

            private suspend fun populateIeltsDecks(database: AppDatabase) {
                val ieltsDao = database.ieltsFlashcardDao()
                if (ieltsDao.getDeckCountSync() == 0) {
                    ieltsDao.insertDecks(IeltsDeckSeed.getDefaultDecks())
                    ieltsDao.insertCards(IeltsDeckSeed.getDefaultFlashcards())
                }
            }
        }
    }
}
