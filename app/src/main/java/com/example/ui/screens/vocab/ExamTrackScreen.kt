package com.example.ui.screens.vocab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TtsManager
import com.example.data.model.ExamTrackStage
import com.example.data.model.ExamTrackState
import com.example.data.model.ExamTrackType
import com.example.data.model.ExamWordItem
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamTrackScreen(
    onBack: () -> Unit,
    viewModel: ExamTrackViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val trackState by viewModel.currentTrackState.collectAsState()

    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var showGoalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Exam Mastery Tracks",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "IELTS • TOEFL • GRE • Complete Vocabulary Bank",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Track Switcher Tabs (IELTS, TOEFL, GRE)
                    ExamTrackSelector(
                        selectedTrack = uiState.selectedTrack,
                        onSelect = { viewModel.selectTrack(it) }
                    )

                    // Track Overview & Stats Card
                    ExamTrackOverviewCard(
                        state = trackState,
                        onSetGoalClick = { showGoalDialog = true }
                    )

                    // Section Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${trackState.trackType.name} Curriculum Stages",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "4 Progressive Stages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Stages List
                    trackState.stages.forEach { stage ->
                        ExamStageCard(
                            stage = stage,
                            trackColor = Color(trackState.trackType.colorHex),
                            onStartStudy = { viewModel.startStudyingStage(stage) }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Interactive Study Session Modal
                uiState.activeStudyStage?.let { stage ->
                    InteractiveStageStudyDialog(
                        stage = stage,
                        wordIndex = uiState.studyWordIndex,
                        isCardFlipped = uiState.isCardFlipped,
                        isUkAccent = uiState.isUkAccent,
                        isPlayingAudio = uiState.isPlayingAudio,
                        onFlip = { viewModel.flipCard() },
                        onToggleAccent = { viewModel.toggleAccent() },
                        onPlayAudio = { text -> viewModel.playAudio(tts, text) },
                        onRecordResult = { isMastered -> viewModel.recordWordResult(isMastered) },
                        onClose = { viewModel.closeStudySession() }
                    )
                }
            }
        }
    }

    // Daily Goal Dialog
    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    text = "Set Daily Vocabulary Goal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "How many words would you like to master each day in ${trackState.trackType.name}?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(5, 10, 15, 20, 25).forEach { goal ->
                            FilterChip(
                                selected = trackState.dailyGoalWords == goal,
                                onClick = {
                                    viewModel.setDailyGoal(goal)
                                    showGoalDialog = false
                                },
                                label = { Text("$goal words", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(trackState.trackType.colorHex),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Close")
                }
            }
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
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (track) {
                                    ExamTrackType.IELTS -> Icons.Default.School
                                    ExamTrackType.TOEFL -> Icons.Default.MenuBook
                                    ExamTrackType.GRE -> Icons.Default.Psychology
                                },
                                contentDescription = null,
                                tint = if (isSelected) trackColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = track.name,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (isSelected) trackColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = wordTarget,
                            fontSize = 10.sp,
                            color = if (isSelected) trackColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
        ExamTrackType.IELTS -> "Band 6.5 - 9.0 Target"
        ExamTrackType.TOEFL -> "Score 80 - 120 Target"
        ExamTrackType.GRE -> "Verbal 150 - 170 Target"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, trackColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Track Title + Goal Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${state.trackType.name} Master Bank",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = scoreBand,
                        style = MaterialTheme.typography.bodySmall,
                        color = trackColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = trackColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, trackColor.copy(alpha = 0.25f)),
                    modifier = Modifier.clickable { onSetGoalClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = trackColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${state.dailyGoalWords}/day",
                            color = trackColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            tint = trackColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            // Daily Progress Section
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Today's Study Session",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.isDailyGoalMet) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Goal Reached ✓",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${state.wordsStudiedToday} of ${state.dailyGoalWords} words",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = trackColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { state.dailyProgressPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (state.isDailyGoalMet) SuccessGreen else trackColor,
                        trackColor = trackColor.copy(alpha = 0.15f)
                    )
                }
            }

            // Overall Track Completion Metric
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = trackColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Total Track Mastery:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${state.totalWordsLearned} / ${state.totalWordsInTrack} words (${state.overallPercentage}%)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = stage.isUnlocked) { onStartStudy() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (stage.isCompleted) SuccessGreen.copy(alpha = 0.4f)
            else if (stage.isCurrent) trackColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Stage Header: Title + Target Score Badge + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (stage.isCompleted) SuccessGreen else trackColor
                    ) {
                        Text(
                            text = "Stage ${stage.stageNumber}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = stage.targetScoreFa,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        stage.isCompleted -> SuccessGreen.copy(alpha = 0.12f)
                        stage.progressPercentage > 0 -> AccentGold.copy(alpha = 0.15f)
                        else -> trackColor.copy(alpha = 0.10f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                stage.isCompleted -> Icons.Default.CheckCircle
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = when {
                                stage.isCompleted -> SuccessGreen
                                stage.progressPercentage > 0 -> AccentGold
                                else -> trackColor
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                stage.isCompleted -> "Completed"
                                stage.progressPercentage > 0 -> "${stage.progressPercentage}% Done"
                                else -> "Ready"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                stage.isCompleted -> SuccessGreen
                                stage.progressPercentage > 0 -> AccentGold
                                else -> trackColor
                            }
                        )
                    }
                }
            }

            // Titles
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stage.titleEn,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stage.subtitleFa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Progress Bar & Count
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stage.masteredCount} of ${stage.totalCount} words mastered",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stage.progressPercentage}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stage.isCompleted) SuccessGreen else trackColor
                    )
                }

                LinearProgressIndicator(
                    progress = { stage.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (stage.isCompleted) SuccessGreen else trackColor,
                    trackColor = trackColor.copy(alpha = 0.12f)
                )
            }

            // Action Button
            Button(
                onClick = onStartStudy,
                enabled = stage.isUnlocked,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (stage.isCompleted) SuccessGreen else trackColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (stage.isCompleted) Icons.Default.Replay else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (stage.isCompleted) "Review Stage Words" else "Start Practice (${stage.totalCount} words)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
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
    onFlip: () -> Unit,
    onToggleAccent: () -> Unit,
    onPlayAudio: (String) -> Unit,
    onRecordResult: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val word = stage.words.getOrNull(wordIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { /* prevent click through */ },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Stage Title, Progress, Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Stage ${stage.stageNumber} • ${stage.titleEn}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Card ${wordIndex + 1} of ${stage.words.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Flashcard Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFlip() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Word & Audio pronunciation row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = word.word,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${word.phonetic} • ${word.partOfSpeech}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = isUkAccent,
                                    onClick = onToggleAccent,
                                    label = { Text(if (isUkAccent) "UK" else "US", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                IconButton(
                                    onClick = { onPlayAudio(word.word) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingAudio) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Audio pronunciation",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // English Example
                        if (word.exampleEn.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "“${word.exampleEn}”",
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Flipped / Revealed Section (Persian Translation & Academic Tips)
                        if (isCardFlipped) {
                            // Persian Meaning Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryBlue.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Persian: ${word.persianMeaning}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (word.exampleFa.isNotBlank()) {
                                        Text(
                                            text = "Translation: ${word.exampleFa}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    if (word.englishDefinition.isNotBlank()) {
                                        Text(
                                            text = "Definition: ${word.englishDefinition}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Collocations & Synonyms
                            if (word.collocations.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    word.collocations.forEach { col ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = SuccessGreen.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = col,
                                                color = SuccessGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Exam Tip
                            if (word.examTipFa.isNotBlank() || word.iranianMistakeFa.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentGold.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = AccentGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = word.examTipFa.ifEmpty { word.iranianMistakeFa },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 16.sp
                                        )
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
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tap to reveal Persian meaning, collocations & tips",
                                    fontSize = 11.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Need Review vs Mastered
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onRecordResult(false) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Need Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onRecordResult(true) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mastered ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
