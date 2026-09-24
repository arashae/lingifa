package com.example.data.ai

import com.example.data.model.AiVocabCardData
import com.example.network.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AiVocabCardGenerator {

    private val PRELOADED_KNOWLEDGE_BASE = mapOf(
        "mitigate" to AiVocabCardData(
            word = "Mitigate",
            phonetic = "/ˈmɪt.ɪ.ɡeɪt/",
            partOfSpeech = "verb",
            persianTranslation = "کاهش دادن، تعدیل کردن، تسکین دادن",
            englishDefinition = "To make something less harmful, severe, or painful.",
            exampleSentenceEn = "Effective urban planning can mitigate the adverse effects of heavy traffic.",
            exampleSentenceFa = "برنامه‌ریزی شهری مؤثر می‌تواند اثرات نامطلوب ترافیک سنگین را کاهش دهد.",
            collocations = listOf("mitigate the risk", "mitigate the impact", "mitigate climate change", "mitigate damage"),
            synonyms = listOf("alleviate", "lessen", "diminish", "attenuate"),
            antonyms = listOf("exacerbate", "aggravate", "intensify"),
            cefrLevel = "C1",
            ieltsTipFa = "واژه‌ای بسیار پرکاربرد برای ارتقای نمره رایتینگ تسک ۲ در موضوعات زیست‌محیطی و اقتصادی.",
            persianCommonMistake = "با کلمه militate اشتباه گرفته نشود؛ حرف اضافه برای mitigate معمولاً لازم نیست."
        ),
        "ubiquitous" to AiVocabCardData(
            word = "Ubiquitous",
            phonetic = "/juːˈbɪk.wɪ.təs/",
            partOfSpeech = "adjective",
            persianTranslation = "همه‌جا حاضر، فراگیر، همه‌گیر",
            englishDefinition = "Present, appearing, or found everywhere.",
            exampleSentenceEn = "Smartphones have become ubiquitous in almost every corner of modern society.",
            exampleSentenceFa = "گوشی‌های هوشمند در تقریباً هر گوشه‌ای از جامعه مدرن فراگیر شده‌اند.",
            collocations = listOf("ubiquitous presence", "ubiquitous technology", "become ubiquitous"),
            synonyms = listOf("omnipresent", "pervasive", "universal"),
            antonyms = listOf("rare", "scarce", "isolated"),
            cefrLevel = "C1",
            ieltsTipFa = "جایگزین عالی برای عبارت‌های تکراری مثل 'everywhere' یا 'common' در اسپیکینگ و رایتینگ.",
            persianCommonMistake = "تلفظ حرف اول 'یو' است نه 'او'."
        ),
        "resilience" to AiVocabCardData(
            word = "Resilience",
            phonetic = "/rɪˈzɪl.jəns/",
            partOfSpeech = "noun",
            persianTranslation = "تاب‌آوری، انعطاف‌پذیری، توانایی بازیابی",
            englishDefinition = "The capacity to recover quickly from difficulties or adapt to change.",
            exampleSentenceEn = "The economic resilience of developing nations was tested during the pandemic.",
            exampleSentenceFa = "تاب‌آوری اقتصادی کشورهای در حال توسعه در طول همه‌گیری به چالش کشیده شد.",
            collocations = listOf("build resilience", "mental resilience", "economic resilience", "show resilience"),
            synonyms = listOf("flexibility", "toughness", "endurance", "adaptability"),
            antonyms = listOf("fragility", "vulnerability", "weakness"),
            cefrLevel = "B2",
            ieltsTipFa = "مفهومی کلیدی در رایتینگ تسک ۲ برای موضوعات روانشناسی، جامعه و اقتصاد.",
            persianCommonMistake = "صفت آن resilient است؛ این دو را به جای یکدیگر به کار نبرید."
        ),
        "pragmatic" to AiVocabCardData(
            word = "Pragmatic",
            phonetic = "/præɡˈmæt.ɪk/",
            partOfSpeech = "adjective",
            persianTranslation = "عمل‌گرایانه، واقع‌بینانه، کاربردی",
            englishDefinition = "Dealing with things sensibly and realistically based on practical rather than theoretical considerations.",
            exampleSentenceEn = "Policymakers need to adopt a pragmatic approach to resolve youth unemployment.",
            exampleSentenceFa = "سیاست‌گذاران باید رویکردی عمل‌گرایانه برای حل بیکاری جوانان در پیش بگیرند.",
            collocations = listOf("pragmatic approach", "pragmatic solution", "pragmatic view"),
            synonyms = listOf("practical", "realistic", "sensible", "down-to-earth"),
            antonyms = listOf("idealistic", "impractical", "unrealistic"),
            cefrLevel = "C1",
            ieltsTipFa = "کلمه‌ای بسیار شیک برای بیان راه‌حل‌ها در پاراگراف‌های بدنه رایتینگ تسک ۲.",
            persianCommonMistake = "قید آن pragmatically است."
        ),
        "meticulous" to AiVocabCardData(
            word = "Meticulous",
            phonetic = "/məˈtɪk.jə.ləs/",
            partOfSpeech = "adjective",
            persianTranslation = "بسیار دقیق، موشکافانه، وسواس‌گونه (مثبت)",
            englishDefinition = "Showing great attention to detail; very careful and precise.",
            exampleSentenceEn = "Academic research demands meticulous attention to statistical methodology.",
            exampleSentenceFa = "پژوهش‌های دانشگاهی نیازمند توجه موشکافانه به روش‌شناسی آماری است.",
            collocations = listOf("meticulous planning", "meticulous attention to detail", "meticulous research"),
            synonyms = listOf("conscientious", "punctilious", "thorough", "rigorous"),
            antonyms = listOf("careless", "sloppy", "negligent"),
            cefrLevel = "C1",
            ieltsTipFa = "مناسب برای توصیف ویژگی‌های فردی در اسپیکینگ پارت ۲ و متون دانشگاهی.",
            persianCommonMistake = "معمولاً با حرف اضافه about یا in به کار می‌رود."
        ),
        "paradigm" to AiVocabCardData(
            word = "Paradigm",
            phonetic = "/ˈpær.ə.daɪm/",
            partOfSpeech = "noun",
            persianTranslation = "پارادایم، الگوی مسلط، مدل فکری",
            englishDefinition = "A typical example or pattern of something; a model or framework.",
            exampleSentenceEn = "The widespread rise of generative AI represents a paradigm shift in education.",
            exampleSentenceFa = "گسترش روزافزون هوش مصنوعی مولد نمایانگر یک تغییر پارادایم در آموزش است.",
            collocations = listOf("paradigm shift", "dominant paradigm", "new paradigm"),
            synonyms = listOf("framework", "model", "archetype", "prototype"),
            antonyms = listOf("anomaly", "deviation"),
            cefrLevel = "C2",
            ieltsTipFa = "ترکیب 'paradigm shift' نمره Lexical Resource شما را در رایتینگ به‌شدت بالا می‌برد.",
            persianCommonMistake = "حرف 'g' در این کلمه تلفظ نمی‌شود: /ˈpær.ə.daɪm/."
        ),
        "ephemeral" to AiVocabCardData(
            word = "Ephemeral",
            phonetic = "/ɪˈfem.ər.əl/",
            partOfSpeech = "adjective",
            persianTranslation = "زودگذر، ناپایدار، کوتاه‌مدت",
            englishDefinition = "Lasting for a very short time; fleeting.",
            exampleSentenceEn = "Fame on social media is often ephemeral, fading within a few weeks.",
            exampleSentenceFa = "شهرت در شبکه‌های اجتماعی اغلب زودگذر است و طی چند هفته محو می‌شود.",
            collocations = listOf("ephemeral nature", "ephemeral trends", "ephemeral pleasures"),
            synonyms = listOf("transient", "fleeting", "evanescent", "short-lived"),
            antonyms = listOf("permanent", "enduring", "perpetual", "everlasting"),
            cefrLevel = "C2",
            ieltsTipFa = "برای مقایسه آثار موقت با تغییرات پایدار در اسپیکینگ پارت ۳ عالی است.",
            persianCommonMistake = "تلفظ با صدای 'ف' در هجای دوم انجام می‌شود."
        ),
        "exacerbate" to AiVocabCardData(
            word = "Exacerbate",
            phonetic = "/ɪɡˈzæs.ə.beɪt/",
            partOfSpeech = "verb",
            persianTranslation = "تشدید کردن، وخیم‌تر کردن، بدتر ساختن",
            englishDefinition = "To make a problem, bad situation, or negative feeling worse.",
            exampleSentenceEn = "Deforestation directly exacerbates global warming and soil erosion.",
            exampleSentenceFa = "جنگل‌زدایی مستقیماً گرمایش زمین و فرسایش خاک را تشدید می‌کند.",
            collocations = listOf("exacerbate the problem", "exacerbate tension", "exacerbate poverty"),
            synonyms = listOf("aggravate", "worsen", "compound", "inflame"),
            antonyms = listOf("alleviate", "ameliorate", "mitigate"),
            cefrLevel = "C1",
            ieltsTipFa = "متضاد مستقیم کلمه mitigate است؛ هر دو را در ذهن خود با هم جفت کنید.",
            persianCommonMistake = "تلفظ بخش اول با 'گز' است نه 'اکس'."
        ),
        "perseverance" to AiVocabCardData(
            word = "Perseverance",
            phonetic = "/ˌpɜː.sɪˈvɪə.rəns/",
            partOfSpeech = "noun",
            persianTranslation = "پشتکار، مداومت، سرسختی در رسیدن به هدف",
            englishDefinition = "Persistence in doing something despite difficulty or delay in achieving success.",
            exampleSentenceEn = "Mastering a foreign language requires daily perseverance and disciplined practice.",
            exampleSentenceFa = "تسلط بر زبان خارجی نیازمند پشتکار روزانه و تمرین منظم است.",
            collocations = listOf("show perseverance", "great perseverance", "through perseverance"),
            synonyms = listOf("persistence", "tenacity", "determination", "grit"),
            antonyms = listOf("apathy", "giving up", "hesitation"),
            cefrLevel = "B2",
            ieltsTipFa = "واژه‌ای درخشان برای داستان‌سرایی و شرح تجربیات فردی در اسپیکینگ پارت ۲.",
            persianCommonMistake = "فعل آن persevere است و معمولاً با حرف اضافه in می‌آید (persevere in)."
        )
    )

    /**
     * Generate or lookup an AI vocabulary card.
     * Tries live Gemini API first if available, then falls back seamlessly to rich preloaded dictionary.
     */
    suspend fun generateVocabularyCard(query: String): AiVocabCardData = withContext(Dispatchers.IO) {
        val cleanWord = query.trim().lowercase()

        // 1. Try preloaded high-yield dictionary for exact/partial match
        val matched = PRELOADED_KNOWLEDGE_BASE[cleanWord]
        if (matched != null) {
            return@withContext matched
        }

        // 2. Try Gemini API if key is available
        if (GeminiClient.hasValidApiKey()) {
            try {
                val aiResult = GeminiClient.enrichWord(cleanWord)
                if (aiResult.isSuccess) {
                    val item = aiResult.getOrNull()
                    if (item != null && item.word.isNotBlank()) {
                        return@withContext AiVocabCardData(
                            word = item.word.replaceFirstChar { it.uppercase() },
                            phonetic = item.ipa,
                            partOfSpeech = item.partOfSpeech,
                            persianTranslation = item.persianMeaning,
                            englishDefinition = item.englishDefinition,
                            exampleSentenceEn = item.example,
                            exampleSentenceFa = item.examplePersian,
                            collocations = item.collocations,
                            synonyms = item.synonyms,
                            antonyms = item.antonyms,
                            cefrLevel = item.cefrLevel,
                            ieltsTipFa = item.ieltsRelevance.ifEmpty { "مناسب برای افزایش نمره در رایتینگ و اسپیکینگ آیلتس." },
                            persianCommonMistake = item.commonMistakes,
                            isGeneratedByAi = true
                        )
                    }
                }
            } catch (e: Exception) {
                // fallback gracefully
            }
        }

        // 3. Fallback: Return unverified status instead of synthetic fake data
        val capitalized = cleanWord.replaceFirstChar { it.uppercase() }
        return AiVocabCardData(
            word = capitalized,
            phonetic = "",
            partOfSpeech = "",
            persianTranslation = "",
            englishDefinition = "",
            exampleSentenceEn = "",
            exampleSentenceFa = "",
            collocations = emptyList(),
            synonyms = emptyList(),
            antonyms = emptyList(),
            cefrLevel = "B2",
            ieltsTipFa = "",
            persianCommonMistake = "",
            isGeneratedByAi = false,
            isValid = false,
            errorMessage = "اطلاعات موثقی برای واژه «$capitalized» در منابع آفلاین یا آنلاین یافت نشد. لطفاً املای واژه را بررسی کنید یا اتصال اینترنت را چک کنید."
        )
    }

    fun getQuickInspirationWords(): List<String> = listOf(
        "Mitigate", "Ubiquitous", "Resilience", "Pragmatic",
        "Meticulous", "Paradigm", "Ephemeral", "Exacerbate", "Perseverance"
    )
}
