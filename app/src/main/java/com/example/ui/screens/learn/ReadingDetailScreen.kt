package com.example.ui.screens.learn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import com.example.audio.TtsManager
import com.example.data.seed.ReadingListeningSeed
import com.example.ui.components.AppCard
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun ReadingDetailScreen(
    passageId: String,
    viewModel: LearnViewModel,
    onBack: () -> Unit
) {
    val passage = remember(passageId) {
        ReadingListeningSeed.getReadingPassages().find { it.id == passageId }
            ?: ReadingListeningSeed.getReadingPassages().first()
    }
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var checkedAnswers by remember { mutableStateOf(mutableMapOf<String, Boolean>()) }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = passage.titleEn,
                    subtitle = passage.exam,
                    onBack = onBack,
                    actions = {
                        CefrBadge(
                            level = passage.level,
                            modifier = Modifier.padding(end = Dimens.screenGutter)
                        )
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = Dimens.screenGutter,
                    end = Dimens.screenGutter,
                    top = Dimens.sectionGap,
                    bottom = Dimens.space20
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
            ) {
                item {
                    AppCard(padding = Dimens.cardPaddingTight) {
                        SectionHeader(
                            title = "Study Tip",
                            subtitle = "Tap any word to view its definition, hear it, and save it to your vocabulary."
                        )
                    }
                }
                item {
                    AppCard(padding = Dimens.cardPaddingLoose) {
                        SectionHeader(
                            title = "Academic Passage",
                            subtitle = passage.topic
                        )
                        Column(
                            modifier = Modifier.padding(top = Dimens.blockGap),
                            verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                        ) {
                            passage.contentEn.split("\n\n").forEach { paragraph ->
                                InteractiveParagraph(
                                    text = paragraph,
                                    onWordClicked = viewModel::inspectWord
                                )
                            }
                        }
                    }
                }
                if (passage.questions.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Comprehension Questions")
                    }
                    itemsIndexed(
                        items = passage.questions,
                        key = { _, question -> question.id }
                    ) { index, question ->
                        QuizQuestionCard(
                            questionIndex = index,
                            question = question.questionEn,
                            options = question.options,
                            correctIndex = question.correctIndex,
                            explanationFa = question.explanationFa,
                            selectedOption = userAnswers[question.id],
                            checked = checkedAnswers[question.id] == true,
                            onOptionSelected = { optionIndex ->
                                userAnswers = userAnswers.toMutableMap().apply {
                                    this[question.id] = optionIndex
                                }
                            },
                            onCheck = {
                                checkedAnswers = checkedAnswers.toMutableMap().apply {
                                    this[question.id] = true
                                }
                            }
                        )
                    }
                }
            }

            state.popupWordDetail?.let { detail ->
                AlertDialog(
                    onDismissRequest = viewModel::dismissWordPopup,
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = detail.word,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            AudioSpeakerButton(
                                onClick = { tts.speak(detail.word) },
                                size = Dimens.minTapTarget,
                                contentDescription = "Play pronunciation"
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
                            if (detail.ipa.isNotEmpty()) {
                                Text(
                                    text = detail.ipa,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FieldLabel(text = "Persian Meaning")
                            PersianContentRtl {
                                Text(
                                    text = detail.persianMeaning,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (detail.englishDefinition.isNotEmpty()) {
                                FieldLabel(text = "English Definition")
                                Text(
                                    text = detail.englishDefinition,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            state.isWordSavedStatus?.let { status ->
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Accent.successOnSoft,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.saveInspectedWord(detail) },
                            modifier = Modifier.heightIn(min = Dimens.minTapTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(Dimens.space4))
                            Text("Add to My Vocab")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = viewModel::dismissWordPopup,
                            modifier = Modifier.heightIn(min = Dimens.minTapTarget)
                        ) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveParagraph(
    text: String,
    onWordClicked: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.space2)
    ) {
        text.split(" ").forEach { rawWord ->
            val cleanWord = rawWord.trim().replace(Regex("[^a-zA-Z]"), "")
            Text(
                text = "$rawWord ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .defaultMinSize(minHeight = Dimens.minTapTarget)
                    .clickable(enabled = cleanWord.isNotEmpty()) {
                        onWordClicked(cleanWord)
                    }
                    .padding(
                        horizontal = Dimens.space2,
                        vertical = Dimens.space8
                    )
            )
        }
    }
}
