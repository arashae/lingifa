package com.example.ui.screens.vocab

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.data.model.VocabularyPack
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.theme.SuccessGreen

@Composable
fun VocabPacksScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit,
    onFilterByPack: (String) -> Unit,
    onStartCefrLevel: () -> Unit,
    onNavigateToExamTracks: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val syncStates by viewModel.packSyncStates.collectAsState()

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { LinguaTopAppBar(title = "Vocabulary Packs", onBack = onBack) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Exam & Study Packs",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Download once, then study and review offline anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    CefrLearningPathCard(
                        selectedLevel = state.learningLevel,
                        onSelectLevel = { level ->
                            viewModel.selectLearningLevel(level)
                            onStartCefrLevel()
                        }
                    )
                }

                item { ExamTracksEntryCard(onClick = onNavigateToExamTracks) }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("All Vocabulary Packs", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${state.packs.size} packs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(state.packs, key = { it.id }) { pack ->
                    VocabPackCard(
                        pack = pack,
                        syncState = syncStates[pack.id],
                        onSync = { viewModel.syncMasterPack(pack.id) },
                        onViewWords = {
                            viewModel.onPackFilterChanged(pack.id)
                            onFilterByPack(pack.id)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun CefrLearningPathCard(
    selectedLevel: String,
    onSelectLevel: (String) -> Unit
) {
    val levels = listOf(
        "A1" to "Beginner (Core Foundations)",
        "A2" to "Elementary (Daily Conversation)",
        "B1" to "Intermediate (Independent Learner)",
        "B2" to "Upper Intermediate (Fluency)",
        "C1" to "Advanced (Professional Proficiency)",
        "C2" to "Mastery (Native Fluency)"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "General Vocabulary Track",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "Start at your target CEFR level; words are curated in learning priority order.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
            )
            levels.forEach { (level, title) ->
                val isSelected = level == selectedLevel
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLevel(level) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    border = if (isSelected) null else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = level,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamTracksEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Exam Mastery Tracks",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "IELTS 9k · TOEFL 7k · GRE 5k",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VocabPackCard(
    pack: VocabularyPack,
    syncState: PackSyncUiState?,
    onSync: () -> Unit,
    onViewWords: () -> Unit
) {
    val target = maxOf(pack.targetWordCount, syncState?.target ?: 0).coerceAtLeast(0)
    val installed = maxOf(pack.installedWordCount, syncState?.installed ?: 0).coerceAtLeast(0)
    val progress = if (target > 0) (installed.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
    val isSyncing = syncState?.isRunning == true
    val isComplete = pack.isCorePack && target > 0 && installed >= target
    val statusMessage = syncState?.message?.takeIf { it.isNotBlank() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = pack.titleEn.ifBlank { pack.titleFa },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = pack.titleFa,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                CefrBadge(level = pack.level)
            }

            if (pack.descriptionEn.isNotBlank() || pack.descriptionFa.isNotBlank()) {
                Text(
                    text = pack.descriptionEn.ifBlank { pack.descriptionFa },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (pack.isCorePack && target > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isComplete) "Ready for offline study" else "Download Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isComplete) SuccessGreen else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$installed / $target",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape),
                        color = if (isComplete) SuccessGreen else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    if (statusMessage != null) {
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (syncState?.stage == "error") {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if ((syncState?.warningCount ?: 0) > 0) {
                        Text(
                            text = "Some resources were unavailable; download can continue.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            } else if (!pack.isCorePack) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(9.dp)
                ) {
                    Text(
                        text = "${pack.wordCount} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pack.isCorePack && !isComplete) {
                    Button(
                        onClick = onSync,
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            when {
                                isSyncing -> "Downloading…"
                                installed > 0 -> "Resume Download"
                                else -> "Download Pack"
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = onViewWords,
                    enabled = !pack.isCorePack || installed > 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(if (pack.isCorePack) "Study Words" else "View Words")
                }
            }
        }
    }
}
