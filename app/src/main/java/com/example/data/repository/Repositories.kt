package com.example.data.repository

import com.example.data.importer.DuplicateAction
import com.example.data.importer.ParsedImportItem
import com.example.data.local.MistakeDao
import com.example.data.local.UserProfileDao
import com.example.data.local.VocabularyDao
import com.example.data.local.VocabularyPackDao
import com.example.data.local.VocabularyPackItemDao
import com.example.data.model.MistakeRecord
import com.example.data.model.UserProfile
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack
import com.example.srs.ReviewRating
import com.example.srs.SpacedRepetitionSystem
import kotlinx.coroutines.flow.Flow

class VocabularyRepository(
    private val vocabDao: VocabularyDao,
    private val packDao: VocabularyPackDao,
    private val packItemDao: VocabularyPackItemDao? = null,
    private val senseDao: VocabularySenseDao? = null
) {
    val allVocabularies: Flow<List<VocabularyItem>> = vocabDao.getAllVocabularies()
    val totalCount: Flow<Int> = vocabDao.getCount()
    val learnedCount: Flow<Int> = vocabDao.getLearnedCount()
    val weakCount: Flow<Int> = vocabDao.getWeakCount()
    val allPacks: Flow<List<VocabularyPack>> = packDao.getAllPacks()

    fun getWeakCount(): Flow<Int> = vocabDao.getWeakCount()

    fun getWordWithSenses(id: Long): Flow<com.example.data.model.VocabularyWithSenses?> =
        vocabDao.getWordWithSenses(id)

    suspend fun getWordWithSensesSync(id: Long): com.example.data.model.VocabularyWithSenses? =
        vocabDao.getWordWithSensesSync(id)

    fun getSensesForWord(vocabularyId: Long): Flow<List<com.example.data.model.VocabularySense>> =
        senseDao?.getSensesForWord(vocabularyId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun saveSenses(vocabularyId: Long, senses: List<com.example.data.model.VocabularySense>) {
        senseDao?.let { dao ->
            dao.deleteSensesForWord(vocabularyId)
            dao.insertAll(senses)
        }
    }

    fun getDueVocabularies(currentTime: Long = System.currentTimeMillis()): Flow<List<VocabularyItem>> {
        return vocabDao.getDueVocabularies(currentTime)
    }

    fun getDueCount(currentTime: Long = System.currentTimeMillis()): Flow<Int> {
        return vocabDao.getDueCount(currentTime)
    }

    fun getDueVocabulariesForReview(limit: Int = 30, currentTime: Long = System.currentTimeMillis()): Flow<List<VocabularyItem>> {
        return vocabDao.getDueVocabulariesForReview(currentTime, limit)
    }

    suspend fun getRandomVocabularies(limit: Int): List<VocabularyItem> {
        return vocabDao.getRandomVocabularies(limit)
    }

    suspend fun getStudiedVocabulariesForReview(limit: Int): List<VocabularyItem> {
        return vocabDao.getStudiedVocabulariesForReview(limit)
    }

    fun search(query: String): Flow<List<VocabularyItem>> {
        return vocabDao.searchVocabularies(query)
    }

    fun getByLevel(level: String): Flow<List<VocabularyItem>> {
        return vocabDao.getByLevel(level)
    }

    fun getByPack(packName: String): Flow<List<VocabularyItem>> {
        return vocabDao.getByPack(packName)
    }

    fun getFavorites(): Flow<List<VocabularyItem>> {
        return vocabDao.getFavoriteVocabularies()
    }

    suspend fun getNewVocabulariesForLearning(level: String, limit: Int = 10): List<VocabularyItem> {
        return vocabDao.getNewVocabulariesForLearning(level, limit)
    }

    suspend fun getDistractors(level: String, partOfSpeech: String, excludeWord: String, limit: Int = 3): List<VocabularyItem> {
        val targeted = vocabDao.getDistractors(level, partOfSpeech, excludeWord, limit = 20).shuffled()
        if (targeted.size >= limit) return targeted.take(limit)
        val byLevel = vocabDao.getDistractorsByLevel(level, excludeWord, limit = 20).shuffled()
        val combined = (targeted + byLevel).distinctBy { it.word }.filter { it.word != excludeWord }
        if (combined.size >= limit) return combined.take(limit)
        return (combined + vocabDao.getRandomVocabularies(limit * 2))
            .filter { it.word != excludeWord }
            .distinctBy { it.word }
            .take(limit)
    }

    fun getFilteredVocabularies(
        query: String,
        level: String,
        status: String,
        packId: String? = null,
        currentTime: Long = System.currentTimeMillis(),
        limit: Int = 300
    ): Flow<List<VocabularyItem>> {
        return if (packId == null) {
            vocabDao.getFilteredVocabularies(query.trim(), level, status, currentTime, limit)
        } else {
            vocabDao.getFilteredVocabulariesByPack(packId, query.trim(), level, status, currentTime, limit)
        }
    }

    fun getById(id: Long): Flow<VocabularyItem?> {
        return vocabDao.getById(id)
    }

    suspend fun getByIdSync(id: Long): VocabularyItem? {
        return vocabDao.getByIdSync(id)
    }

    suspend fun insert(item: VocabularyItem): Long {
        return vocabDao.insert(item)
    }

    suspend fun insertAll(items: List<VocabularyItem>): List<Long> {
        return vocabDao.insertAll(items)
    }

    suspend fun update(item: VocabularyItem) {
        vocabDao.update(item)
    }

    suspend fun delete(item: VocabularyItem) {
        removePackMemberships(item.id)
        vocabDao.delete(item)
    }

    suspend fun deleteById(id: Long) {
        removePackMemberships(id)
        vocabDao.deleteById(id)
    }

    private suspend fun removePackMemberships(vocabularyId: Long) {
        val membershipDao = packItemDao ?: return
        val affectedPackIds = membershipDao.getPackIdsForVocabularySync(vocabularyId)
        membershipDao.deleteByVocabulary(vocabularyId)

        for (packId in affectedPackIds) {
            val count = membershipDao.getPackItemCount(packId)
            val pack = packDao.getPackById(packId)
            if (pack?.isCorePack == true) {
                val complete = pack.targetWordCount > 0 && count >= pack.targetWordCount
                packDao.updateInstallState(
                    packId = packId,
                    count = count,
                    isDownloaded = complete
                )
            } else {
                packDao.updateInstalledWordCount(packId = packId, count = count)
            }
        }
    }

    suspend fun recordReview(item: VocabularyItem, rating: ReviewRating, now: Long = System.currentTimeMillis()) {
        val result = SpacedRepetitionSystem.calculateNextReview(item, rating, now)
        val updated = item.copy(
            intervalDays = result.intervalDays,
            nextReview = result.nextReviewTimestamp,
            difficulty = result.newDifficulty,
            stability = result.newStability,
            mastery = result.newMastery,
            correctCount = result.correctCount,
            incorrectCount = result.incorrectCount,
            lastReview = now,
            updatedAt = now
        )
        vocabDao.update(updated)
    }

    suspend fun checkDuplicate(word: String): VocabularyItem? {
        val normalized = word.lowercase().trim()
        return vocabDao.getByNormalizedWord(normalized) ?: vocabDao.getByExactWord(word.trim())
    }

    suspend fun importItems(
        items: List<ParsedImportItem>,
        duplicateAction: DuplicateAction
    ): Int {
        var count = 0
        for (pItem in items) {
            if (!pItem.isSelected) continue
            val existing = checkDuplicate(pItem.word)

            if (existing != null) {
                when (duplicateAction) {
                    DuplicateAction.SKIP -> continue
                    DuplicateAction.UPDATE -> {
                        val updated = existing.copy(
                            persianMeaning = if (pItem.persianMeaning.isNotEmpty()) pItem.persianMeaning else existing.persianMeaning,
                            englishDefinition = if (pItem.englishDefinition.isNotEmpty()) pItem.englishDefinition else existing.englishDefinition,
                            example = if (pItem.example.isNotEmpty()) pItem.example else existing.example,
                            cefrLevel = if (pItem.cefrLevel.isNotEmpty()) pItem.cefrLevel else existing.cefrLevel,
                            tags = (existing.tags + pItem.tags).distinct(),
                            updatedAt = System.currentTimeMillis()
                        )
                        vocabDao.update(updated)
                        count++
                    }
                    DuplicateAction.MERGE -> {
                        val mergedMeaning = if (existing.persianMeaning.contains(pItem.persianMeaning)) {
                            existing.persianMeaning
                        } else if (pItem.persianMeaning.isNotEmpty()) {
                            "${existing.persianMeaning} / ${pItem.persianMeaning}"
                        } else existing.persianMeaning

                        val updated = existing.copy(
                            persianMeaning = mergedMeaning,
                            englishDefinition = if (existing.englishDefinition.isEmpty()) pItem.englishDefinition else existing.englishDefinition,
                            example = if (existing.example.isEmpty()) pItem.example else existing.example,
                            tags = (existing.tags + pItem.tags).distinct(),
                            updatedAt = System.currentTimeMillis()
                        )
                        vocabDao.update(updated)
                        count++
                    }
                    DuplicateAction.KEEP_BOTH -> {
                        val newItem = VocabularyItem(
                            id = 0L,
                            word = pItem.word,
                            normalizedWord = pItem.word.lowercase().trim(),
                            persianMeaning = pItem.persianMeaning,
                            englishDefinition = pItem.englishDefinition,
                            example = pItem.example,
                            cefrLevel = pItem.cefrLevel,
                            tags = pItem.tags,
                            source = "Import"
                        )
                        vocabDao.insert(newItem)
                        count++
                    }
                }
            } else {
                val newItem = VocabularyItem(
                    id = 0L,
                    word = pItem.word,
                    normalizedWord = pItem.word.lowercase().trim(),
                    persianMeaning = pItem.persianMeaning,
                    englishDefinition = pItem.englishDefinition,
                    example = pItem.example,
                    cefrLevel = pItem.cefrLevel,
                    tags = pItem.tags,
                    source = "Import"
                )
                vocabDao.insert(newItem)
                count++
            }
        }
        return count
    }
}

class MistakeRepository(private val mistakeDao: MistakeDao) {
    val allMistakes: Flow<List<MistakeRecord>> = mistakeDao.getAllMistakes()

    fun getBySkill(skill: String): Flow<List<MistakeRecord>> {
        return mistakeDao.getBySkill(skill)
    }

    suspend fun addMistake(
        question: String,
        myAnswer: String,
        correctAnswer: String,
        explanationFa: String,
        whyWrongFa: String = "",
        concept: String = "",
        skillType: String = "VOCABULARY"
    ): Long {
        return mistakeDao.insert(
            MistakeRecord(
                question = question,
                myAnswer = myAnswer,
                correctAnswer = correctAnswer,
                explanationFa = explanationFa,
                whyWrongFa = whyWrongFa,
                relatedConcept = concept,
                skillType = skillType
            )
        )
    }

    val unreviewedMistakes: Flow<List<MistakeRecord>> = mistakeDao.getUnreviewedMistakes()

    suspend fun markReviewed(id: Long, isReviewed: Boolean = true) {
        mistakeDao.updateReviewedStatus(id, isReviewed)
    }

    suspend fun deleteMistake(id: Long) {
        mistakeDao.deleteById(id)
    }

    suspend fun update(mistake: MistakeRecord) {
        mistakeDao.update(mistake)
    }
}

class UserProfileRepository(private val profileDao: UserProfileDao) {
    val profile: Flow<UserProfile?> = profileDao.getProfile()

    suspend fun getProfileSync(): UserProfile {
        return profileDao.getProfileSync() ?: UserProfile().also {
            profileDao.insertOrUpdate(it)
        }
    }

    suspend fun updateProfile(profile: UserProfile) {
        profileDao.insertOrUpdate(profile)
    }

    suspend fun addXp(amount: Int) {
        val current = getProfileSync()
        profileDao.insertOrUpdate(current.copy(xp = current.xp + amount))
    }
}
