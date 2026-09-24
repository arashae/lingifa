package com.example.ui.screens.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.exporter.VocabularyExporter
import com.example.data.importer.DuplicateAction
import com.example.data.importer.ParsedImportItem
import com.example.data.importer.RemoteMasterVocabularySync
import com.example.data.importer.VocabularyFileParser
import com.example.data.local.AppDatabase
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack
import com.example.data.model.UserProfile
import com.example.data.repository.VocabularyRepository
import com.example.network.GeminiClient
import com.example.srs.ReviewRating
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VocabLibraryUiState(
    val words: List<VocabularyItem> = emptyList(),
    val totalCount: Int = 0,
    val learnedCount: Int = 0,
    val packs: List<VocabularyPack> = emptyList(),
    val searchQuery: String = "",
    val selectedLevel: String = "همه",
    val selectedStatus: String = "همه",
    val selectedPackId: String? = null,
    val learningLevel: String = "B2",
    val isLoading: Boolean = false,
    val isAiGenerating: Boolean = false,
    val aiGeneratedPreview: List<ParsedImportItem> = emptyList(),
    val fileImportPreview: List<ParsedImportItem> = emptyList(),
    val statusMessage: String? = null
)

data class PackSyncUiState(
    val isRunning: Boolean = false,
    val installed: Int = 0,
    val target: Int = 0,
    val stage: String = "",
    val message: String = "",
    val warningCount: Int = 0
) {
    val progress: Float
        get() = if (target <= 0) 0f else (installed.toFloat() / target).coerceIn(0f, 1f)
}

@OptIn(ExperimentalCoroutinesApi::class)
class VocabViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repo = VocabularyRepository(
        vocabDao = db.vocabularyDao(),
        packDao = db.vocabularyPackDao(),
        packItemDao = db.vocabularyPackItemDao()
    )

    private val _searchQuery = MutableStateFlow("")
    private val _selectedLevel = MutableStateFlow("همه")
    private val _selectedStatus = MutableStateFlow("همه")
    private val _selectedPackId = MutableStateFlow<String?>(null)
    private val _isAiGenerating = MutableStateFlow(false)
    private val _aiPreview = MutableStateFlow<List<ParsedImportItem>>(emptyList())
    private val _filePreview = MutableStateFlow<List<ParsedImportItem>>(emptyList())
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _packSyncStates = MutableStateFlow<Map<String, PackSyncUiState>>(emptyMap())
    private val _activeMasterSyncPackId = MutableStateFlow<String?>(null)
    val packSyncStates: StateFlow<Map<String, PackSyncUiState>> = _packSyncStates.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredWordsFlow: Flow<List<VocabularyItem>> =
        combine(_searchQuery, _selectedLevel, _selectedStatus, _selectedPackId) { query, level, status, packId ->
            FilterParams(query, level, status, packId)
        }.flatMapLatest { params ->
            repo.getFilteredVocabularies(
                query = params.query,
                level = params.level,
                status = params.status,
                packId = params.packId,
                limit = 250
            )
        }.flowOn(kotlinx.coroutines.Dispatchers.Default)

    private data class FilterParams(
        val query: String,
        val level: String,
        val status: String,
        val packId: String?
    )

    val uiState: StateFlow<VocabLibraryUiState> = combine(
        filteredWordsFlow,
        repo.allPacks,
        _searchQuery,
        _selectedLevel,
        _selectedStatus,
        _selectedPackId,
        _isAiGenerating,
        _aiPreview,
        _filePreview,
        _statusMessage,
        repo.totalCount,
        repo.learnedCount,
        db.userProfileDao().getProfile()
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val words = params[0] as List<VocabularyItem>
        @Suppress("UNCHECKED_CAST")
        val packs = params[1] as List<VocabularyPack>
        val query = params[2] as String
        val level = params[3] as String
        val status = params[4] as String
        val packId = params[5] as String?
        val aiGen = params[6] as Boolean
        @Suppress("UNCHECKED_CAST")
        val aiPreview = params[7] as List<ParsedImportItem>
        @Suppress("UNCHECKED_CAST")
        val filePreview = params[8] as List<ParsedImportItem>
        val statusMsg = params[9] as String?
        val totalCount = params[10] as Int
        val learnedCount = params[11] as Int
        val profile = params[12] as UserProfile?

        VocabLibraryUiState(
            words = words,
            totalCount = totalCount,
            learnedCount = learnedCount,
            packs = packs,
            searchQuery = query,
            selectedLevel = level,
            selectedStatus = status,
            selectedPackId = packId,
            learningLevel = profile?.currentLevel ?: "B2",
            isAiGenerating = aiGen,
            aiGeneratedPreview = aiPreview,
            fileImportPreview = filePreview,
            statusMessage = statusMsg
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VocabLibraryUiState()
    )

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
    }

    fun onLevelFilterChanged(level: String) {
        _selectedLevel.value = level
    }

    fun onStatusFilterChanged(status: String) {
        _selectedStatus.value = status
    }

    fun onPackFilterChanged(packId: String?) {
        _selectedPackId.value = packId
    }

    /** Lets a learner start at any CEFR level, without forcing a placement test. */
    fun selectLearningLevel(level: String) {
        _selectedLevel.value = level
        viewModelScope.launch {
            val profileDao = db.userProfileDao()
            val current = profileDao.getProfileSync() ?: UserProfile()
            profileDao.insertOrUpdate(current.copy(currentLevel = level))
            _statusMessage.value = "مسیر یادگیری از سطح $level انتخاب شد."
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun syncMasterPack(packId: String) {
        if (_packSyncStates.value[packId]?.isRunning == true) return

        val activePackId = _activeMasterSyncPackId.value
        if (activePackId != null && activePackId != packId) {
            val activeTitle = uiState.value.packs.firstOrNull { it.id == activePackId }?.titleFa
                ?: "بانک فعلی"
            _statusMessage.value = "ابتدا دانلود «$activeTitle» را تمام کنید؛ برای جلوگیری از محدودیت سرور، بانک‌ها همزمان دانلود نمی‌شوند."
            return
        }

        _activeMasterSyncPackId.value = packId
        val pack = uiState.value.packs.firstOrNull { it.id == packId }
        updatePackSyncState(
            packId,
            PackSyncUiState(
                isRunning = true,
                installed = pack?.installedWordCount ?: 0,
                target = pack?.targetWordCount ?: 0,
                stage = "starting",
                message = "شروع آماده‌سازی بانک واژگان…"
            )
        )

        viewModelScope.launch {
            try {
                val result = RemoteMasterVocabularySync.syncMasterPack(
                    packId = packId,
                    vocabularyDao = db.vocabularyDao(),
                    packDao = db.vocabularyPackDao(),
                    packItemDao = db.vocabularyPackItemDao()
                ) { progress ->
                    updatePackSyncState(
                        packId,
                        PackSyncUiState(
                            isRunning = progress.stage != "complete" && progress.stage != "partial",
                            installed = progress.installed,
                            target = progress.target,
                            stage = progress.stage,
                            message = progress.message
                        )
                    )
                }

                updatePackSyncState(
                    packId,
                    PackSyncUiState(
                        isRunning = false,
                        installed = result.installed,
                        target = result.target,
                        stage = if (result.complete) "complete" else "partial",
                        message = if (result.complete) {
                            "بانک کامل شد و برای استفاده آفلاین آماده است."
                        } else {
                            "دانلود تا ${result.installed} واژه پیش رفت؛ برای ادامه دوباره بزن."
                        },
                        warningCount = result.warnings.size
                    )
                )
                _statusMessage.value = if (result.complete) {
                    "بانک واژگان با ${result.installed} واژه تکمیل شد."
                } else {
                    "${result.installed} از ${result.target} واژه ذخیره شد؛ دانلود قابل ادامه است."
                }
            } catch (t: Throwable) {
                val current = _packSyncStates.value[packId] ?: PackSyncUiState()
                updatePackSyncState(
                    packId,
                    current.copy(
                        isRunning = false,
                        stage = "error",
                        message = "دانلود متوقف شد: ${t.message ?: "خطای شبکه"}"
                    )
                )
                _statusMessage.value = "خطا در دانلود بانک واژگان: ${t.message ?: "ارتباط شبکه"}"
            } finally {
                if (_activeMasterSyncPackId.value == packId) {
                    _activeMasterSyncPackId.value = null
                }
            }
        }
    }

    private fun updatePackSyncState(packId: String, state: PackSyncUiState) {
        _packSyncStates.value = _packSyncStates.value.toMutableMap().apply {
            put(packId, state)
        }
    }

    fun getWordById(id: Long): Flow<VocabularyItem?> {
        return repo.getById(id)
    }

    private fun curriculumLevelRank(level: String): Int = when (level.trim().uppercase()) {
        "PRE-A1" -> 0
        "A1" -> 1
        "A2" -> 2
        "B1" -> 3
        "B2" -> 4
        "C1" -> 5
        "C2" -> 6
        else -> 99
    }

    fun toggleFavorite(item: VocabularyItem) {
        viewModelScope.launch {
            repo.update(item.copy(isFavorite = !item.isFavorite))
        }
    }

    /** Records the learner's first judgement so the word can enter SRS safely. */
    fun recordLearningJudgement(item: VocabularyItem, rating: ReviewRating) {
        viewModelScope.launch {
            repo.recordReview(item, rating)
        }
    }

    fun deleteWord(item: VocabularyItem) {
        viewModelScope.launch {
            repo.delete(item)
            _statusMessage.value = "لغت '${item.word}' حذف شد."
        }
    }

    fun addWordManually(
        word: String,
        meaning: String,
        definition: String = "",
        example: String = "",
        exampleFa: String = "",
        level: String = "B2",
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val existing = repo.checkDuplicate(word)
            if (existing != null) {
                _statusMessage.value = "این لغت قبلاً در لغات شما وجود داشته است!"
                return@launch
            }

            val newItem = VocabularyItem(
                word = word.trim(),
                persianMeaning = meaning.trim(),
                englishDefinition = definition.trim(),
                example = example.trim(),
                examplePersian = exampleFa.trim(),
                cefrLevel = level,
                tags = tags,
                source = "Manual"
            )
            repo.insert(newItem)
            _statusMessage.value = "لغت '${word.trim()}' با موفقیت افزوده شد."
        }
    }

    fun enrichWordWithAi(word: String, meaning: String, onResult: (VocabularyItem?) -> Unit) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            val result = GeminiClient.enrichWord(word, meaning)
            _isAiGenerating.value = false
            result.onSuccess {
                onResult(it)
                _statusMessage.value = "اطلاعات لغت توسط هوش مصنوعی تکمیل شد."
            }.onFailure { err ->
                onResult(null)
                _statusMessage.value = "خطا در هوش مصنوعی: ${err.message}"
            }
        }
    }

    fun generateVocabWithAi(prompt: String) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            val result = GeminiClient.generateVocabularyList(prompt)
            _isAiGenerating.value = false
            result.onSuccess { list ->
                val previewItems = list.map { item ->
                    ParsedImportItem(
                        word = item.word,
                        persianMeaning = item.persianMeaning,
                        englishDefinition = item.englishDefinition,
                        example = item.example,
                        cefrLevel = item.cefrLevel,
                        tags = item.tags,
                        isSelected = true
                    )
                }
                _aiPreview.value = previewItems
                _statusMessage.value = "${previewItems.size} لغت توسط هوش مصنوعی تولید شد. پیش‌نمایش را بررسی فرمایید."
            }.onFailure { err ->
                _statusMessage.value = "خطا در تولید لغات: ${err.message}"
            }
        }
    }

    fun toggleAiPreviewItem(index: Int) {
        val current = _aiPreview.value.toMutableList()
        if (index in current.indices) {
            val item = current[index]
            current[index] = item.copy(isSelected = !item.isSelected)
            _aiPreview.value = current
        }
    }

    fun selectAllAiPreview(select: Boolean) {
        val updated = _aiPreview.value.map { it.copy(isSelected = select) }
        _aiPreview.value = updated
    }

    fun confirmImportAiList(duplicateAction: DuplicateAction = DuplicateAction.MERGE) {
        viewModelScope.launch {
            val selected = _aiPreview.value.filter { it.isSelected }
            val count = repo.importItems(selected, duplicateAction)
            _aiPreview.value = emptyList()
            _statusMessage.value = "$count لغت با موفقیت وارد لغات شما شد."
        }
    }

    fun parseImportContent(content: String, format: String) {
        viewModelScope.launch {
            val parsed = when (format.uppercase()) {
                "CSV" -> VocabularyFileParser.parseCsv(content)
                "JSON" -> VocabularyFileParser.parseJson(content)
                "PASTE", "TXT" -> VocabularyFileParser.parsePlainText(content)
                else -> VocabularyFileParser.parseCsv(content)
            }

            for (p in parsed) {
                val existing = repo.checkDuplicate(p.word)
                if (existing != null) {
                    p.isDuplicate = true
                    p.existingItem = existing
                }
            }

            _filePreview.value = parsed
            _statusMessage.value = "${parsed.size} لغت استخراج شد. لطفاً پیش‌نمایش را بررسی کنید."
        }
    }

    fun toggleFilePreviewItem(index: Int) {
        val current = _filePreview.value.toMutableList()
        if (index in current.indices) {
            val item = current[index]
            current[index] = item.copy(isSelected = !item.isSelected)
            _filePreview.value = current
        }
    }

    fun selectAllFilePreview(select: Boolean) {
        val updated = _filePreview.value.map { it.copy(isSelected = select) }
        _filePreview.value = updated
    }

    fun confirmImportFileList(duplicateAction: DuplicateAction = DuplicateAction.MERGE) {
        viewModelScope.launch {
            val selected = _filePreview.value.filter { it.isSelected }
            val count = repo.importItems(selected, duplicateAction)
            _filePreview.value = emptyList()
            _statusMessage.value = "$count لغت با موفقیت ذخیره گردید."
        }
    }

    fun clearFilePreview() {
        _filePreview.value = emptyList()
    }

    fun clearAiPreview() {
        _aiPreview.value = emptyList()
    }

    suspend fun exportToCsv(): String {
        val all = uiState.value.words
        return VocabularyExporter.exportToCsv(all)
    }

    suspend fun exportToJson(): String {
        val all = uiState.value.words
        return VocabularyExporter.exportToJson(all)
    }
}
