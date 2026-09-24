package com.example.data.importer

import android.util.Log
import com.example.data.local.VocabularyDao
import com.example.data.local.VocabularyPackDao
import com.example.data.local.VocabularyPackItemDao
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularyPackItem
import com.example.data.seed.InitialDataSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Downloads and builds the large IELTS / TOEFL / GRE master banks.
 *
 * Source policy:
 *  1) Exam-focused core membership and rich Persian data come from Openjam (MIT).
 *  2) Openjam's general CEFR/frequency vocabulary expands each exam bank with reviewed Persian data.
 *  3) Remaining target gaps are filled from the Apache-2.0 VahidN English-Persian
 *     "essential-english-words-2" dictionary. Only clean single-word headwords are accepted.
 *
 * This deliberately treats 9k / 7k / 5k as LinguaFa curated coverage targets rather than
 * claiming that IELTS, ETS/TOEFL, or ETS/GRE publish an official closed vocabulary list.
 * Everything is deduplicated by normalized English headword and cached in Room for offline use.
 */
object RemoteMasterVocabularySync {
    private const val TAG = "RemoteMasterVocabSync"
    private const val OPENJAM_BASE = "https://openjam.amirj4m.com"
    private const val VAHID_BASE =
        "https://raw.githubusercontent.com/VahidN/EnglishToPersianDictionaries/master/Dictionaries/essential-english-words-2"
    private const val REMOTE_DATASET_VERSION = "2026.09"
    private const val PAGE_SIZE = 500

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    data class Progress(
        val packId: String,
        val installed: Int,
        val target: Int,
        val stage: String,
        val message: String
    ) {
        val fraction: Float
            get() = if (target <= 0) 0f else (installed.toFloat() / target).coerceIn(0f, 1f)
    }

    data class SyncResult(
        val packId: String,
        val installed: Int,
        val target: Int,
        val insertedWords: Int,
        val updatedWords: Int,
        val membershipsAdded: Int,
        val complete: Boolean,
        val warnings: List<String>
    )

    private data class PackSpec(
        val packId: String,
        val exam: String,
        val displayFa: String,
        val openjamBookSlug: String,
        val target: Int,
        val expansionLevels: List<String>,
        val defaultCefr: String
    )

    private data class Counters(
        var installed: Int,
        var inserted: Int = 0,
        var updated: Int = 0,
        var memberships: Int = 0
    )

    private data class SupplementalCandidate(
        val word: String,
        val meaning: String,
        val score: Int
    )

    private val specs = mapOf(
        InitialDataSeed.IELTS_MASTER_PACK_ID to PackSpec(
            packId = InitialDataSeed.IELTS_MASTER_PACK_ID,
            exam = "IELTS",
            displayFa = "IELTS",
            openjamBookSlug = "ielts",
            target = 9000,
            expansionLevels = listOf("B2", "C1", "B1", "C2", "A2"),
            defaultCefr = "B2"
        ),
        InitialDataSeed.TOEFL_MASTER_PACK_ID to PackSpec(
            packId = InitialDataSeed.TOEFL_MASTER_PACK_ID,
            exam = "TOEFL",
            displayFa = "TOEFL",
            openjamBookSlug = "toefl",
            target = 7000,
            expansionLevels = listOf("B2", "C1", "C2", "B1"),
            defaultCefr = "B2"
        ),
        InitialDataSeed.GRE_MASTER_PACK_ID to PackSpec(
            packId = InitialDataSeed.GRE_MASTER_PACK_ID,
            exam = "GRE",
            displayFa = "GRE",
            openjamBookSlug = "gre",
            target = 5000,
            expansionLevels = listOf("C2", "C1"),
            defaultCefr = "C1"
        )
    )

    suspend fun syncMasterPack(
        packId: String,
        vocabularyDao: VocabularyDao,
        packDao: VocabularyPackDao,
        packItemDao: VocabularyPackItemDao,
        onProgress: (Progress) -> Unit = {}
    ): SyncResult = withContext(Dispatchers.IO) {
        val spec = specs[packId]
            ?: throw IllegalArgumentException("Unknown master vocabulary pack: $packId")
        val warnings = mutableListOf<String>()
        val counters = Counters(installed = packItemDao.getPackItemCount(packId))

        fun report(stage: String, message: String) {
            onProgress(
                Progress(
                    packId = spec.packId,
                    installed = counters.installed,
                    target = spec.target,
                    stage = stage,
                    message = message
                )
            )
        }

        if (counters.installed >= spec.target) {
            packDao.updateInstallState(spec.packId, counters.installed, true)
            report("complete", "بانک ${spec.displayFa} از قبل کامل است.")
            return@withContext SyncResult(
                packId = spec.packId,
                installed = counters.installed,
                target = spec.target,
                insertedWords = 0,
                updatedWords = 0,
                membershipsAdded = 0,
                complete = true,
                warnings = emptyList()
            )
        }

        report("core", "در حال دریافت فهرست آزمون‌محور ${spec.displayFa}…")
        try {
            importOpenjamBook(spec, counters, vocabularyDao, packItemDao) { pageMessage ->
                report("core", pageMessage)
            }
        } catch (t: Throwable) {
            warnings += "Openjam ${spec.openjamBookSlug} core: ${t.message ?: t::class.java.simpleName}"
            Log.w(TAG, "Openjam core sync failed for ${spec.packId}", t)
        }
        refreshPackState(spec, counters, packDao)

        if (counters.installed < spec.target) {
            report("openjam", "در حال گسترش با واژگان CEFR و پرتکرار Openjam…")
            for (level in spec.expansionLevels) {
                if (counters.installed >= spec.target) break
                try {
                    importOpenjamLevel(
                        spec = spec,
                        level = level,
                        counters = counters,
                        vocabularyDao = vocabularyDao,
                        packItemDao = packItemDao
                    ) { pageMessage -> report("openjam", pageMessage) }
                } catch (t: Throwable) {
                    warnings += "Openjam level $level: ${t.message ?: t::class.java.simpleName}"
                    Log.w(TAG, "Openjam level sync failed for ${spec.packId}/$level", t)
                }
                refreshPackState(spec, counters, packDao)
            }
        }

        if (counters.installed < spec.target) {
            report("supplement", "در حال تکمیل شکاف بانک با دیکشنری باز انگلیسی–فارسی…")
            try {
                importVahidSupplement(
                    spec = spec,
                    counters = counters,
                    vocabularyDao = vocabularyDao,
                    packItemDao = packItemDao
                ) { message -> report("supplement", message) }
            } catch (t: Throwable) {
                warnings += "Apache supplemental dictionary: ${t.message ?: t::class.java.simpleName}"
                Log.w(TAG, "Supplemental sync failed for ${spec.packId}", t)
            }
        }

        counters.installed = packItemDao.getPackItemCount(spec.packId)
        val complete = counters.installed >= spec.target
        packDao.updateInstallState(spec.packId, counters.installed, complete)
        report(
            if (complete) "complete" else "partial",
            if (complete) {
                "بانک ${spec.displayFa} با ${counters.installed} واژه کامل و آفلاین شد."
            } else {
                "${counters.installed} از ${spec.target} واژه نصب شد؛ با اتصال اینترنت دوباره ادامه بده."
            }
        )

        SyncResult(
            packId = spec.packId,
            installed = counters.installed,
            target = spec.target,
            insertedWords = counters.inserted,
            updatedWords = counters.updated,
            membershipsAdded = counters.memberships,
            complete = complete,
            warnings = warnings
        )
    }

    private suspend fun importOpenjamBook(
        spec: PackSpec,
        counters: Counters,
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao,
        onPage: (String) -> Unit
    ) {
        var offset = 0
        while (counters.installed < spec.target) {
            val url = "$OPENJAM_BASE/v1/books/${spec.openjamBookSlug}/words" +
                "?lang=fa&limit=$PAGE_SIZE&offset=$offset"
            val root = JSONObject(httpGet(url))
            val data = root.optJSONArray("data") ?: JSONArray()
            if (data.length() == 0) break

            for (i in 0 until data.length()) {
                if (counters.installed >= spec.target) break
                val obj = data.optJSONObject(i) ?: continue
                val item = openjamBookItem(obj, spec) ?: continue
                upsertAndAttach(item, spec.packId, counters, vocabularyDao, packItemDao)
            }

            offset += data.length()
            onPage("هسته ${spec.displayFa}: ${counters.installed} / ${spec.target}")
            if (data.length() < PAGE_SIZE) break
        }
    }

    private suspend fun importOpenjamLevel(
        spec: PackSpec,
        level: String,
        counters: Counters,
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao,
        onPage: (String) -> Unit
    ) {
        var offset = 0
        while (counters.installed < spec.target) {
            val url = "$OPENJAM_BASE/v1/words?level=$level&lang=fa" +
                "&limit=$PAGE_SIZE&offset=$offset"
            val root = JSONObject(httpGet(url))
            val data = root.optJSONArray("data") ?: JSONArray()
            if (data.length() == 0) break

            for (i in 0 until data.length()) {
                if (counters.installed >= spec.target) break
                val obj = data.optJSONObject(i) ?: continue
                val item = openjamGeneralItem(obj, spec) ?: continue
                upsertAndAttach(item, spec.packId, counters, vocabularyDao, packItemDao)
            }

            offset += data.length()
            onPage("Openjam $level: ${counters.installed} / ${spec.target}")
            if (data.length() < PAGE_SIZE) break
        }
    }

    private suspend fun importVahidSupplement(
        spec: PackSpec,
        counters: Counters,
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao,
        onProgress: (String) -> Unit
    ) {
        val candidates = ArrayList<SupplementalCandidate>(18_000)
        for (letter in 'A'..'Z') {
            if (counters.installed >= spec.target) break
            try {
                val raw = httpGet("$VAHID_BASE/$letter.json").removePrefix("\uFEFF")
                val root = JSONObject(raw)
                val words = root.optJSONArray("Words") ?: JSONArray()
                for (i in 0 until words.length()) {
                    val entry = words.optJSONObject(i) ?: continue
                    val original = entry.optString("EnglishWord").trim()
                    val normalized = original.lowercase(Locale.US)
                    if (!isCleanSupplementalHeadword(original, normalized)) continue
                    if (!isCandidateForExam(spec.exam, normalized)) continue

                    val meanings = entry.optJSONArray("Meanings") ?: continue
                    val meaning = buildList {
                        for (m in 0 until minOf(meanings.length(), 2)) {
                            meanings.optString(m).trim().takeIf { it.isNotBlank() }?.let(::add)
                        }
                    }.joinToString("؛ ").trim()
                    if (meaning.isBlank()) continue

                    candidates += SupplementalCandidate(
                        word = normalized,
                        meaning = meaning,
                        score = supplementalScore(spec.exam, normalized)
                    )
                }
                onProgress("منبع تکمیلی $letter: ${candidates.size} نامزد بررسی شد")
            } catch (t: Throwable) {
                Log.w(TAG, "Could not read supplemental letter $letter", t)
            }
        }

        val ordered = candidates
            .distinctBy { it.word }
            .sortedWith(
                compareByDescending<SupplementalCandidate> { it.score }
                    .thenBy { stableTieBreaker(it.word) }
            )

        for ((index, candidate) in ordered.withIndex()) {
            if (counters.installed >= spec.target) break
            val item = VocabularyItem(
                word = candidate.word,
                normalizedWord = candidate.word,
                persianMeaning = candidate.meaning,
                cefrLevel = inferSupplementalCefr(spec.exam, candidate.word),
                ieltsRelevance = if (spec.exam == "IELTS") "High" else "Medium",
                toeflRelevance = if (spec.exam == "TOEFL") "High" else "Medium",
                greRelevance = if (spec.exam == "GRE") "High" else "Medium",
                tags = listOf(spec.exam, "Master Bank", "Downloaded", "Persian"),
                source = "VahidN EnglishToPersianDictionaries / LinguaFa curated",
                sourceLicense = "Apache-2.0",
                datasetVersion = REMOTE_DATASET_VERSION,
                examPriority = candidate.score.coerceIn(1, 100)
            )
            upsertAndAttach(item, spec.packId, counters, vocabularyDao, packItemDao)
            if (index % 250 == 0) {
                onProgress("تکمیل ${spec.displayFa}: ${counters.installed} / ${spec.target}")
            }
        }
    }

    private fun openjamBookItem(obj: JSONObject, spec: PackSpec): VocabularyItem? {
        val word = obj.optString("english").trim().lowercase(Locale.US)
        if (word.isBlank()) return null
        val translations = obj.optJSONArray("translations") ?: JSONArray()
        val translation = firstUsefulTranslation(translations) ?: return null
        val meaning = translation.optString("meaning").trim()
        if (meaning.isBlank()) return null

        val phonetics = obj.optJSONArray("phonetics") ?: JSONArray()
        val ipa = bestIpa(phonetics)
        val partOfSpeech = translation.optString("part_of_speech", "word").ifBlank { "word" }
        val group = obj.optString("group").trim()
        val sortOrder = obj.optInt("sort_order", 0)
        val priority = when {
            sortOrder <= 0 -> 90
            sortOrder <= 500 -> 100 - (sortOrder / 10).coerceAtMost(40)
            else -> 55
        }

        return VocabularyItem(
            word = word,
            normalizedWord = word,
            ipa = ipa,
            persianMeaning = collectMeanings(translations),
            partOfSpeech = partOfSpeech,
            examplePersian = translation.optString("example").trim(),
            cefrLevel = obj.optString("level", spec.defaultCefr).ifBlank { spec.defaultCefr },
            ieltsRelevance = if (spec.exam == "IELTS") "Very High" else "Medium",
            toeflRelevance = if (spec.exam == "TOEFL") "Very High" else "Medium",
            greRelevance = if (spec.exam == "GRE") "Very High" else "Medium",
            tags = buildList {
                add(spec.exam)
                add("Master Bank")
                add("Openjam")
                if (group.isNotBlank()) add(group)
            },
            source = "Openjam ${spec.openjamBookSlug} book list",
            sourceLicense = "MIT",
            datasetVersion = REMOTE_DATASET_VERSION,
            frequencyRank = obj.optInt("frequency_rank", 0),
            examPriority = priority
        )
    }

    private fun openjamGeneralItem(obj: JSONObject, spec: PackSpec): VocabularyItem? {
        val word = obj.optString("english").trim().lowercase(Locale.US)
        if (word.isBlank()) return null
        val translations = obj.optJSONArray("translations") ?: JSONArray()
        val translation = firstUsefulTranslation(translations) ?: return null
        val meaning = translation.optString("meaning").trim()
        if (meaning.isBlank()) return null

        return VocabularyItem(
            word = word,
            normalizedWord = word,
            persianMeaning = collectMeanings(translations),
            englishDefinition = translation.optString("definition_en").trim(),
            partOfSpeech = translation.optString("part_of_speech", "word").ifBlank { "word" },
            examplePersian = translation.optString("example").trim(),
            cefrLevel = obj.optString("level", spec.defaultCefr).ifBlank { spec.defaultCefr },
            ieltsRelevance = if (spec.exam == "IELTS") "High" else "Medium",
            toeflRelevance = if (spec.exam == "TOEFL") "High" else "Medium",
            greRelevance = if (spec.exam == "GRE") "High" else "Medium",
            tags = listOf(spec.exam, "Master Bank", "Openjam", "CEFR"),
            source = "Openjam CEFR/frequency expansion",
            sourceLicense = "MIT",
            datasetVersion = REMOTE_DATASET_VERSION,
            frequencyRank = obj.optInt("frequency_rank", 0),
            examPriority = openjamPriority(obj.optInt("frequency_rank", 0), spec.exam)
        )
    }

    private suspend fun upsertAndAttach(
        incoming: VocabularyItem,
        packId: String,
        counters: Counters,
        vocabularyDao: VocabularyDao,
        packItemDao: VocabularyPackItemDao
    ) {
        val existing = vocabularyDao.getByNormalizedWord(incoming.normalizedWord)
        val vocabularyId = if (existing == null) {
            counters.inserted++
            vocabularyDao.insert(incoming)
        } else {
            counters.updated++
            vocabularyDao.update(mergeExisting(existing, incoming))
            existing.id
        }

        if (!packItemDao.hasMembership(packId, vocabularyId)) {
            packItemDao.insert(VocabularyPackItem(packId = packId, vocabularyId = vocabularyId))
            counters.memberships++
            counters.installed++
        }
    }

    private fun mergeExisting(existing: VocabularyItem, incoming: VocabularyItem): VocabularyItem {
        return existing.copy(
            ipa = existing.ipa.ifBlank { incoming.ipa },
            persianMeaning = existing.persianMeaning.ifBlank { incoming.persianMeaning },
            englishDefinition = existing.englishDefinition.ifBlank { incoming.englishDefinition },
            partOfSpeech = existing.partOfSpeech.takeUnless { it.isBlank() || it == "word" }
                ?: incoming.partOfSpeech,
            example = existing.example.ifBlank { incoming.example },
            examplePersian = existing.examplePersian.ifBlank { incoming.examplePersian },
            cefrLevel = chooseCefr(existing.cefrLevel, incoming.cefrLevel),
            synonyms = (existing.synonyms + incoming.synonyms).distinct(),
            antonyms = (existing.antonyms + incoming.antonyms).distinct(),
            collocations = (existing.collocations + incoming.collocations).distinct(),
            wordFamily = (existing.wordFamily + incoming.wordFamily).distinct(),
            commonMistakes = existing.commonMistakes.ifBlank { incoming.commonMistakes },
            ieltsRelevance = strongerRelevance(existing.ieltsRelevance, incoming.ieltsRelevance),
            toeflRelevance = strongerRelevance(existing.toeflRelevance, incoming.toeflRelevance),
            greRelevance = strongerRelevance(existing.greRelevance, incoming.greRelevance),
            tags = (existing.tags + incoming.tags).distinct(),
            sourceLicense = mergeSourceText(existing.sourceLicense, incoming.sourceLicense),
            datasetVersion = incoming.datasetVersion,
            frequencyRank = chooseFrequencyRank(existing.frequencyRank, incoming.frequencyRank),
            examPriority = maxOf(existing.examPriority, incoming.examPriority),
            updatedAt = System.currentTimeMillis()
        )
    }

    private suspend fun refreshPackState(
        spec: PackSpec,
        counters: Counters,
        packDao: VocabularyPackDao
    ) {
        packDao.updateInstallState(
            packId = spec.packId,
            count = counters.installed,
            isDownloaded = counters.installed >= spec.target
        )
    }

    private suspend fun httpGet(url: String): String {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "LinguaFa-Android/1.0")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        delay(1800L * (attempt + 1))
                        throw IOException("Rate limited by vocabulary source")
                    }
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} for $url")
                    }
                    return response.body?.string()
                        ?: throw IOException("Empty response from $url")
                }
            } catch (t: Throwable) {
                lastError = t
                if (attempt < 2) delay(500L * (attempt + 1))
            }
        }
        throw IOException("Vocabulary download failed: ${lastError?.message}", lastError)
    }

    private fun firstUsefulTranslation(array: JSONArray): JSONObject? {
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optString("meaning").isNotBlank()) return obj
        }
        return null
    }

    private fun collectMeanings(array: JSONArray): String {
        val meanings = linkedSetOf<String>()
        for (i in 0 until array.length()) {
            val meaning = array.optJSONObject(i)?.optString("meaning")?.trim().orEmpty()
            if (meaning.isNotBlank()) meanings += meaning
            if (meanings.size >= 3) break
        }
        return meanings.joinToString("؛ ")
    }

    private fun bestIpa(array: JSONArray): String {
        var fallback = ""
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val ipa = obj.optString("ipa").trim()
            if (ipa.isBlank()) continue
            if (fallback.isBlank()) fallback = ipa
            if (obj.optString("variant").equals("us", ignoreCase = true)) return ipa
        }
        return fallback
    }

    private fun isCleanSupplementalHeadword(original: String, normalized: String): Boolean {
        if (original.isBlank() || normalized.length !in 3..24) return false
        if (original.firstOrNull()?.isUpperCase() == true) return false
        return HEADWORD_REGEX.matches(normalized)
    }

    private fun isCandidateForExam(exam: String, word: String): Boolean {
        if (word in SUPPLEMENTAL_STOP_WORDS) return false
        return when (exam) {
            "GRE" -> word.length >= 6 && (
                word.length >= 8 ||
                    ADVANCED_SUFFIXES.any(word::endsWith) ||
                    ACADEMIC_SUFFIXES.any(word::endsWith)
                )
            "TOEFL" -> word.length >= 5
            else -> word.length >= 4
        }
    }

    private fun supplementalScore(exam: String, word: String): Int {
        var score = 35
        if (word.length in 5..12) score += 10
        if (ACADEMIC_SUFFIXES.any(word::endsWith)) score += 22
        if (ACADEMIC_PREFIXES.any(word::startsWith)) score += 8
        when (exam) {
            "GRE" -> {
                if (word.length >= 8) score += 18
                if (ADVANCED_SUFFIXES.any(word::endsWith)) score += 25
                if (word.length >= 11) score += 8
            }
            "TOEFL" -> {
                if (word.length in 6..14) score += 12
                if (SCIENCE_SUFFIXES.any(word::endsWith)) score += 10
            }
            "IELTS" -> {
                if (word.length in 4..11) score += 12
                if (word.length > 15) score -= 8
            }
        }
        if ('-' in word || '\'' in word) score -= 5
        return score.coerceIn(1, 100)
    }

    private fun inferSupplementalCefr(exam: String, word: String): String = when (exam) {
        "GRE" -> if (word.length >= 10 || ADVANCED_SUFFIXES.any(word::endsWith)) "C2" else "C1"
        "TOEFL" -> if (ACADEMIC_SUFFIXES.any(word::endsWith) && word.length >= 9) "C1" else "B2"
        else -> if (ACADEMIC_SUFFIXES.any(word::endsWith) && word.length >= 9) "C1" else "B2"
    }

    private fun openjamPriority(frequencyRank: Int, exam: String): Int {
        val base = when {
            frequencyRank <= 0 -> 60
            frequencyRank <= 1000 -> 90
            frequencyRank <= 3000 -> 80
            frequencyRank <= 5000 -> 70
            else -> 60
        }
        return (base + if (exam == "GRE") 5 else 0).coerceAtMost(100)
    }

    private fun chooseCefr(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank()) return existing
        return existing
    }

    private fun chooseFrequencyRank(existing: Int, incoming: Int): Int = when {
        existing <= 0 -> incoming
        incoming <= 0 -> existing
        else -> minOf(existing, incoming)
    }

    private fun strongerRelevance(a: String, b: String): String {
        fun rank(value: String): Int = when (value.trim().lowercase(Locale.US)) {
            "essential", "very high" -> 4
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 0
        }
        return if (rank(b) > rank(a)) b else a
    }

    private fun mergeSourceText(a: String, b: String): String {
        if (a.isBlank()) return b
        if (b.isBlank() || a.contains(b, ignoreCase = true)) return a
        return "$a; $b"
    }

    private fun stableTieBreaker(word: String): Int = word.fold(17) { acc, c -> acc * 31 + c.code }

    private val HEADWORD_REGEX = Regex("^[a-z][a-z'-]*$")

    private val ACADEMIC_SUFFIXES = listOf(
        "tion", "sion", "ment", "ance", "ence", "ity", "ism", "ist", "ology",
        "graphy", "ative", "itive", "ous", "ive", "ize", "ise", "ify", "ical",
        "al", "ary", "ory", "able", "ible"
    )
    private val ADVANCED_SUFFIXES = listOf(
        "acious", "itious", "uous", "escent", "escence", "phile", "phobe", "cratic",
        "istic", "nomy", "pathy", "archy", "logy"
    )
    private val SCIENCE_SUFFIXES = listOf(
        "ology", "graphy", "metry", "scope", "genic", "chemical", "physics", "logical"
    )
    private val ACADEMIC_PREFIXES = listOf(
        "inter", "trans", "counter", "sub", "super", "multi", "poly", "anti", "meta",
        "macro", "micro", "pre", "post", "non"
    )
    private val SUPPLEMENTAL_STOP_WORDS = setOf(
        "about", "above", "after", "again", "against", "almost", "along", "already",
        "also", "although", "always", "among", "another", "around", "because", "before",
        "being", "below", "between", "both", "could", "every", "first", "found", "from",
        "have", "having", "here", "into", "itself", "just", "more", "most", "other",
        "over", "same", "should", "since", "some", "such", "than", "that", "their",
        "there", "these", "they", "this", "those", "through", "under", "until", "very",
        "were", "what", "when", "where", "which", "while", "with", "would", "your"
    )
}
