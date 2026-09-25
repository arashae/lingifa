package com.example.ui.screens.mistakes

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MistakeRecord
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun MistakeNotebookScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedSkillFilter by remember { mutableStateOf("All") }
    var selectedStatusFilter by remember { mutableStateOf("All") }

    val skills = listOf("All", "VOCABULARY", "GRAMMAR", "READING", "LISTENING", "SPEAKING", "WRITING")
    val statuses = listOf("All", "Needs Review", "Resolved")

    val filteredMistakes = state.mistakes.filter { mistake ->
        val matchesSkill = selectedSkillFilter == "All" || mistake.skillType.equals(selectedSkillFilter, ignoreCase = true)
        val matchesStatus = when (selectedStatusFilter) {
            "Needs Review" -> !mistake.isReviewed
            "Resolved" -> mistake.isReviewed
            else -> true
        }
        matchesSkill && matchesStatus
    }

    EnglishLtrLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "Mistake Notebook",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Header Note
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart Error Pattern Analysis",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ErrorRed)
                            )
                            Text(
                                text = "Mistakes from reviews and exercises are recorded for targeted spaced repetition.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onErrorContainer)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Skill Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    skills.forEach { sk ->
                        val label = when (sk) {
                            "VOCABULARY" -> "Vocabulary"
                            "GRAMMAR" -> "Grammar"
                            "READING" -> "Reading"
                            "LISTENING" -> "Listening"
                            "SPEAKING" -> "Speaking"
                            "WRITING" -> "Writing"
                            else -> "All Skills"
                        }
                        FilterChip(
                            selected = selectedSkillFilter == sk,
                            onClick = { selectedSkillFilter = sk },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Review Status Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    statuses.forEach { st ->
                        FilterChip(
                            selected = selectedStatusFilter == st,
                            onClick = { selectedStatusFilter = st },
                            label = { Text(st, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mistakes List
                if (filteredMistakes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No errors recorded in this section!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredMistakes, key = { it.id }) { mistake ->
                            MistakeCard(
                                mistake = mistake,
                                onDelete = { viewModel.deleteMistake(mistake.id) },
                                onToggleReviewed = { viewModel.markMistakeReviewed(mistake.id, !mistake.isReviewed) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeCard(
    mistake: MistakeRecord,
    onDelete: () -> Unit,
    onToggleReviewed: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mistake.isReviewed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = mistake.skillType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (mistake.isReviewed) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Resolved ✓",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Needs Review",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD97706)),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleReviewed, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = if (mistake.isReviewed) "Mark as needs review" else "Mark as resolved",
                            tint = if (mistake.isReviewed) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete error", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mistake.question,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // My Answer vs Correct Answer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("My Answer:", style = MaterialTheme.typography.labelSmall.copy(color = ErrorRed))
                        Text(mistake.myAnswer, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = ErrorRed))
                    }
                }

                Surface(
                    color = Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Correct Answer:", style = MaterialTheme.typography.labelSmall.copy(color = SuccessGreen))
                        Text(mistake.correctAnswer, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen))
                    }
                }
            }

            if (mistake.explanationFa.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explanation: ${mistake.explanationFa}",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}
