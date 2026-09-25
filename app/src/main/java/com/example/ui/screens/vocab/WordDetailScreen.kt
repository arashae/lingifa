package com.example.ui.screens.vocab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.data.model.VocabularySense
import com.example.srs.ReviewRating
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.HairLine
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun WordDetailScreen(
    vocabId: Long,
    viewModel: VocabViewModel,
    onBack: () -> Unit
) {
    val wordFlow = remember(vocabId) { viewModel.getWordById(vocabId) }
    val item by wordFlow.collectAsState(initial = null)
    val sensesFlow = remember(vocabId) { viewModel.getSensesForWord(vocabId) }
    val senses by sensesFlow.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    var userSelectedTab by remember(vocabId) { mutableStateOf<Int?>(null) }
    var feedbackMessage by remember(vocabId) { mutableStateOf<String?>(null) }

    DisposableEffect(tts) { onDispose { tts.shutdown() } }

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LinguaTopAppBar(
                    title = "Word Details",
                    onBack = onBack,
                    actions = {
                        item?.let { current ->
                            IconButton(
                                onClick = { viewModel.toggleFavorite(current) },
                                modifier = Modifier.size(Dimens.minTapTarget)
                            ) {
                                Icon(
                                    imageVector = if (current.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (current.isFavorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (current.isFavorite) Accent.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(Dimens.iconMd)
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            val currentWord = item
            if (currentWord == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading word…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                return@Scaffold
            }

            val defaultTab = if (currentWord.cefrLevel.uppercase() in setOf("A1", "A2")) 0 else 1
            val selectedTab = userSelectedTab ?: defaultTab

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space10),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                WordHeroCard(
                    item = currentWord,
                    onPronounce = { tts.speak(currentWord.word) }
                )

                LanguageTabs(
                    selectedTab = selectedTab,
                    onSelected = { userSelectedTab = it }
                )

                if (senses.size > 1) {
                    WordSensesCard(
                        senses = senses,
                        englishFirst = selectedTab == 1,
                        onPronounceExample = { tts.speak(it) }
                    )
                } else {
                    DefinitionCard(
                        item = currentWord,
                        englishFirst = selectedTab == 1,
                        onPronounceExample = { tts.speak(currentWord.example) }
                    )
                }

                RelatedLanguageCard(
                    collocations = currentWord.collocations,
                    synonyms = currentWord.synonyms,
                    wordFamily = currentWord.wordFamily
                )

                if (currentWord.commonMistakes.isNotBlank()) {
                    CommonMistakeCard(currentWord.commonMistakes)
                }

                StudyActionsCard(
                    ielts = currentWord.ieltsRelevance,
                    toefl = currentWord.toeflRelevance,
                    gre = currentWord.greRelevance,
                    feedbackMessage = feedbackMessage,
                    onKnown = {
                        viewModel.recordLearningJudgement(currentWord, ReviewRating.GOOD)
                        feedbackMessage = "Saved: scheduled for the optimal spaced-review interval."
                    },
                    onHard = {
                        viewModel.recordLearningJudgement(currentWord, ReviewRating.HARD)
                        feedbackMessage = "Saved: scheduled for early review."
                    },
                    onPractice = {
                        viewModel.recordLearningJudgement(currentWord, ReviewRating.AGAIN)
                        feedbackMessage = "Saved: added to the upcoming review queue."
                    }
                )
            }
        }
    }
}

@Composable
private fun WordHeroCard(item: VocabularyItem, onPronounce: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        padding = Dimens.cardPadding,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space10)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.ipa.isNotBlank()) {
                        Text(
                            text = item.ipa,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.partOfSpeech.isNotBlank() && item.partOfSpeech != "word") {
                        Text(
                            text = item.partOfSpeech,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                CefrBadge(level = item.cefrLevel)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                AudioSpeakerButton(onClick = onPronounce)
                Text(
                    text = "Listen to pronunciation",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LanguageTabs(selectedTab: Int, onSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onSelected(0) },
            text = {
                Text(
                    text = "Persian",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onSelected(1) },
            text = {
                Text(
                    text = "English",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}

@Composable
private fun WordSensesCard(
    senses: List<VocabularySense>,
    englishFirst: Boolean,
    onPronounceExample: (String) -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Word Senses & Contexts",
            subtitle = "${senses.size} meanings"
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            senses.forEachIndexed { index, sense ->
                AppInset(padding = Dimens.space8) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagChip(
                                text = "Sense ${index + 1}${sense.partOfSpeech.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}",
                                modifier = Modifier.weight(1f),
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (sense.cefrLevel.isNotBlank()) {
                                CefrBadge(level = sense.cefrLevel)
                            }
                        }

                        if (englishFirst) {
                            if (sense.englishDefinition.isNotBlank()) {
                                Text(
                                    text = sense.englishDefinition,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (sense.persianMeaning.isNotBlank()) {
                                PersianContentRtl {
                                    Text(
                                        text = sense.persianMeaning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            if (sense.persianMeaning.isNotBlank()) {
                                PersianContentRtl {
                                    Text(
                                        text = sense.persianMeaning,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (sense.englishDefinition.isNotBlank()) {
                                Text(
                                    text = sense.englishDefinition,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (sense.exampleSentence.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(Dimens.space4)
                                ) {
                                    Text(
                                        text = sense.exampleSentence,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Start
                                    )
                                    if (sense.exampleTranslation.isNotBlank()) {
                                        PersianContentRtl {
                                            Text(
                                                text = sense.exampleTranslation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                AudioSpeakerButton(
                                    onClick = { onPronounceExample(sense.exampleSentence) },
                                    contentDescription = "Play example"
                                )
                            }
                        }

                        if (sense.collocations.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                                verticalArrangement = Arrangement.spacedBy(Dimens.space6)
                            ) {
                                sense.collocations.take(4).forEach { collocation ->
                                    TagChip(
                                        text = collocation,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefinitionCard(
    item: VocabularyItem,
    englishFirst: Boolean,
    onPronounceExample: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = if (englishFirst) "English Definition" else "Meaning & Definition"
        )
        if (englishFirst) {
            if (item.englishDefinition.isNotBlank()) {
                Text(
                    text = item.englishDefinition,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PersianContentRtl {
                    Text(
                        text = item.persianMeaning,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item.englishDefinition.isNotBlank()) {
                PersianContentRtl {
                    Text(
                        text = item.persianMeaning,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            PersianContentRtl {
                Text(
                    text = item.persianMeaning,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (item.englishDefinition.isNotBlank()) {
                Text(
                    text = item.englishDefinition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (item.example.isNotBlank()) {
            HairLine()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space4)
                ) {
                    FieldLabel("Example")
                    Text(
                        text = item.example,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                    if (item.examplePersian.isNotBlank()) {
                        PersianContentRtl {
                            Text(
                                text = item.examplePersian,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                AudioSpeakerButton(
                    onClick = onPronounceExample,
                    contentDescription = "Play example"
                )
            }
        }
    }
}

@Composable
private fun RelatedLanguageCard(
    collocations: List<String>,
    synonyms: List<String>,
    wordFamily: List<String>
) {
    if (collocations.isEmpty() && synonyms.isEmpty() && wordFamily.isEmpty()) return

    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Related Language",
            subtitle = "Natural combinations and related forms"
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            if (collocations.isNotEmpty()) {
                AppInset(padding = Dimens.space8) {
                    FieldLabel("Collocations")
                    Spacer(modifier = Modifier.size(Dimens.space4))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space6)
                    ) {
                        collocations.take(10).forEach { value ->
                            TagChip(
                                text = value,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            if (synonyms.isNotEmpty()) {
                DetailListInset(title = "Synonyms", values = synonyms)
            }
            if (wordFamily.isNotEmpty()) {
                DetailListInset(title = "Word Family", values = wordFamily)
            }
        }
    }
}

@Composable
private fun DetailListInset(title: String, values: List<String>) {
    AppInset(padding = Dimens.space8) {
        FieldLabel(title)
        Spacer(modifier = Modifier.size(Dimens.space4))
        Text(
            text = values.take(10).joinToString("  ·  "),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CommonMistakeCard(text: String) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.24f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimens.iconMd)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.space4)
            ) {
                Text(
                    text = "Common Mistake",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StudyActionsCard(
    ielts: String,
    toefl: String,
    gre: String,
    feedbackMessage: String?,
    onKnown: () -> Unit,
    onHard: () -> Unit,
    onPractice: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Exam & Review",
            subtitle = "Exam relevance and spaced-repetition assessment"
        )
        AppInset(padding = Dimens.space8) {
            ExamRow("IELTS", ielts)
            ExamRow("TOEFL", toefl)
            ExamRow("GRE", gre)
        }
        HairLine()
        FieldLabel("SRS learning status")
        Text(
            text = "Choose the result that best matches your recall.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            Button(
                onClick = onKnown,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.minTapTarget)
            ) {
                Text("Mastered", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onHard,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.minTapTarget)
            ) {
                Text("Hard", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onPractice,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.minTapTarget)
            ) {
                Text("Practice", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        feedbackMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExamRow(exam: String, relevance: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = exam,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = relevance.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
