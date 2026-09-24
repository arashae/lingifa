package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MistakeRecord
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack
import com.example.data.model.VocabularyPackItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_items ORDER BY id DESC")
    fun getAllVocabularies(): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE word LIKE '%' || :query || '%' OR persianMeaning LIKE '%' || :query || '%' ORDER BY word ASC")
    fun searchVocabularies(query: String): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE (correctCount > 0 OR incorrectCount > 0) AND nextReview <= :currentTime ORDER BY nextReview ASC")
    fun getDueVocabularies(currentTime: Long): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE isFavorite = 1 ORDER BY word ASC")
    fun getFavoriteVocabularies(): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE cefrLevel = :level ORDER BY CASE WHEN learningOrder > 0 THEN learningOrder ELSE 2147483647 END, CASE WHEN frequencyRank > 0 THEN frequencyRank ELSE 2147483647 END, word ASC")
    fun getByLevel(level: String): Flow<List<VocabularyItem>>

    @Query(
        """
        SELECT v.* FROM vocabulary_items AS v
        INNER JOIN vocabulary_pack_items AS membership
            ON membership.vocabularyId = v.id
        WHERE membership.packId = :packId
        ORDER BY v.word ASC
        """
    )
    fun getByPack(packId: String): Flow<List<VocabularyItem>>

    @Query("SELECT * FROM vocabulary_items WHERE id = :id")
    fun getById(id: Long): Flow<VocabularyItem?>

    @Query("SELECT * FROM vocabulary_items WHERE normalizedWord = :normalizedWord LIMIT 1")
    suspend fun getByNormalizedWord(normalizedWord: String): VocabularyItem?

    @Query("SELECT * FROM vocabulary_items WHERE word = :word LIMIT 1")
    suspend fun getByExactWord(word: String): VocabularyItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: VocabularyItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<VocabularyItem>): List<Long>

    @Update
    suspend fun update(item: VocabularyItem)

    @Delete
    suspend fun delete(item: VocabularyItem)

    @Query("DELETE FROM vocabulary_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM vocabulary_items")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary_items")
    suspend fun getCountSync(): Int

    @Query("SELECT * FROM vocabulary_items")
    suspend fun getAllVocabulariesSync(): List<VocabularyItem>

    @Query("SELECT COUNT(*) FROM vocabulary_items WHERE mastery >= 70")
    fun getLearnedCount(): Flow<Int>

    @Query("SELECT * FROM vocabulary_items WHERE mastery >= 70 ORDER BY word ASC")
    fun getLearnedVocabularies(): Flow<List<VocabularyItem>>
}

@Dao
interface VocabularyPackDao {
    @Query("SELECT * FROM vocabulary_packs ORDER BY titleFa ASC")
    fun getAllPacks(): Flow<List<VocabularyPack>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pack: VocabularyPack)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(packs: List<VocabularyPack>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIfMissing(packs: List<VocabularyPack>)

    @Update
    suspend fun update(pack: VocabularyPack)

    @Query("UPDATE vocabulary_packs SET installedWordCount = :count WHERE id = :packId")
    suspend fun updateInstalledWordCount(packId: String, count: Int)

    @Query("UPDATE vocabulary_packs SET installedWordCount = :count, isDownloaded = :isDownloaded WHERE id = :packId")
    suspend fun updateInstallState(packId: String, count: Int, isDownloaded: Boolean)

    @Query("SELECT * FROM vocabulary_packs WHERE id = :packId LIMIT 1")
    suspend fun getPackById(packId: String): VocabularyPack?

    @Query("SELECT COUNT(*) FROM vocabulary_packs")
    suspend fun getPackCount(): Int
}

@Dao
interface VocabularyPackItemDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: VocabularyPackItem)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<VocabularyPackItem>)

    @Query("DELETE FROM vocabulary_pack_items WHERE packId = :packId")
    suspend fun deleteByPack(packId: String)

    @Query("DELETE FROM vocabulary_pack_items WHERE vocabularyId = :vocabularyId")
    suspend fun deleteByVocabulary(vocabularyId: Long)

    @Query("SELECT packId FROM vocabulary_pack_items WHERE vocabularyId = :vocabularyId ORDER BY packId")
    fun getPackIdsForVocabulary(vocabularyId: Long): Flow<List<String>>

    @Query("SELECT packId FROM vocabulary_pack_items WHERE vocabularyId = :vocabularyId ORDER BY packId")
    suspend fun getPackIdsForVocabularySync(vocabularyId: Long): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM vocabulary_pack_items WHERE packId = :packId AND vocabularyId = :vocabularyId)")
    suspend fun hasMembership(packId: String, vocabularyId: Long): Boolean

    @Query("SELECT COUNT(*) FROM vocabulary_pack_items WHERE packId = :packId")
    suspend fun getPackItemCount(packId: String): Int
}

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistake_records ORDER BY createdAt DESC")
    fun getAllMistakes(): Flow<List<MistakeRecord>>

    @Query("SELECT * FROM mistake_records WHERE skillType = :skillType ORDER BY createdAt DESC")
    fun getBySkill(skillType: String): Flow<List<MistakeRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mistake: MistakeRecord): Long

    @Delete
    suspend fun delete(mistake: MistakeRecord)

    @Query("DELETE FROM mistake_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(mistake: MistakeRecord)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)
}
