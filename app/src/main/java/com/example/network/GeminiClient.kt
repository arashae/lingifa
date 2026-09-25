package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.VocabularyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WritingEvaluationResult(
    val estimatedBand: String,
    val taskAchievementScore: String,
    val coherenceScore: String,
    val lexicalScore: String,
    val grammarScore: String,
    val overallFeedbackFa: String,
    val strengthsFa: List<String>,
    val mainIssuesFa: List<String>,
    val sentenceCorrections: List<SentenceCorrection>,
    val improvedVersion: String
)

data class SentenceCorrection(
    val original: String,
    val corrected: String,
    val explanationFa: String
)

data class SpeakingEvaluationResult(
    val estimatedBand: String,
    val fluencyFeedbackFa: String,
    val lexicalFeedbackFa: String,
    val grammarFeedbackFa: String,
    val pronunciationHintsFa: String,
    val betterPhrasings: List<String>
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    fun hasValidApiKey(): Boolean {
        if (AiPreferences.hasValidAiKey()) return true
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    /**
     * AI Vocabulary Generator: user prompt -> structured JSON array of VocabularyItem
     */
    suspend fun generateVocabularyList(userPrompt: String): Result<List<VocabularyItem>> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey()
        if (dsKey.isNotBlank()) {
            val res = DeepSeekClient.generateVocabularyList(dsKey, userPrompt, AiPreferences.getDeepSeekModel())
            if (res.isSuccess) return@withContext res
        }

        val apiKey = getApiKey()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(
                Exception("هوش مصنوعی پیکربندی نشده است. لطفاً کلید DeepSeek خود را در صفحه تنظیمات پروفایل وارد نمایید.")
            )
        }

        val systemInstruction = """
            You are a master English lexicographer and Persian linguist for IELTS and TOEFL.
            Generate a rich vocabulary list based on the user's prompt.
            The user prompt is in Persian or English.
            Return ONLY a valid JSON array where each object has these exact fields:
            - "word": string (lowercase)
            - "ipa": string (International Phonetic Alphabet)
            - "persianMeaning": string (concise, accurate Persian translation)
            - "englishDefinition": string (learner definition)
            - "partOfSpeech": string (noun, verb, adjective, adverb, or phrase)
            - "example": string (authentic academic example sentence)
            - "examplePersian": string (accurate Persian translation of example)
            - "cefrLevel": string ("A2", "B1", "B2", "C1", or "C2")
            - "synonyms": array of strings (2-4 synonyms)
            - "antonyms": array of strings (optional 1-3 antonyms)
            - "collocations": array of strings (2-4 strong collocations)
            - "wordFamily": array of strings
            - "commonMistakes": string (common pitfall for Persian speakers)
            - "ieltsRelevance": string
            - "toeflRelevance": string
            - "tags": array of strings
            Do not enclose in markdown code fences if possible, or return strictly valid JSON array.
        """.trimIndent()

        val promptJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "System instruction: $systemInstruction\n\nUser Request: $userPrompt"))
                    })
                })
            }
            put("contents", contents)

            val genConfig = JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            }
            put("generationConfig", genConfig)
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(promptJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                return@withContext Result.failure(Exception("خطای ارتباط با جمینای (${response.code})"))
            }

            val jsonResponse = JSONObject(body)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val rawText = parts?.optJSONObject(0)?.optString("text") ?: "[]"

            val cleanJson = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val array = if (cleanJson.startsWith("[")) JSONArray(cleanJson) else JSONObject(cleanJson).optJSONArray("vocabulary") ?: JSONArray()

            val items = mutableListOf<VocabularyItem>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val word = obj.optString("word", "").trim()
                if (word.isEmpty()) continue

                val syns = mutableListOf<String>()
                obj.optJSONArray("synonyms")?.let { arr ->
                    for (j in 0 until arr.length()) syns.add(arr.optString(j))
                }

                val collocations = mutableListOf<String>()
                obj.optJSONArray("collocations")?.let { arr ->
                    for (j in 0 until arr.length()) collocations.add(arr.optString(j))
                }

                val wordFamily = mutableListOf<String>()
                obj.optJSONArray("wordFamily")?.let { arr ->
                    for (j in 0 until arr.length()) wordFamily.add(arr.optString(j))
                }

                val tags = mutableListOf<String>()
                obj.optJSONArray("tags")?.let { arr ->
                    for (j in 0 until arr.length()) tags.add(arr.optString(j))
                }

                items.add(
                    VocabularyItem(
                        id = 0L,
                        word = word,
                        normalizedWord = word.lowercase().trim(),
                        ipa = obj.optString("ipa", ""),
                        persianMeaning = obj.optString("persianMeaning", ""),
                        englishDefinition = obj.optString("englishDefinition", ""),
                        partOfSpeech = obj.optString("partOfSpeech", "word"),
                        example = obj.optString("example", ""),
                        examplePersian = obj.optString("examplePersian", ""),
                        cefrLevel = obj.optString("cefrLevel", "B2"),
                        synonyms = syns,
                        collocations = collocations,
                        wordFamily = wordFamily,
                        commonMistakes = obj.optString("commonMistakes", ""),
                        ieltsRelevance = obj.optString("ieltsRelevance", "High"),
                        toeflRelevance = obj.optString("toeflRelevance", "High"),
                        tags = tags,
                        source = "Gemini AI"
                    )
                )
            }

            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Vocabulary generation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Automatically completes all details (IPA, Persian, Definition, Example, Collocations) for a single word
     */
    suspend fun enrichWord(word: String, optionalPersian: String = ""): Result<VocabularyItem> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey()
        if (dsKey.isNotBlank()) {
            val res = DeepSeekClient.enrichWord(dsKey, word, optionalPersian, AiPreferences.getDeepSeekModel())
            if (res.isSuccess) return@withContext res
        }

        val apiKey = getApiKey()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(Exception("هوش مصنوعی پیکربندی نشده است. لطفاً کلید DeepSeek خود را در صفحه تنظیمات پروفایل وارد نمایید."))
        }

        val prompt = "Word: '$word'. Optional Persian context: '$optionalPersian'. Return detailed lexicographical information."
        val result = generateVocabularyList(prompt)
        result.mapCatching { list ->
            list.firstOrNull() ?: throw Exception("اطلاعاتی برای کلمه یافت نشد.")
        }
    }

    /**
     * Persian AI Tutor Chat: guides the student, explains grammar & nuances in Persian
     */
    suspend fun askTutor(userQuestion: String, conversationContext: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey()
        if (dsKey.isNotBlank()) {
            val systemInstruction = """
                You are LinguaFa AI's elite English teacher for Persian speakers preparing for IELTS, TOEFL, and GRE.
                Respond primarily in fluent, polite, pedagogical Persian (فارسی روان و آموزشی).
                English words, example sentences, and grammar patterns should remain in clear English, with Persian explanations and translations.
                When answering questions about word differences (e.g. affect vs effect, economic vs economical), explain clearly with practical examples and common Iranian learner errors.
                Guide the student towards active recall and self-correction.
            """.trimIndent()
            val res = DeepSeekClient.chat(
                apiKey = dsKey,
                systemPrompt = systemInstruction,
                userMessage = if (conversationContext.isNotBlank()) "Context:\n$conversationContext\n\nStudent Question: $userQuestion" else userQuestion,
                model = AiPreferences.getDeepSeekModel()
            )
            if (res.isSuccess) return@withContext res
        }

        val apiKey = getApiKey()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(
                Exception("هوش مصنوعی پیکربندی نشده است. لطفاً کلید DeepSeek خود را در صفحه تنظیمات پروفایل وارد نمایید.")
            )
        }

        val systemInstruction = """
            You are LinguaFa AI's elite English teacher for Persian speakers preparing for IELTS and TOEFL.
            Respond primarily in fluent, polite, pedagogical Persian (فارسی روان و آموزشی).
            English words, example sentences, and grammar patterns should remain in clear English, with Persian explanations and translations.
            When answering questions about word differences (e.g. affect vs effect, economic vs economical), explain clearly with practical examples and common Iranian learner errors.
            Guide the student towards active recall and self-correction.
        """.trimIndent()

        val promptJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "$systemInstruction\n\nContext:\n$conversationContext\n\nStudent Question: $userQuestion"))
                    })
                })
            }
            put("contents", contents)
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(promptJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                return@withContext Result.failure(Exception("خطا در برقراری تماس (${response.code})"))
            }

            val jsonResponse = JSONObject(body)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: "پاسخی دریافت نشد."

            Result.success(text)
        } catch (e: Exception) {
            Log.e(TAG, "Tutor chat error", e)
            Result.failure(e)
        }
    }

    /**
     * IELTS Writing Evaluator: Analyzes Task 1 or 2 according to official 4 criteria
     */
    suspend fun evaluateEssay(taskPrompt: String, essayText: String): Result<WritingEvaluationResult> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey()
        if (dsKey.isNotBlank()) {
            val res = DeepSeekClient.evaluateEssay(dsKey, taskPrompt, essayText, AiPreferences.getDeepSeekModel())
            if (res.isSuccess) return@withContext res
        }

        val apiKey = getApiKey()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(
                Exception("ارزیابی هوش مصنوعی در دسترس نیست. لطفاً کلید DeepSeek خود را در صفحه تنظیمات پروفایل وارد نمایید.")
            )
        }

        val prompt = """
            Evaluate this IELTS essay according to official IELTS Writing band descriptors:
            1. Task Response / Task Achievement
            2. Coherence and Cohesion
            3. Lexical Resource
            4. Grammatical Range and Accuracy
            
            Prompt: $taskPrompt
            Essay: $essayText
            
            Provide explanations and feedback primarily in Persian (فارسی).
            Return ONLY a valid JSON object with:
            {
              "estimatedBand": "string (e.g. 6.5 - 7.0)",
              "taskAchievementScore": "string",
              "coherenceScore": "string",
              "lexicalScore": "string",
              "grammarScore": "string",
              "overallFeedbackFa": "string (in Persian)",
              "strengthsFa": ["string in Persian", ...],
              "mainIssuesFa": ["string in Persian", ...],
              "sentenceCorrections": [
                 {
                   "original": "string",
                   "corrected": "string",
                   "explanationFa": "string"
                 }
              ],
              "improvedVersion": "string (revised academic version in English)"
            }
        """.trimIndent()

        val promptJson = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(promptJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                return@withContext Result.failure(Exception("خطا در ارزیابی مقاله"))
            }

            val json = JSONObject(body)
            val raw = json.optJSONArray("candidates")?.optJSONObject(0)
                ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: "{}"

            val clean = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(clean)

            val strengths = mutableListOf<String>()
            obj.optJSONArray("strengthsFa")?.let { arr ->
                for (i in 0 until arr.length()) strengths.add(arr.optString(i))
            }

            val issues = mutableListOf<String>()
            obj.optJSONArray("mainIssuesFa")?.let { arr ->
                for (i in 0 until arr.length()) issues.add(arr.optString(i))
            }

            val corrections = mutableListOf<SentenceCorrection>()
            obj.optJSONArray("sentenceCorrections")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i) ?: continue
                    corrections.add(
                        SentenceCorrection(
                            original = c.optString("original", ""),
                            corrected = c.optString("corrected", ""),
                            explanationFa = c.optString("explanationFa", "")
                        )
                    )
                }
            }

            Result.success(
                WritingEvaluationResult(
                    estimatedBand = obj.optString("estimatedBand", "6.5"),
                    taskAchievementScore = obj.optString("taskAchievementScore", "6.5"),
                    coherenceScore = obj.optString("coherenceScore", "6.5"),
                    lexicalScore = obj.optString("lexicalScore", "6.5"),
                    grammarScore = obj.optString("grammarScore", "6.5"),
                    overallFeedbackFa = obj.optString("overallFeedbackFa", "مقاله بررسی شد."),
                    strengthsFa = strengths,
                    mainIssuesFa = issues,
                    sentenceCorrections = corrections,
                    improvedVersion = obj.optString("improvedVersion", essayText)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Essay evaluation error", e)
            Result.failure(e)
        }
    }
}
