package com.example.ui.screens.exams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.TtsManager
import com.example.data.model.IeltsSpeakingFeedback
import com.example.data.model.IeltsSpeakingPrompt
import com.example.data.model.IeltsSpeakingSessionRecord
import com.example.ui.components.MinimalStreakCard
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            ieltsViewModel.clearStatusMessage()
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
                                text = "شبیه‌ساز آزمون اسپیکینگ آیلتس",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "ممتحن هوش مصنوعی با بازخورد فارسی و تحلیل تلفظ",
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
                    actions = {
                        // Average score badge
                        avgBand?.let { score ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryBlue.copy(alpha = 0.12f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "Band ${String.format(Locale.US, "%.1f", score)}",
                                    color = PrimaryBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // History icon
                        IconButton(onClick = { showHistorySheet = !showHistorySheet }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "تاریخچه آزمون‌ها",
                                tint = if (showHistorySheet) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Streak Card
                MinimalStreakCard(
                    streakInfo = streakInfo,
                    modifier = Modifier.fillMaxWidth()
                )

                // History Toggle View
                if (showHistorySheet && pastSessions.isNotEmpty()) {
                    IeltsSpeakingHistoryCard(
                        sessions = pastSessions,
                        onClose = { showHistorySheet = false }
                    )
                }

                // IELTS Speaking Parts Selector
                TabRow(
                    selectedTabIndex = state.selectedPart - 1,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PrimaryBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[state.selectedPart - 1]),
                            color = PrimaryBlue
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = state.selectedPart == 1,
                        onClick = { ieltsViewModel.selectPart(1) },
                        text = {
                            Text(
                                text = "Part 1 (مصاحبه)",
                                fontWeight = if (state.selectedPart == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = state.selectedPart == 2,
                        onClick = { ieltsViewModel.selectPart(2) },
                        text = {
                            Text(
                                text = "Part 2 (کیو کارت)",
                                fontWeight = if (state.selectedPart == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                    Tab(
                        selected = state.selectedPart == 3,
                        onClick = { ieltsViewModel.selectPart(3) },
                        text = {
                            Text(
                                text = "Part 3 (بحث تحلیلی)",
                                fontWeight = if (state.selectedPart == 3) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                // Examiner Question & Cue Card Container
                IeltsCueCardView(
                    prompt = state.currentPrompt,
                    isExaminerSpeaking = state.isExaminerSpeaking,
                    isUkAccent = state.isUkAccent,
                    onToggleAccent = { ieltsViewModel.toggleAccent() },
                    onPlayAudio = { ieltsViewModel.playExaminerPrompt(tts) },
                    onGenerateAiPrompt = { topic -> ieltsViewModel.generateAiPrompt(topic) }
                )

                // Preparation & Speaking Timers
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

                // Candidate Spoken Transcript & Evaluation Action
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "پاسخ گفتاری شما (Speaking Response):",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            // Populate sample button
                            Text(
                                text = "نمونه آزمایشی",
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryBlue.copy(alpha = 0.08f))
                                    .clickable { ieltsViewModel.populateSampleCandidateAnswer() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        OutlinedTextField(
                            value = state.candidateTranscript,
                            onValueChange = { ieltsViewModel.onTranscriptChanged(it) },
                            placeholder = {
                                Text(
                                    text = if (state.isRecording) "در حال ضبط صحبت‌های شما... بگویید و سپس متن را بررسی فرمایید." else "متن پاسخ خود را بگویید یا در این کادر بنویسید..."
                                )
                            },
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        )

                        // Word count and pacing hint
                        val wordCount = state.candidateTranscript.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "تعداد کلمات: $wordCount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val targetWords = if (state.selectedPart == 2) "۱۵۰ تا ۲۲۰ کلمه برای ۲ دقیقه" else "۴۰ تا ۷۰ کلمه"
                            Text(
                                text = "هدف پیشنهادی: $targetWords",
                                fontSize = 11.sp,
                                color = if (wordCount >= 60) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Evaluate Button
                        Button(
                            onClick = { ieltsViewModel.evaluateSpeakingResponse() },
                            enabled = state.candidateTranscript.isNotBlank() && !state.isEvaluating,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (state.isEvaluating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال ارزیابی با ۴ معیار رسمی آیلتس...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ارزیابی هوشمند و تحلیل کارنامه ممتحن (+۳۵ XP)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Real-time Comprehensive Feedback Dashboard
                state.feedback?.let { feedback ->
                    IeltsSpeakingFeedbackDashboard(
                        feedback = feedback,
                        isModelSpeaking = state.isModelSpeaking,
                        onPlayModelAudio = { ieltsViewModel.playModelAnswer(tts) }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IeltsCueCardView(
    prompt: IeltsSpeakingPrompt,
    isExaminerSpeaking: Boolean,
    isUkAccent: Boolean,
    onToggleAccent: () -> Unit,
    onPlayAudio: () -> Unit,
    onGenerateAiPrompt: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Topic, Part Badge, Examiner Audio
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
                        color = PrimaryBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Part ${prompt.part}",
                            color = PrimaryBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = prompt.topicFa,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Accent Toggle
                    FilterChip(
                        selected = isUkAccent,
                        onClick = onToggleAccent,
                        label = { Text(if (isUkAccent) "UK" else "US", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        )
                    )

                    // Play audio button
                    IconButton(
                        onClick = onPlayAudio,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isExaminerSpeaking) PrimaryBlue else PrimaryBlue.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = if (isExaminerSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "پخش سوال ممتحن",
                            tint = if (isExaminerSpeaking) Color.White else PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Question Text
            Text(
                text = prompt.questionEn,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Persian Translation
            Text(
                text = prompt.questionFa,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Part 2 Cue Card Bullet Points Box
            if (prompt.part == 2 && prompt.cueCardBulletPoints.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "You should say:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryBlue
                        )
                        prompt.cueCardBulletPoints.forEach { point ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("•", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                Text(
                                    text = point,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Recommended Vocab & Collocations
            if (prompt.recommendedVocab.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "واژگان و کالوکیشن‌های پیشنهادی برای نمره ۷.۵+:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        prompt.recommendedVocab.forEach { vocab ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SuccessGreen.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = vocab.word,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = SuccessGreen
                                    )
                                    Text(
                                        text = "(${vocab.meaningFa})",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AI Topic Generators Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "تولید با هوش مصنوعی:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7C3AED)
                )
                listOf("Environment", "Artificial Intelligence", "Higher Education", "Urbanization", "Hometown").forEach { topic ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.08f),
                        modifier = Modifier.clickable { onGenerateAiPrompt(topic) }
                    ) {
                        Text(
                            text = topic,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Preparation Timer (primarily for Part 2)
        if (part == 2) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPrepRunning) AccentGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "زمان آماده‌سازی (۱ دقیقه)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = String.format(Locale.US, "00:%02d", prepSeconds),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (prepSeconds <= 10) Color(0xFFEF4444) else AccentGold
                    )

                    LinearProgressIndicator(
                        progress = { prepSeconds / 60f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentGold,
                        trackColor = AccentGold.copy(alpha = 0.2f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = onTogglePrep,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isPrepRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "آماده‌سازی",
                                tint = AccentGold
                            )
                        }
                        IconButton(
                            onClick = onResetPrep,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "ریست",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Speaking Duration & Live Recording Pulse
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = if (isRecording) 1.25f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )

            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "زمان صحبت شما",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val mins = speakingSeconds / 60
                val secs = speakingSeconds % 60
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isRecording) Color(0xFFEF4444) else PrimaryBlue
                )

                // Recording mic button
                IconButton(
                    onClick = onToggleRecording,
                    modifier = Modifier
                        .size(42.dp)
                        .scale(if (isRecording) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(if (isRecording) Color(0xFFEF4444) else PrimaryBlue)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "ضبط صحبت",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = if (isRecording) "در حال ضبط... (برای توقف کلیک کنید)" else "برای شروع ضبط کلیک کنید",
                    fontSize = 9.sp,
                    color = if (isRecording) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IeltsSpeakingFeedbackDashboard(
    feedback: IeltsSpeakingFeedback,
    isModelSpeaking: Boolean,
    onPlayModelAudio: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Score Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "کارنامه و نمره تخمینی ممتحن آیلتس",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "بر اساس ۴ معیار ارزیابی استاندارد کمبریج",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Estimated Overall Band badge
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PrimaryBlue,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Band",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", feedback.overallBand),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // 4 Criteria Breakdown Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IeltsCriterionScoreCard(
                        titleEn = "Fluency & Coherence",
                        titleFa = "روانی و پیوستگی کلام",
                        score = feedback.fluencyScore,
                        feedback = feedback.fluencyFeedbackFa,
                        modifier = Modifier.weight(1f)
                    )
                    IeltsCriterionScoreCard(
                        titleEn = "Lexical Resource",
                        titleFa = "دامنه و تنوع واژگان",
                        score = feedback.lexicalScore,
                        feedback = feedback.lexicalFeedbackFa,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IeltsCriterionScoreCard(
                        titleEn = "Grammatical Range",
                        titleFa = "تنوع و دقت گرامری",
                        score = feedback.grammarScore,
                        feedback = feedback.grammarFeedbackFa,
                        modifier = Modifier.weight(1f)
                    )
                    IeltsCriterionScoreCard(
                        titleEn = "Pronunciation",
                        titleFa = "تلفظ و آهنگ کلام",
                        score = feedback.pronunciationScore,
                        feedback = feedback.pronunciationHintsFa,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Persian-Speaker Specific Pronunciation & Pitfalls Card
            if (feedback.persianLearnerMistakes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "اصلاح اشتباهات متداول زبان‌آموزان ایرانی:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                        }

                        feedback.persianLearnerMistakes.forEach { mistake ->
                            Text(
                                text = "• $mistake",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Vocabulary Upgrades
            if (feedback.vocabUpgrades.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ارتقای واژگان شما به نمره بالای ۸ (Vocabulary Upgrades):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    feedback.vocabUpgrades.forEach { upgrade ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = upgrade.original,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text("➔", fontSize = 12.sp)
                                        Text(
                                            text = upgrade.upgraded,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessGreen
                                        )
                                    }
                                    Text(
                                        text = upgrade.explanationFa,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Band 8+ Cambridge Model Answer with Audio
            if (feedback.band8ModelResponseEn.isNotBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryBlue.copy(alpha = 0.05f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f)),
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
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "نمونه پاسخ استاندارد ممتحن (Band 8.5 Model Answer):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryBlue
                                )
                            }

                            IconButton(
                                onClick = onPlayModelAudio,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isModelSpeaking) Icons.Default.GraphicEq else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "پخش صوتی نمونه پاسخ",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = feedback.band8ModelResponseEn,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (feedback.band8ModelResponseFa.isNotBlank()) {
                            Text(
                                text = feedback.band8ModelResponseFa,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IeltsCriterionScoreCard(
    titleEn: String,
    titleFa: String,
    score: Float,
    feedback: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleFa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = String.format(Locale.US, "%.1f", score),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = PrimaryBlue
                )
            }
            Text(
                text = titleEn,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = feedback,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3
            )
        }
    }
}

@Composable
private fun IeltsSpeakingHistoryCard(
    sessions: List<IeltsSpeakingSessionRecord>,
    onClose: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آزمون‌های اسپیکینگ پیشین در دیتابیس (${sessions.size} مورد):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "بستن ✕",
                    fontSize = 11.sp,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable { onClose() }
                )
            }

            sessions.take(4).forEach { session ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Part ${session.part}: ${session.topicFa}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = dateFormat.format(Date(session.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PrimaryBlue.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Band ${String.format(Locale.US, "%.1f", session.overallBand)}",
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
