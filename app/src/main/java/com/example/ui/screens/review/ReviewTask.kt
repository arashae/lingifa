package com.example.ui.screens.review

import com.example.data.model.VocabularyItem
import com.example.vocab.VocabularySkillAxis
import com.example.vocab.VocabularyStudyPolicy
import com.example.vocab.ReviewPersistencePolicy
import java.util.Locale

enum class ReviewMode(
    val skill: VocabularySkillAxis,
    val instruction: String,
    val requiresTypedAnswer: Boolean
) {
    MEANING(VocabularySkillAxis.MEANING, "Recall the meaning", false),
    WORD_RECALL(VocabularySkillAxis.RETRIEVAL, "Write the English word", true),
    ENGLISH_DEFINITION(
        VocabularySkillAxis.ENGLISH_DEFINITION,
        "Write the word from its English definition",
        true
    ),
    CONTEXT(VocabularySkillAxis.CONTEXT, "Complete the sentence", true),
    SYNONYM(VocabularySkillAxis.SYNONYM, "Choose the word that fits", true)
}

data class ReviewTask(
    val item: VocabularyItem,
    val mode: ReviewMode,
    val prompt: String,
    val expectedAnswer: String,
    val answerNoteFa: String = ""
) {
    fun isCorrect(answer: String): Boolean =
        normalize(answer) == normalize(expectedAnswer)

    val allowsAlternative: Boolean get() = mode.requiresTypedAnswer && mode != ReviewMode.SYNONYM

    val targetHint: String get() = "Target: ${expectedAnswer.firstOrNull() ?: '?'}… (${expectedAnswer.length} characters)"

    companion object {
        private data class Contrast(
            val first: String,
            val second: String,
            val firstSentence: String,
            val secondSentence: String,
            val explanationFa: String
        )

        // Use hand-written, sense-specific contrasts. Raw dictionary synonym fields are not
        // answer keys: they can include near-neighbours, different parts of speech, or errors.
        private val reviewedContrasts = listOf(
            Contrast(
                first = "avoid",
                second = "prevent",
                firstSentence = "I left early to ____ the rush-hour traffic.",
                secondSentence = "The new safety checks may ____ serious accidents.",
                explanationFa = "avoid یعنی از چیزی دوری کردن؛ prevent یعنی جلوی رخ‌دادن چیزی را گرفتن."
            ),
            Contrast(
                first = "economic",
                second = "economical",
                firstSentence = "The report examines the country's ____ growth.",
                secondSentence = "This small car is cheap and ____ to run.",
                explanationFa = "economic یعنی مربوط به اقتصاد؛ economical یعنی کم‌هزینه و به‌صرفه."
            ),
            Contrast(
                first = "say",
                second = "tell",
                firstSentence = "What did the witness ____ about the accident?",
                secondSentence = "Please ____ me the truth.",
                explanationFa = "say معمولاً روی خودِ گفته تمرکز دارد؛ tell معمولاً شنونده یا چیزی را که گفته می‌شود هم می‌گیرد."
            ),
            Contrast(
                first = "borrow",
                second = "lend",
                firstSentence = "Could I ____ your pen for a minute?",
                secondSentence = "Can you ____ me your pen for a minute?",
                explanationFa = "borrow یعنی قرض گرفتن؛ lend یعنی قرض دادن."
            ),
            Contrast(
                first = "rise",
                second = "raise",
                firstSentence = "Prices often ____ during the holiday season.",
                secondSentence = "The company plans to ____ its prices next month.",
                explanationFa = "rise یعنی خودِ چیزی بالا برود؛ raise یعنی کسی یا چیزی آن را بالا ببرد."
            ),
            Contrast(
                first = "remember",
                second = "remind",
                firstSentence = "I must ____ to call the dentist.",
                secondSentence = "Please ____ me to call the dentist.",
                explanationFa = "remember یعنی به خاطر آوردن؛ remind یعنی چیزی را به یادِ کسی انداختن."
            ),
            Contrast(
                first = "affect",
                second = "effect",
                firstSentence = "Lack of sleep can ____ your concentration.",
                secondSentence = "The new policy had an immediate ____ on prices.",
                explanationFa = "affect معمولاً فعلِ «تأثیر گذاشتن» است؛ effect معمولاً اسمِ «تأثیر یا نتیجه» است."
            ),
            Contrast(
                first = "advice",
                second = "advise",
                firstSentence = "Could you give me some ____?",
                secondSentence = "I would ____ you to check the figures.",
                explanationFa = "advice اسم و به‌معنی توصیه است؛ advise فعل و به‌معنی توصیه کردن است."
            ),
            Contrast(
                first = "accept",
                second = "except",
                firstSentence = "She decided to ____ the job offer.",
                secondSentence = "Everyone came ____ Ali.",
                explanationFa = "accept یعنی پذیرفتن؛ except در این جمله یعنی به‌جز."
            ),
            Contrast(
                first = "lose",
                second = "loose",
                firstSentence = "Be careful not to ____ your keys.",
                secondSentence = "This screw is ____; it needs tightening.",
                explanationFa = "lose یعنی گم‌کردن یا از دست‌دادن؛ loose یعنی شل یا گشاد."
            ),
            Contrast(
                first = "do",
                second = "make",
                firstSentence = "I need to ____ my homework before dinner.",
                secondSentence = "They had to ____ a difficult decision.",
                explanationFa = "do با فعالیت‌هایی مثل تکلیف می‌آید؛ make با ساختن یا ایجادکردن نتیجه‌ای مثل تصمیم."
            ),
            Contrast(
                first = "discover",
                second = "invent",
                firstSentence = "Scientists hope to ____ a new planet.",
                secondSentence = "She hopes to ____ a tool that saves water.",
                explanationFa = "discover یعنی چیزی را که از قبل وجود دارد کشف‌کردن؛ invent یعنی چیزی تازه اختراع‌کردن."
            )
        )

        fun forItem(item: VocabularyItem): ReviewTask {
            val candidates = buildList {
                add(ReviewMode.MEANING)
                if (item.persianMeaning.isNotBlank()) add(ReviewMode.WORD_RECALL)
                if (VocabularyStudyPolicy.definitionCue(item) != null) add(ReviewMode.ENGLISH_DEFINITION)
                if (VocabularyStudyPolicy.clozeSentence(item) != null) add(ReviewMode.CONTEXT)
                if (reviewedContrasts.any {
                        item.word.equals(it.first, ignoreCase = true) ||
                            item.word.equals(it.second, ignoreCase = true)
                    }
                ) add(ReviewMode.SYNONYM)
            }
            val leastPracticed = candidates.minOf { VocabularyStudyPolicy.skillMastery(item, it.skill) }
            val tied = candidates.filter { VocabularyStudyPolicy.skillMastery(item, it.skill) == leastPracticed }
            // Spread first encounters across eligible modes; then prioritize the weakest skill.
            val mode = tied[Math.floorMod(item.id + ReviewPersistencePolicy.sequence(item), tied.size.toLong()).toInt()]

            return when (mode) {
                ReviewMode.MEANING -> ReviewTask(item, mode, item.word, item.persianMeaning)
                ReviewMode.WORD_RECALL -> ReviewTask(item, mode, item.persianMeaning, item.word)
                ReviewMode.ENGLISH_DEFINITION -> ReviewTask(
                    item, mode, VocabularyStudyPolicy.definitionCue(item).orEmpty(), item.word
                )
                ReviewMode.CONTEXT -> ReviewTask(
                    item, mode, VocabularyStudyPolicy.clozeSentence(item).orEmpty(), item.word
                )
                ReviewMode.SYNONYM -> {
                    val contrast = reviewedContrasts.first {
                        item.word.equals(it.first, ignoreCase = true) ||
                            item.word.equals(it.second, ignoreCase = true)
                    }
                    val firstIsTarget = item.word.equals(contrast.first, ignoreCase = true)
                    val sentence = if (firstIsTarget) contrast.firstSentence else contrast.secondSentence
                    ReviewTask(
                        item,
                        mode,
                        "${contrast.first.replaceFirstChar { it.uppercase() }} or ${contrast.second}?\n$sentence",
                        item.word,
                        answerNoteFa = contrast.explanationFa
                    )
                }
            }
        }

        private fun normalize(value: String): String = value
            .trim()
            .lowercase(Locale.US)
            .replace('’', '\'')
            .replace('‘', '\'')
            .replace('–', '-')
            .replace('—', '-')
            .trimEnd('.', ',', '!', '?', ';', ':')
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
