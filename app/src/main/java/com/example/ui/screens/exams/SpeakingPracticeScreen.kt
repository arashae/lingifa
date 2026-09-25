package com.example.ui.screens.exams

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TtsManager
import com.example.data.model.IeltsSpeakingFeedback
import com.example.data.model.IeltsSpeakingPrompt
import com.example.data.model.IeltsSpeakingSessionRecord
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.HairLine
import com.example.ui.components.IconTile
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.MinimalStreakCard
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectChip
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeakingPracticeScreen(
    viewModel: ExamsViewModel? = null,
    ieltsViewModel: IeltsSpeakingViewModel = viewModel(),
    onBack: () -> Unit
) {
    val state by ieltsViewModel.uiState.collectAsState()
    val streakInfo by ieltsViewModel.streakInfo.collectAsState()
    val pastSessions by ieltsViewModel.pastSessions.collectAsState()
    val avgBand by ieltsViewModel.averageBandScore.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showHistorySheet by remember { mutableStateOf(false) }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            ieltsViewModel.clearStatusMessage()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LinguaTopAppBar(
                    title = "IELTS Speaking Simulator",
                    subtitle = "AI examiner with band scoring and pronunciation analysis",
                    onBack = onBack,
                    actions = {
                        avgBand?.let { score ->
                            TagChip(
                                text = "Band ${String.format(Locale.US, "%.1f", score)}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = Dimens.space4)
                            )
                        }
                        IconButton(onClick = { showHistorySheet = !showHistorySheet }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Exam history",
                                tint = if (showHistorySheet) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space8),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                MinimalStreakCard(
                    streakInfo = streakInfo,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showHistorySheet && pastSessions.isNotEmpty()) {
                    IeltsSpeakingHistoryCard(
                        sessions = pastSessions,
                        onClose = { showHistorySheet = false }
                    )
                }

                TabRow(
                    selectedTabIndex = state.selectedPart - 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[state.selectedPart - 1]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.radiusLg))
                ) {
                    listOf("Part 1 · Interview", "Part 2 · Cue Card", "Part 3 · Discussion").forEachIndexed { index, label ->
                        val part = index + 1
                        Tab(
                            selected = state.selectedPart == part,
                            onClick = { ieltsViewModel.selectPart(part) },
                            text = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (state.selectedPart == part) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }

                IeltsCueCardView(
                    prompt = state.currentPrompt,
                    isExaminerSpeaking = state.isExaminerSpeaking,
                    isUkAccent = state.isUkAccent,
                    onToggleAccent = { ieltsViewModel.toggleAccent() },
                    onPlayAudio = { ieltsViewModel.playExaminerPrompt(tts) },
                    onGenerateAiPrompt = { topic -> ieltsViewModel.generateAiPrompt(topic) }
                )

                IeltsTimerSection(
                    part = state.selectedPart,
                    prepSeconds = state.prepTimerSeconds,
                    isPrepRunning = state.isPrepTimerRunning,
                    speakingSeconds = state.speakingTimerSeconds,
                    isRecording = state.isRecording,
                    onTogglePrep = { ieltsViewModel.togglePrepTimer() },
                    onResetPrep = { ieltsViewModel.resetPrepTimer() },
                    onToggleRecording = { ieltsViewModel.toggleRecording() }
                )

                AppCard(
                    shape = RoundedCornerShape(Dimens.radiusLg),
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                ) {
                    SectionHeader(
                        title = "Your speaking response",
                        actionText = "Sample answer",
                        onActionClick = { ieltsViewModel.populateSampleCandidateAnswer() }
                    )

                    OutlinedTextField(
                        value = state.candidateTranscript,
                        onValueChange = { ieltsViewModel.onTranscriptChanged(it) },
                        placeholder = {
                            Text(
                                text = if (state.isRecording) "Recording your speech. Speak clearly or type your response."
                                else "Speak your response or type here."
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    val wordCount = state.candidateTranscript.trim()
                        .split(Regex("\\s+"))
                        .count { it.isNotBlank() }
                    val targetWords = if (state.selectedPart == 2) "150–220 words for 2 min" else "40–70 words"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Word count: $wordCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Target: $targetWords",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (wordCount >= 60) Accent.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = { ieltsViewModel.evaluateSpeakingResponse() },
                        enabled = state.candidateTranscript.isNotBlank() && !state.isEvaluating,
                        shape = RoundedCornerShape(Dimens.radiusMd),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.minTapTarget)
                    ) {
                        if (state.isEvaluating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimens.iconMd),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = Dimens.space2
                            )
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text("Evaluating across 4 IELTS criteria…", style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text("Evaluate with AI examiner (+35 XP)", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                state.feedback?.let { feedback ->
                    IeltsSpeakingFeedbackDashboard(
                        feedback = feedback,
                        isModelSpeaking = state.isModelSpeaking,
                        onPlayModelAudio = { ieltsViewModel.playModelAnswer(tts) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IeltsCueCardView(
    prompt: IeltsSpeakingPrompt,
    isExaminerSpeaking: Boolean,
    isUkAccent: Boolean,
    onToggleAccent: () -> Unit,
    onPlayAudio: () -> Unit,
    onGenerateAiPrompt: (String) -> Unit
) {
    AppCard(
        shape = RoundedCornerShape(Dimens.radiusXl),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.space4)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TagChip(
                        text = "Part ${prompt.part}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = prompt.topicEn,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                PersianContentRtl {
                    Text(
                        text = prompt.topicFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectChip(
                    text = if (isUkAccent) "UK accent" else "US accent",
                    selected = isUkAccent,
                    onClick = onToggleAccent
                )
                if (isExaminerSpeaking) {
                    TagChip(
                        text = "Playing",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                AudioSpeakerButton(
                    onClick = onPlayAudio,
                    size = Dimens.minTapTarget,
                    contentDescription = "Play examiner prompt"
                )
            }
        }

        Text(
            text = prompt.questionEn,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
        PersianContentRtl {
            Text(
                text = prompt.questionFa,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (prompt.part == 2 && prompt.cueCardBulletPoints.isNotEmpty()) {
            AppInset(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                FieldLabel("You should say", color = MaterialTheme.colorScheme.primary)
                prompt.cueCardBulletPoints.forEach { point ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("•", color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (prompt.recommendedVocab.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
                FieldLabel("Recommended vocabulary and collocations")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    prompt.recommendedVocab.forEach { vocab ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TagChip(
                                text = vocab.word,
                                containerColor = Accent.successSoft,
                                contentColor = Accent.successOnSoft
                            )
                            PersianContentRtl {
                                Text(
                                    text = vocab.meaningFa,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
            FieldLabel("AI topics")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                verticalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                listOf("Environment", "Artificial Intelligence", "Higher Education", "Urbanization", "Hometown")
                    .forEach { topic ->
                        SelectChip(
                            text = topic,
                            selected = false,
                            onClick = { onGenerateAiPrompt(topic) }
                        )
                    }
            }
        }
    }
}

@Composable
private fun IeltsTimerSection(
    part: Int,
    prepSeconds: Int,
    isPrepRunning: Boolean,
    speakingSeconds: Int,
    isRecording: Boolean,
    onTogglePrep: () -> Unit,
    onResetPrep: () -> Unit,
    onToggleRecording: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap)
    ) {
        if (part == 2) {
            AppCard(
                modifier = Modifier.weight(1f),
                padding = Dimens.cardPaddingTight,
                shape = RoundedCornerShape(Dimens.radiusLg),
                borderColor = if (isPrepRunning) Accent.warning else MaterialTheme.colorScheme.outlineVariant
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    Text("Prep timer (1 min)", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = String.format(Locale.US, "00:%02d", prepSeconds),
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (prepSeconds <= 10) Accent.danger else Accent.warning
                    )
                    LinearProgressIndicator(
                        progress = { (prepSeconds / 60f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.progressHeight)
                            .clip(RoundedCornerShape(Dimens.radiusPill)),
                        color = Accent.warning,
                        trackColor = Accent.warning.copy(alpha = 0.2f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.space6)) {
                        IconButton(
                            onClick = onTogglePrep,
                            modifier = Modifier.size(Dimens.minTapTarget)
                        ) {
                            Icon(
                                imageVector = if (isPrepRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Start or pause preparation timer",
                                tint = Accent.warning
                            )
                        }
                        IconButton(
                            onClick = onResetPrep,
                            modifier = Modifier.size(Dimens.minTapTarget)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Reset preparation timer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val infiniteTransition = rememberInfiniteTransition(label = "recording pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isRecording) 1.25f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "recording pulse scale"
        )
        AppCard(
            modifier = Modifier.weight(1f),
            padding = Dimens.cardPaddingTight,
            shape = RoundedCornerShape(Dimens.radiusLg),
            borderColor = if (isRecording) Accent.danger else MaterialTheme.colorScheme.outlineVariant
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                Text("Speaking duration", style = MaterialTheme.typography.labelMedium)
                val mins = speakingSeconds / 60
                val secs = speakingSeconds % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (isRecording) Accent.danger else MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onToggleRecording,
                    modifier = Modifier
                        .size(Dimens.minTapTarget)
                        .scale(if (isRecording) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(if (isRecording) Accent.danger else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Record speech",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Dimens.iconLg)
                    )
                }
                Text(
                    text = if (isRecording) "Recording… tap to stop" else "Tap the mic to start speaking",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRecording) Accent.danger else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class SpeakingCriterion(
    val title: String,
    val score: Float,
    val feedback: String
)

@Composable
private fun IeltsSpeakingFeedbackDashboard(
    feedback: IeltsSpeakingFeedback,
    isModelSpeaking: Boolean,
    onPlayModelAudio: () -> Unit
) {
    val criteria = listOf(
        SpeakingCriterion("Fluency & Coherence", feedback.fluencyScore, feedback.fluencyFeedbackFa),
        SpeakingCriterion("Lexical Range & Variety", feedback.lexicalScore, feedback.lexicalFeedbackFa),
        SpeakingCriterion("Grammatical Range & Accuracy", feedback.grammarScore, feedback.grammarFeedbackFa),
        SpeakingCriterion("Pronunciation & Intonation", feedback.pronunciationScore, feedback.pronunciationHintsFa)
    )

    AppCard(
        shape = RoundedCornerShape(Dimens.radiusXl),
        padding = Dimens.cardPaddingLoose,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(
                title = "IELTS examiner scorecard",
                subtitle = "Based on the four Cambridge assessment criteria",
                modifier = Modifier.weight(1f)
            )
            TagChip(
                text = "Band ${String.format(Locale.US, "%.1f", feedback.overallBand)}",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }

        criteria.forEachIndexed { index, criterion ->
            if (index > 0) HairLine()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.rowHeight)
                    .padding(vertical = Dimens.space4),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                IconTile(
                    icon = Icons.Default.GraphicEq,
                    tint = MaterialTheme.colorScheme.primary,
                    size = Dimens.iconTileSm,
                    iconSize = Dimens.iconSm
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    Text(
                        text = criterion.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    PersianContentRtl {
                        Text(
                            text = criterion.feedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                TagChip(
                    text = String.format(Locale.US, "%.1f", criterion.score),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        if (feedback.persianLearnerMistakes.isNotEmpty()) {
            AppInset(
                color = Accent.warningSoft,
                contentColor = Accent.warningOnSoft
            ) {
                SectionHeader(
                    title = "Pronunciation pitfalls",
                    subtitle = "Common patterns for Persian speakers"
                )
                PersianContentRtl {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                        feedback.persianLearnerMistakes.forEach { mistake ->
                            Text(
                                text = "• $mistake",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (feedback.vocabUpgrades.isNotEmpty()) {
            SectionHeader(title = "Band 8+ vocabulary upgrades")
            feedback.vocabUpgrades.forEach { upgrade ->
                AppInset(
                    padding = Dimens.cardPaddingTight,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = upgrade.original,
                            style = MaterialTheme.typography.bodySmall,
                            color = Accent.danger,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = upgrade.upgraded,
                            style = MaterialTheme.typography.bodySmall,
                            color = Accent.success,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    PersianContentRtl {
                        Text(
                            text = upgrade.explanationFa,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (feedback.band8ModelResponseEn.isNotBlank()) {
            AppInset(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTile(
                        icon = if (isModelSpeaking) Icons.Default.GraphicEq else Icons.Default.School,
                        tint = MaterialTheme.colorScheme.primary,
                        size = Dimens.iconTileSm,
                        iconSize = Dimens.iconSm
                    )
                    SectionHeader(
                        title = "Band 8+ model answer",
                        subtitle = "Listen, then compare your response",
                        modifier = Modifier.weight(1f)
                    )
                    AudioSpeakerButton(
                        onClick = onPlayModelAudio,
                        size = Dimens.minTapTarget,
                        contentDescription = "Play model answer"
                    )
                }
                Text(
                    text = feedback.band8ModelResponseEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                if (feedback.band8ModelResponseFa.isNotBlank()) {
                    PersianContentRtl {
                        Text(
                            text = feedback.band8ModelResponseFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun IeltsSpeakingHistoryCard(
    sessions: List<IeltsSpeakingSessionRecord>,
    onClose: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    AppCard(
        shape = RoundedCornerShape(Dimens.radiusLg),
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        SectionHeader(
            title = "Previous speaking sessions (${sessions.size})",
            actionText = "Close",
            onActionClick = onClose
        )
        sessions.take(4).forEach { session ->
            AppInset(
                padding = Dimens.cardPaddingTight,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                    ) {
                        Text(
                            text = session.topicEn,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        PersianContentRtl {
                            Text(
                                text = session.topicFa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = dateFormat.format(Date(session.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TagChip(
                        text = "Band ${String.format(Locale.US, "%.1f", session.overallBand)}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
