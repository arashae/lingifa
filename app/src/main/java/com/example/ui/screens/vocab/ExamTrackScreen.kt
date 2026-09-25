package com.example.ui.screens.vocab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TtsManager
import com.example.data.model.ExamTrackStage
import com.example.data.model.ExamTrackState
import com.example.data.model.ExamTrackType
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.AudioSpeakerButton
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianContentRtl
import com.example.ui.components.SectionHeader
import com.example.ui.components.SelectChip
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamTrackScreen(
    onBack: () -> Unit,
    viewModel: ExamTrackViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trackState by viewModel.currentTrackState.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showGoalDialog by remember { mutableStateOf(false) }

    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStatusMessage()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LinguaTopAppBar(
                    title = "Exam Mastery Tracks",
                    subtitle = "IELTS · TOEFL · GRE · Complete vocabulary bank",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Dimens.screenGutter, vertical = Dimens.space8),
                    verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
                ) {
                    ExamTrackSelector(
                        selectedTrack = uiState.selectedTrack,
                        onSelect = viewModel::selectTrack
                    )
                    ExamTrackOverviewCard(
                        state = trackState,
                        onSetGoalClick = { showGoalDialog = true }
                    )
                    SectionHeader(
                        title = "${trackState.trackType.name} curriculum stages",
                        subtitle = "Four progressive stages"
                    )
                    trackState.stages.forEach { stage ->
                        ExamStageCard(
                            stage = stage,
                            trackColor = Color(trackState.trackType.colorHex),
                            onStartStudy = { viewModel.startStudyingStage(stage) }
                        )
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    text = "Set daily vocabulary goal",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
                    Text(
                        text = "How many words would you like to master each day in ${trackState.trackType.name}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space6)
                    ) {
                        listOf(5, 10, 15, 20, 25).forEach { goal ->
                            SelectChip(
                                text = "$goal words",
                                selected = trackState.dailyGoalWords == goal,
                                onClick = {
                                    viewModel.setDailyGoal(goal)
                                    showGoalDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showGoalDialog = false },
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTapTarget)
                ) {
                    Text("Close")
                }
            }
        )
    }

    uiState.activeStudyStage?.let { stage ->
        InteractiveStageStudyDialog(
            stage = stage,
            wordIndex = uiState.studyWordIndex,
            isCardFlipped = uiState.isCardFlipped,
            isUkAccent = uiState.isUkAccent,
            isPlayingAudio = uiState.isPlayingAudio,
            trackColor = Color(trackState.trackType.colorHex),
            onFlip = viewModel::flipCard,
            onToggleAccent = viewModel::toggleAccent,
            onPlayAudio = { text -> viewModel.playAudio(tts, text) },
            onRecordResult = viewModel::recordWordResult,
            onClose = viewModel::closeStudySession
        )
    }
}

@Composable
private fun ExamTrackSelector(
    selectedTrack: ExamTrackType,
    onSelect: (ExamTrackType) -> Unit
) {
    val tracks = ExamTrackType.values()
    val selectedIndex = tracks.indexOf(selectedTrack).coerceAtLeast(0)
    val activeColor = Color(selectedTrack.colorHex)

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = activeColor,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = activeColor
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusLg))
            .border(
                Dimens.hairline,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(Dimens.radiusLg)
            )
    ) {
        tracks.forEach { track ->
            val isSelected = selectedTrack == track
            val trackColor = Color(track.colorHex)
            val wordTarget = when (track) {
                ExamTrackType.IELTS -> "9k words"
                ExamTrackType.TOEFL -> "7k words"
                ExamTrackType.GRE -> "5k words"
            }

            Tab(
                selected = isSelected,
                onClick = { onSelect(track) },
                text = {
                    Column(
                        modifier = Modifier.padding(vertical = Dimens.space8),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                        ) {
                            Icon(
                                imageVector = when (track) {
                                    ExamTrackType.IELTS -> Icons.Default.School
                                    ExamTrackType.TOEFL -> Icons.Default.MenuBook
                                    ExamTrackType.GRE -> Icons.Default.Psychology
                                },
                                contentDescription = null,
                                tint = if (isSelected) trackColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(Dimens.iconSm)
                            )
                            Text(
                                text = track.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) trackColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = wordTarget,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) trackColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun ExamTrackOverviewCard(
    state: ExamTrackState,
    onSetGoalClick: () -> Unit
) {
    val trackColor = Color(state.trackType.colorHex)
    val scoreBand = when (state.trackType) {
        ExamTrackType.IELTS -> "Band 6.5–9.0 target"
        ExamTrackType.TOEFL -> "Score 80–120 target"
        ExamTrackType.GRE -> "Verbal 150–170 target"
    }

    AppCard(
        borderColor = trackColor.copy(alpha = 0.35f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    Text(
                        text = "${state.trackType.name} master bank",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = scoreBand,
                        style = MaterialTheme.typography.bodySmall,
                        color = trackColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = onSetGoalClick,
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTapTarget)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space4))
                    Text("${state.dailyGoalWords}/day")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                Text(
                    text = "Today's study session",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (state.isDailyGoalMet) {
                    TagChip(
                        text = "Goal reached",
                        containerColor = Accent.successSoft,
                        contentColor = Accent.successOnSoft
                    )
                }
                Text(
                    text = "${state.wordsStudiedToday}/${state.dailyGoalWords}",
                    style = MaterialTheme.typography.labelLarge,
                    color = trackColor,
                    maxLines = 1
                )
            }

            LinearProgressIndicator(
                progress = { state.dailyProgressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.progressHeight)
                    .clip(RoundedCornerShape(Dimens.radiusPill)),
                color = if (state.isDailyGoalMet) Accent.success else trackColor,
                trackColor = trackColor.copy(alpha = 0.15f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = trackColor,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Text(
                        text = "Total track mastery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${state.totalWordsLearned}/${state.totalWordsInTrack} · ${state.overallPercentage}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ExamStageCard(
    stage: ExamTrackStage,
    trackColor: Color,
    onStartStudy: () -> Unit
) {
    val statusText = when {
        stage.isCompleted -> "Completed"
        !stage.isUnlocked -> "Locked"
        stage.progressPercentage > 0 -> "${stage.progressPercentage}% done"
        else -> "Ready"
    }
    val statusColor = when {
        stage.isCompleted -> Accent.success
        stage.progressPercentage > 0 -> Accent.warning
        else -> trackColor
    }
    val borderColor = when {
        stage.isCompleted -> Accent.success.copy(alpha = 0.4f)
        stage.isCurrent -> trackColor.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    AppCard(
        borderColor = borderColor,
        onClick = if (stage.isUnlocked) onStartStudy else null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                TagChip(
                    text = "Stage ${stage.stageNumber}",
                    containerColor = if (stage.isCompleted) Accent.success else trackColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                Box(modifier = Modifier.weight(1f)) {
                    PersianContentRtl {
                        Text(
                            text = stage.targetScoreFa,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                TagChip(
                    text = statusText,
                    containerColor = statusColor.copy(alpha = 0.12f),
                    contentColor = statusColor
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.space2)) {
                Text(
                    text = stage.titleEn,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                PersianContentRtl {
                    Text(
                        text = stage.subtitleFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stage.masteredCount}/${stage.totalCount} words mastered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${stage.progressPercentage}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    maxLines = 1
                )
            }
            LinearProgressIndicator(
                progress = { stage.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.progressHeight)
                    .clip(RoundedCornerShape(Dimens.radiusPill)),
                color = if (stage.isCompleted) Accent.success else trackColor,
                trackColor = trackColor.copy(alpha = 0.12f)
            )

            Button(
                onClick = onStartStudy,
                enabled = stage.isUnlocked,
                shape = RoundedCornerShape(Dimens.radiusSm),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stage.isCompleted) Accent.success else trackColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.rowHeight)
            ) {
                Icon(
                    imageVector = if (stage.isCompleted) Icons.Default.Replay else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSm)
                )
                Spacer(modifier = Modifier.width(Dimens.space6))
                Text(
                    text = if (stage.isCompleted) "Review stage words" else "Start practice · ${stage.totalCount} words",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveStageStudyDialog(
    stage: ExamTrackStage,
    wordIndex: Int,
    isCardFlipped: Boolean,
    isUkAccent: Boolean,
    isPlayingAudio: Boolean,
    trackColor: Color,
    onFlip: () -> Unit,
    onToggleAccent: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onRecordResult: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val word = stage.words.getOrNull(wordIndex) ?: return

    Dialog(onDismissRequest = onClose) {
        AppCard(
            modifier = Modifier.fillMaxWidth(0.96f),
            padding = Dimens.cardPadding
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stage ${stage.stageNumber} · ${stage.titleEn}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Card ${wordIndex + 1} of ${stage.words.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.defaultMinSize(minHeight = Dimens.minTapTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close study session"
                        )
                    }
                }

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    padding = Dimens.cardPaddingTight,
                    borderColor = trackColor.copy(alpha = 0.35f),
                    onClick = onFlip
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${word.phonetic} · ${word.partOfSpeech}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            SelectChip(
                                text = if (isUkAccent) "UK" else "US",
                                selected = isUkAccent,
                                onClick = onToggleAccent
                            )
                            AudioSpeakerButton(
                                onClick = { onPlayAudio(word.word) },
                                size = Dimens.minTapTarget,
                                contentDescription = if (isPlayingAudio) {
                                    "Playing ${word.word} pronunciation"
                                } else {
                                    "Play ${word.word} pronunciation"
                                }
                            )
                        }

                        if (word.exampleEn.isNotBlank()) {
                            AppInset {
                                Text(
                                    text = "“${word.exampleEn}”",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isCardFlipped) {
                            AppInset(color = Accent.infoSoft, contentColor = Accent.infoOnSoft) {
                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.space4)) {
                                    Text(
                                        text = "Persian",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Accent.infoOnSoft
                                    )
                                    PersianContentRtl {
                                        Text(
                                            text = word.persianMeaning,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Accent.infoOnSoft,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (word.exampleFa.isNotBlank()) {
                                        Text(
                                            text = "Translation",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Accent.infoOnSoft
                                        )
                                        PersianContentRtl {
                                            Text(
                                                text = word.exampleFa,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Accent.infoOnSoft,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    if (word.englishDefinition.isNotBlank()) {
                                        Text(
                                            text = "Definition: ${word.englishDefinition}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Accent.infoOnSoft,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            if (word.collocations.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                                    verticalArrangement = Arrangement.spacedBy(Dimens.space6)
                                ) {
                                    word.collocations.forEach { collocation ->
                                        TagChip(
                                            text = collocation,
                                            containerColor = Accent.successSoft,
                                            contentColor = Accent.successOnSoft
                                        )
                                    }
                                }
                            }

                            if (word.examTipFa.isNotBlank() || word.iranianMistakeFa.isNotBlank()) {
                                AppInset(
                                    color = Accent.warningSoft,
                                    contentColor = Accent.warningOnSoft
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.space6),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = Accent.warning,
                                            modifier = Modifier.size(Dimens.iconSm)
                                        )
                                        PersianContentRtl {
                                            Text(
                                                text = word.examTipFa.ifEmpty { word.iranianMistakeFa },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Accent.warningOnSoft,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(Dimens.iconSm)
                                )
                                Spacer(modifier = Modifier.width(Dimens.space6))
                                Text(
                                    text = "Tap to reveal meaning, collocations, and tips",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                ) {
                    OutlinedButton(
                        onClick = { onRecordResult(false) },
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.rowHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSm)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space6))
                        Text(
                            text = "Need review",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = { onRecordResult(true) },
                        shape = RoundedCornerShape(Dimens.radiusSm),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent.success,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.rowHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSm)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space6))
                        Text(
                            text = "Mastered",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
