package com.example.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.seed.ReadingListeningSeed
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun ListeningDetailScreen(
    exerciseId: String,
    viewModel: LearnViewModel,
    onBack: () -> Unit
) {
    val exercise = remember(exerciseId) {
        ReadingListeningSeed.getListeningExercises().find { it.id == exerciseId }
            ?: ReadingListeningSeed.getListeningExercises().first()
    }

    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    var isPlaying by remember { mutableStateOf(false) }
    var showTranscript by remember { mutableStateOf(false) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var checkedAnswers by remember { mutableStateOf(mutableMapOf<String, Boolean>()) }

    DisposableEffect(tts) {
        onDispose {
            tts.shutdown()
        }
    }

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = exercise.titleEn,
                    onBack = {
                        tts.stop()
                        onBack()
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Audio Player Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = exercise.titleEn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            )
                            CefrBadge(level = exercise.level)
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Big Play / Pause Audio Button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                                .clickable {
                                    if (isPlaying) {
                                        tts.stop()
                                        isPlaying = false
                                    } else {
                                        tts.speak(exercise.audioScriptEn)
                                        isPlaying = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Stop" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (isPlaying) "Audio is playing..." else "Tap to listen to audio passage",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle Transcript Button
                        OutlinedButton(
                            onClick = { showTranscript = !showTranscript },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showTranscript) "Hide Transcript" else "Show Transcript")
                        }
                    }
                }

                // Transcript Card
                if (showTranscript) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "English Transcript:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = exercise.audioScriptEn,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Persian Translation:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exercise.transcriptFa,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)
                            )
                        }
                    }
                }

                // Comprehension Questions
                if (exercise.questions.isNotEmpty()) {
                    Text(
                        text = "Listening Comprehension Questions:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    exercise.questions.forEachIndexed { qIdx, q ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Question ${qIdx + 1}: ${q.questionEn}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val selected = userAnswers[q.id]
                                val isChecked = checkedAnswers[q.id] == true

                                q.options.forEachIndexed { optIdx, optText ->
                                    val isThisSelected = selected == optIdx
                                    val isCorrect = optIdx == q.correctIndex

                                    val bg = when {
                                        !isChecked -> if (isThisSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        isCorrect -> Color(0xFFDCFCE7)
                                        isThisSelected -> Color(0xFFFEE2E2)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Surface(
                                        color = bg,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable(enabled = !isChecked) {
                                                val m = userAnswers.toMutableMap()
                                                m[q.id] = optIdx
                                                userAnswers = m
                                            }
                                    ) {
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isChecked && isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isThisSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                if (!isChecked && selected != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val m = checkedAnswers.toMutableMap()
                                            m[q.id] = true
                                            checkedAnswers = m
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                    ) {
                                        Text("Check Answer", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (isChecked) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Explanation: ${q.explanationFa}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
