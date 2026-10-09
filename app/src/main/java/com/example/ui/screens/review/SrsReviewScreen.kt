package com.example.ui.screens.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import com.example.srs.Fsrs6
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.srs.SpacedRepetitionSystem
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.HairLine
import com.example.ui.components.IconTile
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun SrsReviewScreen(
    viewModel: ReviewViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    ReviewSessionScreen(state, ReviewSessionActions(
        onStart = viewModel::startSession, onAnswer = viewModel::updateAnswer,
        onCheck = viewModel::checkAnswer, onReveal = viewModel::revealAnswer,
        onRate = viewModel::submitRating, onDontKnow = viewModel::dontKnow,
        onAlternative = viewModel::submitAlternative, onHint = viewModel::showHint,
        onRetention = viewModel::setRetention
    ), onBack)
}

internal data class ReviewSessionActions(
    val onStart: () -> Unit = {}, val onAnswer: (String) -> Unit = {},
    val onCheck: () -> Unit = {}, val onReveal: () -> Unit = {},
    val onRate: (ReviewRating) -> Unit = {}, val onDontKnow: () -> Unit = {},
    val onAlternative: () -> Unit = {}, val onHint: () -> Unit = {},
    val onRetention: (Double) -> Unit = {}
)

@Composable
internal fun ReviewSessionScreen(state: ReviewSessionUiState, actions: ReviewSessionActions, onBack: () -> Unit = {}) {
    var retentionDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    LaunchedEffect(state.answerChecked) {
        if (state.answerChecked) focusManager.clearFocus()
    }
    val tts = remember { TtsManager(context) }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    if (retentionDialog) {
        AlertDialog(onDismissRequest = { retentionDialog = false },
            title = { Text("FSRS retention target") },
            text = {
                Column {
                    Text("A higher target brings future graded reviews closer together. This is a scheduling target, not a measured guarantee.")
                    listOf(0.85, 0.90, 0.95).forEach { target ->
                        TextButton(onClick = { actions.onRetention(target); retentionDialog = false }) {
                            Text("${(target * 100).toInt()}% — a 10-day stability: ${Fsrs6.interval(10.0, target)} days")
                        }
                    }
                    Text("${state.evidence.samples} delayed, independent answers recorded.")
                    if (state.evidence.samples >= 20) {
                        Text("Observed recall: ${((state.evidence.recallRate ?: 0.0) * 100).toInt()}%")
                        Text("Prediction error (Brier): ${"%.3f".format(java.util.Locale.US, state.evidence.brierScore ?: 0.0)}; lower is better.")
                    } else Text("Your history is still building; long-term recall has not yet been measured.")
                }
            }, confirmButton = { TextButton(onClick = { retentionDialog = false }) { Text("Close") } })
    }
    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "Vocabulary Review",
                    subtitle = "Only words you have already learned",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            when {
                state.errorMessage != null && state.queue.isEmpty() -> {
                    Column(Modifier.padding(paddingValues).padding(Dimens.screenGutter)) {
                        Text(state.errorMessage.orEmpty())
                        Button(onClick = actions.onStart) { Text("Retry") }
                    }
                }
                state.isSessionFinished -> {
                    FinishedVocabularyReview(
                        reviewedCount = state.completedCount,
                        xp = state.xpEarned,
                        onRefresh = actions.onStart,
                        onBack = onBack,
                        modifier = Modifier.padding(paddingValues)
                    )
                }

                state.queue.getOrNull(state.currentIndex) == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimens.iconLg),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = Dimens.hairline
                        )
                    }
                }

                else -> {
                    val task = state.queue[state.currentIndex]
                    val currentWord = task.item
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues)
                            .imePadding()
                            .padding(
                                horizontal = Dimens.screenGutter,
                                vertical = Dimens.space12
                            ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
                    ) {
                        ReviewProgressHeader(
                            currentIndex = state.currentIndex,
                            total = state.sessionTotal,
                            mastery = currentWord.mastery,
                            retention = state.desiredRetention,
                            onSettings = { retentionDialog = true }
                        )
                        key(task.item.id, task.mode) {
                            VocabularyReviewContent(
                                task = task,
                                item = currentWord,
                                answerText = state.answerText,
                                answerChecked = state.answerChecked,
                                typedAnswerCorrect = state.typedAnswerCorrect,
                                hintUsed = state.hintUsed,
                                onHint = actions.onHint,
                                onAnswerChange = actions.onAnswer,
                                isRevealed = state.isAnswerRevealed,
                                onPlayWord = { tts.speak(currentWord.word) },
                                onPlayExample = {
                                    if (currentWord.example.isNotBlank()) {
                                        tts.speak(currentWord.example)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                        if (state.errorMessage != null) {
                            Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = actions.onStart) { Text("Restart review") }
                        }
                        if (state.isAnswerRevealed) {
                            if (task.allowsAlternative && state.typedAnswerCorrect == false && state.answerText.isNotBlank()) {
                                OutlinedButton(onClick = actions.onAlternative, enabled = !state.isSubmitting) {
                                    Text("Another answer fits — skip grading")
                                }
                            }
                            ReviewActionBar(
                                item = task.predictionItem,
                                desiredRetention = state.desiredRetention,
                                hintUsed = state.hintUsed,
                                verifiedAlternative = task.mode.requiresTypedAnswer && state.typedAnswerCorrect == true && !task.isExactTarget(state.answerText),
                                enabled = !state.isSubmitting,
                                isScheduled = task.isSkillDue,
                                successEnabled = !task.mode.requiresTypedAnswer || state.typedAnswerCorrect == true,
                                onRate = actions.onRate
                            )
                        } else {
                            if (task.mode.requiresTypedAnswer) {
                                OutlinedButton(onClick = actions.onDontKnow, enabled = !state.isSubmitting) {
                                    Text("I don’t know")
                                }
                            }
                            val canCheck = task.mode.requiresTypedAnswer && !state.answerChecked
                            Button(
                                onClick = if (canCheck) actions.onCheck else actions.onReveal,
                                enabled = !state.isSubmitting && (!canCheck || state.answerText.isNotBlank()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.minTapTarget),
                                shape = RoundedCornerShape(Dimens.radiusSm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.iconSm)
                                )
                                Spacer(modifier = Modifier.width(Dimens.space8))
                                Text(if (canCheck) "Check Answer" else "Show Answer", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewProgressHeader(
    currentIndex: Int,
    total: Int,
    mastery: Int,
    retention: Double,
    onSettings: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Word ${currentIndex + 1} of $total",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$mastery progress · FSRS ${(retention * 100).toInt()}%",
                modifier = Modifier.clickable(onClick = onSettings).padding(Dimens.space4),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LinearProgressIndicator(
            progress = {
                ((currentIndex + 1).toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.progressHeight)
                .clip(RoundedCornerShape(Dimens.radiusPill)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun VocabularyReviewContent(
    task: ReviewTask,
    item: VocabularyItem,
    answerText: String,
    answerChecked: Boolean,
    typedAnswerCorrect: Boolean?,
    hintUsed: Boolean,
    onHint: () -> Unit,
    onAnswerChange: (String) -> Unit,
    isRevealed: Boolean,
    onPlayWord: () -> Unit,
    onPlayExample: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = Dimens.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
    ) {
        Text(
            text = task.mode.instruction,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CefrBadge(level = item.cefrLevel)
        if (task.mode == ReviewMode.MEANING) {
            Text(
                text = item.word,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = task.prompt,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (hintUsed) {
                Text(task.targetHint, style = MaterialTheme.typography.labelMedium)
            } else if (!answerChecked) {
                TextButton(onClick = onHint) { Text("Show a hint") }
            }
            OutlinedTextField(
                value = answerText,
                onValueChange = onAnswerChange,
                enabled = !answerChecked,
                label = { Text("Your answer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (answerChecked) {
                Text(
                    text = when {
                        typedAnswerCorrect != true -> "Different from the target. Another word may also fit."
                        !task.isExactTarget(answerText) -> "Valid alternative. The target word stays ungraded."
                        hintUsed -> "Correct with a hint — guided practice"
                        else -> "Correct"
                    },
                    color = if (typedAnswerCorrect == true) Accent.success else Accent.danger,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        if ((task.mode == ReviewMode.MEANING || isRevealed) && item.ipa.isNotBlank()) {
            Text(
                text = item.ipa,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (task.mode == ReviewMode.MEANING || isRevealed) {
            AudioSpeakerButton(
                onClick = onPlayWord,
                contentDescription = "Play word pronunciation"
            )
        }

        if (isRevealed) {
            HairLine(modifier = Modifier.padding(vertical = Dimens.space4))
            if (task.mode != ReviewMode.MEANING) {
                Text(
                    text = item.word,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (task.answerNoteFa.isNotBlank()) {
                AppInset(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    PersianContentRtl {
                        Text(
                            text = task.answerNoteFa,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            task.lexicalRelation?.let { relation ->
                val uriHandler = LocalUriHandler.current
                Text("${relation.type.name.replace('_', ' ')} · compare these senses", style = MaterialTheme.typography.labelLarge)
                listOf(relation.first, relation.second).forEach { sense ->
                    AppInset {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                            Text("${sense.word} (${sense.partOfSpeech})", fontWeight = FontWeight.Bold)
                            Text(sense.definition)
                            PersianContentRtl { Text(sense.meaningFa) }
                            Text("“${sense.example}”")
                        }
                    }
                }
                relation.sources.forEachIndexed { index, url ->
                    TextButton(onClick = { uriHandler.openUri(url) }) { Text("Usage reference: ${if (index == 0) relation.first.word else relation.second.word}") }
                }
            }
            if (task.lexicalRelation == null && item.englishDefinition.isNotBlank()) {
                Text(
                    text = item.englishDefinition,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (item.example.isNotBlank()) {
                AppInset(
                    modifier = Modifier.heightIn(min = Dimens.minTapTarget),
                    onClick = onPlayExample
                ) {
                    Text(
                        text = "“${item.example}”",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AppInset(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                PersianContentRtl {
                    Text(
                        text = item.persianMeaning,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.examplePersian.isNotBlank()) {
                    PersianContentRtl {
                        Text(
                            text = item.examplePersian,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewActionBar(
    item: VocabularyItem,
    desiredRetention: Double,
    hintUsed: Boolean,
    verifiedAlternative: Boolean,
    enabled: Boolean,
    successEnabled: Boolean,
    isScheduled: Boolean,
    onRate: (ReviewRating) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        Text(
            text = when {
                verifiedAlternative -> "Valid alternative — continue without grading target"
                hintUsed -> "A hint was used — successful answers count as Hard"
                isScheduled -> "How well did you remember this word?"
                else -> "Extra practice — success keeps your scheduled review"
            },
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            ReviewRatingButton(
                label = if (verifiedAlternative) "Continue" else "Again",
                interval = if (verifiedAlternative) "Ungraded" else "30 min",
                color = Accent.danger,
                enabled = enabled,
                onClick = { onRate(if (verifiedAlternative) ReviewRating.GOOD else ReviewRating.AGAIN) },
                modifier = Modifier.weight(1f)
            )
            ReviewRatingButton(
                label = "Hard",
                interval = if (isScheduled) SpacedRepetitionSystem.getIntervalLabel(item, ReviewRating.HARD, desiredRetention) else "Practice",
                color = Accent.warning,
                enabled = enabled && successEnabled && !verifiedAlternative,
                onClick = { onRate(ReviewRating.HARD) },
                modifier = Modifier.weight(1f)
            )
            ReviewRatingButton(
                label = "Good",
                interval = if (isScheduled) SpacedRepetitionSystem.getIntervalLabel(item, if (hintUsed) ReviewRating.HARD else ReviewRating.GOOD, desiredRetention) else "Practice",
                color = MaterialTheme.colorScheme.primary,
                enabled = enabled && successEnabled && !verifiedAlternative,
                onClick = { onRate(if (hintUsed) ReviewRating.HARD else ReviewRating.GOOD) },
                modifier = Modifier.weight(1f)
            )
            ReviewRatingButton(
                label = "Easy",
                interval = if (isScheduled) SpacedRepetitionSystem.getIntervalLabel(item, if (hintUsed) ReviewRating.HARD else ReviewRating.EASY, desiredRetention) else "Practice",
                color = Accent.success,
                enabled = enabled && successEnabled && !verifiedAlternative,
                onClick = { onRate(if (hintUsed) ReviewRating.HARD else ReviewRating.EASY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReviewRatingButton(
    label: String,
    interval: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.radiusSm),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.10f),
            contentColor = color,
            disabledContainerColor = color.copy(alpha = 0.05f),
            disabledContentColor = color.copy(alpha = 0.45f)
        ),
        modifier = modifier.height(Dimens.minTapTarget + Dimens.space8)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = interval,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FinishedVocabularyReview(
    reviewedCount: Int,
    xp: Int,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = Dimens.screenGutter,
                vertical = Dimens.space12
            ),
        contentAlignment = Alignment.Center
    ) {
        AppCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
            ) {
                IconTile(
                    icon = if (reviewedCount == 0) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.EmojiEvents
                    },
                    tint = if (reviewedCount == 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Accent.warning
                    },
                    size = Dimens.cardPaddingLoose * 4,
                    iconSize = Dimens.iconLg,
                    shape = RoundedCornerShape(Dimens.radiusLg)
                )
                Text(
                    text = if (reviewedCount == 0) {
                        "No cards available right now"
                    } else {
                        "Vocabulary review complete"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (reviewedCount == 0) {
                        "Due words come first, followed by extra practice. Recently practised words rest for 30 minutes. New words never appear here."
                    } else {
                        "You reviewed $reviewedCount learned words. Words marked Again return within 30 minutes."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (reviewedCount > 0) {
                    TagChip(
                        text = "+$xp XP",
                        containerColor = Accent.successSoft,
                        contentColor = Accent.successOnSoft
                    )
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.minTapTarget),
                    shape = RoundedCornerShape(Dimens.radiusSm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space8))
                    Text(
                        text = "Check Due Words Again",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.minTapTarget),
                    shape = RoundedCornerShape(Dimens.radiusSm)
                ) {
                    Text("Back")
                }
            }
        }
    }
}
