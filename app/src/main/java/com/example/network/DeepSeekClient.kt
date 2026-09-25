package com.example.network

import android.util.Log
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

object DeepSeekClient {
    private const val TAG = "DeepSeekClient"
    private const val BASE_URL = "https://api.deepseek.com/chat/completions"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun testConnection(apiKey: String, model: String = "deepseek-chat"): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("DeepSeek API key is empty."))
        }
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "user")
                put("content", "Respond with 'Connected' if you receive this.")
            })
        }
        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("max_tokens", 10)
        }

        try {
            val request = Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                val errorMsg = try {
                    val errJson = JSONObject(body ?: "")
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("Connection failed: $errorMsg"))
            }

            val json = JSONObject(body)
            val reply = json.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content") ?: "Connected"
            Result.success(reply.trim())
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek test connection error", e)
            Result.failure(e)
        }
    }

    suspend fun chat(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        model: String = "deepseek-chat",
        temperature: Double = 0.7
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("DeepSeek API key is not configured."))
        }

        val messages = JSONArray().apply {
            if (systemPrompt.isNotBlank()) {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", temperature)
        }

        try {
            val request = Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body == null) {
                val errorMsg = try {
                    val errJson = JSONObject(body ?: "")
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("DeepSeek error ($errorMsg)"))
            }

            val json = JSONObject(body)
            val content = json.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.optString("content") ?: ""
            Result.success(content.trim())
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek chat error", e)
            Result.failure(e)
        }
    }

    suspend fun generateVocabularyList(
        apiKey: String,
        userPrompt: String,
        model: String = "deepseek-chat"
    ): Result<List<VocabularyItem>> = withContext(Dispatchers.IO) {
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
            - "collocations": array of strings (2-4 strong verified academic collocations)
            - "wordFamily": array of strings
            - "commonMistakes": string (common pitfall for Persian speakers)
            - "ieltsRelevance": string ("High", "Medium", or "Low")
            - "toeflRelevance": string ("High", "Medium", or "Low")
            - "tags": array of strings
            Do not enclose in markdown code fences if possible, or return strictly valid JSON array.
        """.trimIndent()

        val chatResult = chat(
            apiKey = apiKey,
            systemPrompt = systemInstruction,
            userMessage = userPrompt,
            model = model,
            temperature = 0.3
        )

        chatResult.mapCatching { rawText ->
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
                        source = "DeepSeek AI"
                    )
                )
            }
            items
        }
    }

    suspend fun enrichWord(
        apiKey: String,
        word: String,
        optionalPersian: String = "",
        model: String = "deepseek-chat"
    ): Result<VocabularyItem> = withContext(Dispatchers.IO) {
        val prompt = "Word: '$word'. Optional Persian context: '$optionalPersian'. Return detailed lexicographical information."
        val result = generateVocabularyList(apiKey, prompt, model)
        result.mapCatching { list ->
            list.firstOrNull() ?: throw Exception("No details found for '$word'.")
        }
    }

    suspend fun evaluateEssay(
        apiKey: String,
        taskPrompt: String,
        essayText: String,
        model: String = "deepseek-chat"
    ): Result<WritingEvaluationResult> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            Evaluate this IELTS essay according to official IELTS Writing band descriptors:
            1. Task Response / Task Achievement
            2. Coherence and Cohesion
            3. Lexical Resource
            4. Grammatical Range and Accuracy
            
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

        val userPrompt = "Prompt: $taskPrompt\n\nEssay:\n$essayText"
        val chatResult = chat(apiKey, systemPrompt, userPrompt, model, temperature = 0.2)

        chatResult.mapCatching { rawText ->
            val clean = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
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

            WritingEvaluationResult(
                estimatedBand = obj.optString("estimatedBand", "N/A"),
                taskAchievementScore = obj.optString("taskAchievementScore", "N/A"),
                coherenceScore = obj.optString("coherenceScore", "N/A"),
                lexicalScore = obj.optString("lexicalScore", "N/A"),
                grammarScore = obj.optString("grammarScore", "N/A"),
                overallFeedbackFa = obj.optString("overallFeedbackFa", "ارزیابی انجام شد."),
                strengthsFa = strengths,
                mainIssuesFa = issues,
                sentenceCorrections = corrections,
                improvedVersion = obj.optString("improvedVersion", essayText)
            )
        }
    }

    suspend fun evaluateSpeaking(
        apiKey: String,
        taskPrompt: String,
        transcriptText: String,
        model: String = "deepseek-chat"
    ): Result<SpeakingEvaluationResult> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are an expert IELTS/TOEFL Speaking examiner and Persian coach.
            Evaluate the following spoken response transcript.
            
            Return ONLY a valid JSON object:
            {
              "estimatedBand": "string (e.g. 6.5)",
              "fluencyFeedbackFa": "string (Persian feedback on fluency, discourse markers)",
              "lexicalFeedbackFa": "string (Persian feedback on vocabulary range, idiomatic language)",
              "grammarFeedbackFa": "string (Persian feedback on grammatical accuracy, complex structures)",
              "pronunciationHintsFa": "string (Persian advice on intonation, stress patterns)",
              "betterPhrasings": ["string", "string", "string"]
            }
        """.trimIndent()

        val userPrompt = "Task: $taskPrompt\n\nSpoken Transcript: $transcriptText"
        val chatResult = chat(apiKey, systemPrompt, userPrompt, model, temperature = 0.2)

        chatResult.mapCatching { rawText ->
            val clean = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(clean)

            val phrasings = mutableListOf<String>()
            obj.optJSONArray("betterPhrasings")?.let { arr ->
                for (i in 0 until arr.length()) phrasings.add(arr.optString(i))
            }

            SpeakingEvaluationResult(
                estimatedBand = obj.optString("estimatedBand", "N/A"),
                fluencyFeedbackFa = obj.optString("fluencyFeedbackFa", ""),
                lexicalFeedbackFa = obj.optString("lexicalFeedbackFa", ""),
                grammarFeedbackFa = obj.optString("grammarFeedbackFa", ""),
                pronunciationHintsFa = obj.optString("pronunciationHintsFa", ""),
                betterPhrasings = phrasings
            )
        }
    }
}
