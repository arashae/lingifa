package com.example.ui.screens.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SuccessGreen

@Composable
fun PracticeScreen(
    onNavigateToReview: () -> Unit,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToWriting: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    onNavigateToTutor: () -> Unit
) {
    EnglishLtrLayout {
        Scaffold(
            topBar = { LinguaTopAppBar(title = "Practice Hub") },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Targeted Practice", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Specialized tools designed for each language skill.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    PracticeCard(
                        title = "Smart SRS Review",
                        description = "Spaced repetition flashcards, memory recall and active review",
                        icon = Icons.Default.Timer,
                        accent = PrimaryBlue,
                        label = "SRS",
                        onClick = onNavigateToReview
                    )
                }
                item {
                    PracticeCard(
                        title = "Speaking Simulator",
                        description = "Interactive IELTS interview simulation with pronunciation feedback",
                        icon = Icons.Default.RecordVoiceOver,
                        accent = SecondaryTeal,
                        label = "IELTS",
                        onClick = onNavigateToSpeaking
                    )
                }
                item {
                    PracticeCard(
                        title = "Writing Grader",
                        description = "Task 1 & Task 2 essay evaluation with Cambridge IELTS scoring",
                        icon = Icons.Default.EditNote,
                        accent = AccentGold,
                        label = "TASK 1 · 2",
                        onClick = onNavigateToWriting
                    )
                }
                item {
                    PracticeCard(
                        title = "AI Language Tutor",
                        description = "Instant answers to grammar questions, vocab usage, and nuance differences",
                        icon = Icons.Default.Psychology,
                        accent = PrimaryBlue,
                        label = "AI",
                        onClick = onNavigateToTutor
                    )
                }
                item {
                    PracticeCard(
                        title = "Level Placement",
                        description = "Assess current CEFR level and personalize study tracks",
                        icon = Icons.Default.FitnessCenter,
                        accent = SuccessGreen,
                        label = "CEFR",
                        onClick = onNavigateToDiagnostic
                    )
                }
                item {
                    PracticeCard(
                        title = "Mistake Notebook",
                        description = "Review past errors and turn weaknesses into strengths",
                        icon = Icons.Default.Warning,
                        accent = ErrorRed,
                        label = "REVIEW",
                        onClick = onNavigateToMistakes
                    )
                }
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                "Daily Habit: Complete at least one focused session every day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = accent.copy(alpha = 0.1f)) {
                        Text(
                            label,
                            color = accent,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
