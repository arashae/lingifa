package com.example.ui.screens.vocab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.MinimalStreakCard
import com.example.ui.components.PersianRtlLayout
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
    val streakInfo by viewModel.streakInfo.collectAsState()

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

    PersianRtlLayout {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "مسیرهای یادگیری واژگان آزمون‌ها",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "آیلتس، تافل و جی‌آر‌ای؛ مرحله‌به‌مرحله و روزانه",
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
                                contentDescription = "بازگشت"
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
                    // Minimal Streak Card
                    MinimalStreakCard(
                        streakInfo = streakInfo,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Track Switcher Tabs (IELTS, TOEFL, GRE)
                    ExamTrackSelector(
                        selectedTrack = uiState.selectedTrack,
                        onSelect = { viewModel.selectTrack(it) }
                    )

                    // Track Daily Progress & Overall Completion Card
                    ExamTrackOverviewCard(
                        state = trackState,
                        onSetGoalClick = { showGoalDialog = true }
                    )

                    // Stage-by-Stage Curriculum List
                    Text(
                        text = "مراحل یادگیری و سرفصل‌های ${trackState.trackType.titleFa}:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    trackState.stages.forEach { stage ->
                        ExamStageCard(
                            stage = stage,
                            trackColor = Color(trackState.trackType.colorHex),
                            onStartStudy = { viewModel.startStudyingStage(stage) }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // Interactive Study Session Modal/Viewer
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
                    text = "تعیین هدف روزانه واژگان",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "چند لغت در روز مایلید در مسیر ${trackState.trackType.titleFa} یاد بگیرید؟",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(5, 10, 15, 20).forEach { goal ->
                            FilterChip(
                                selected = trackState.dailyGoalWords == goal,
                                onClick = {
                                    viewModel.setDailyGoal(goal)
                                    showGoalDialog = false
                                },
                                label = { Text("$goal واژه", fontSize = 12.sp) },
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
                    Text("انصراف")
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

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Color(selectedTrack.colorHex),
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = Color(selectedTrack.colorHex)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        tracks.forEach { track ->
            val isSelected = selectedTrack == track
            Tab(
                selected = isSelected,
                onClick = { onSelect(track) },
                text = {
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
                            tint = if (isSelected) Color(track.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = track.name,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isSelected) Color(track.colorHex) else MaterialTheme.colorScheme.onSurfaceVariant
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, trackColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Track title + Daily Goal button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.trackType.titleFa,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.trackType.descriptionFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = trackColor.copy(alpha = 0.12f),
                    modifier = Modifier.clickable { onSetGoalClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = trackColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "هدف: ${state.dailyGoalWords} لغت/روز",
                            color = trackColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Daily Progress Section
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
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
                                text = "پیشرفت امروز:",
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
                                        text = "هدف روزانه محقق شد ✓",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "${state.wordsStudiedToday} از ${state.dailyGoalWords} واژه (${state.dailyProgressPercentage}٪)",
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
                        text = "پیشرفت کل دوره:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${state.totalWordsLearned} از ${state.totalWordsInTrack} واژه مسلط (${state.overallPercentage}٪)",
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (stage.isCompleted) SuccessGreen.copy(alpha = 0.4f)
            else if (stage.isCurrent) trackColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            text = "مرحله ${stage.stageNumber}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                        !stage.isUnlocked -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        stage.progressPercentage > 0 -> AccentGold.copy(alpha = 0.15f)
                        else -> trackColor.copy(alpha = 0.1f)
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
                                !stage.isUnlocked -> Icons.Default.Lock
                                else -> Icons.Default.PlayArrow
                            },
                            contentDescription = null,
                            tint = when {
                                stage.isCompleted -> SuccessGreen
                                !stage.isUnlocked -> MaterialTheme.colorScheme.outline
                                stage.progressPercentage > 0 -> AccentGold
                                else -> trackColor
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = when {
                                stage.isCompleted -> "تکمیل شد"
                                !stage.isUnlocked -> "قفل شده"
                                stage.progressPercentage > 0 -> "${stage.progressPercentage}٪"
                                else -> "آماده شروع"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                stage.isCompleted -> SuccessGreen
                                !stage.isUnlocked -> MaterialTheme.colorScheme.outline
                                stage.progressPercentage > 0 -> AccentGold
                                else -> trackColor
                            }
                        )
                    }
                }
            }

            // Titles
            Column {
                Text(
                    text = stage.titleFa,
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
                        text = "${stage.masteredCount} از ${stage.totalCount} کلمه مسلط",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stage.progressPercentage}٪",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stage.isCompleted) SuccessGreen else trackColor
                    )
                }

                LinearProgressIndicator(
                    progress = { stage.progressPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
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
                    text = if (stage.isCompleted) "مرور مجدد لغات این مرحله" else "شروع تمرین مرحله (${stage.totalCount} واژه)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
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
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header: Stage Title, Word Index, Close Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مرحله ${stage.stageNumber}: ${stage.targetScoreFa}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "واژه ${wordIndex + 1} از ${stage.words.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن")
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
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
                                        contentDescription = "تلفظ صوتی",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // English Example Sentence
                        Text(
                            text = "\"${word.exampleEn}\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Flipped content: Persian meaning, example translation, tips
                        if (isCardFlipped) {
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
                                        text = "معنی فارسی: ${word.persianMeaning}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "ترجمه مثال: ${word.exampleFa}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
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
                            Text(
                                text = "👆 برای دیدن ترجمه فارسی، کالوکیشن‌ها و نکات امتحانی کلیک کنید",
                                fontSize = 11.sp,
                                color = PrimaryBlue,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Action Buttons: Review vs Mastered
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
                        Text("نیاز به مرور", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("بلدم (تسلط)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
