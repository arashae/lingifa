package com.example.ui.screens.vocab

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.exporter.VocabularyExporter
import com.example.data.importer.DuplicateAction
import com.example.data.importer.ParsedImportItem
import com.example.data.importer.VocabularyFileParser
import com.example.data.local.AppDatabase
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPack
import com.example.data.repository.VocabularyRepository
import com.example.network.GeminiClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val isLoading: Boolean = false,
    val isAiGenerating: Boolean = false,
    val aiGeneratedPreview: List<ParsedImportItem> = emptyList(),
    val fileImportPreview: List<ParsedImportItem> = emptyList(),
    val statusMessage: String? = null
)

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

    private val wordsForSelectedPack: Flow<List<VocabularyItem>> =
        _selectedPackId.flatMapLatest { packId ->
            if (packId == null) repo.allVocabularies else repo.getByPack(packId)
        }

    val uiState: StateFlow<VocabLibraryUiState> = combine(
        wordsForSelectedPack,
        repo.allPacks,
        _searchQuery,
        _selectedLevel,
        _selectedStatus,
        _selectedPackId,
        _isAiGenerating,
        _aiPreview,
        _filePreview,
        _statusMessage
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val allWords = params[0] as List<VocabularyItem>
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

        val filtered = allWords.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.word.contains(query, ignoreCase = true) ||
                    item.persianMeaning.contains(query, ignoreCase = true)

            val matchesLevel = level == "همه" || item.cefrLevel.equals(level, ignoreCase = true)

            val matchesStatus = when (status) {
                "مرور امروز" -> item.nextReview <= System.currentTimeMillis()
                "یاد گرفته شده" -> item.mastery >= 70
                "در حال یادگیری" -> item.mastery in 1..69
                "جدید" -> item.mastery == 0
                "نشان‌شده‌ها" -> item.isFavorite
                else -> true
            }

            matchesQuery && matchesLevel && matchesStatus
        }

        VocabLibraryUiState(
            words = filtered,
            totalCount = allWords.size,
            learnedCount = allWords.count { it.mastery >= 70 },
            packs = packs,
            searchQuery = query,
            selectedLevel = level,
            selectedStatus = status,
            selectedPackId = packId,
            isAiGenerating = aiGen,
            aiGeneratedPreview = aiPreview,
            fileImportPreview = filePreview,
            statusMessage = statusMsg
        )
    }.stateIn(
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

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun getWordById(id: Long): Flow<VocabularyItem?> {
        return repo.getById(id)
    }

    fun toggleFavorite(item: VocabularyItem) {
        viewModelScope.launch {
            repo.update(item.copy(isFavorite = !item.isFavorite))
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
