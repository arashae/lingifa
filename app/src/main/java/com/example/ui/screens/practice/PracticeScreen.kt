package com.example.ui.screens.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.IconTile
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

private data class PracticeDestination(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun PracticeScreen(
    onNavigateToReview: () -> Unit,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToWriting: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    onNavigateToTutor: () -> Unit
) {
    val destinations = listOf(
        PracticeDestination(
            title = "Smart SRS Review",
            description = "Spaced repetition flashcards, memory recall, and active review",
            icon = Icons.Default.Timer,
            accent = MaterialTheme.colorScheme.primary,
            label = "SRS",
            onClick = onNavigateToReview
        ),
        PracticeDestination(
            title = "Speaking Simulator",
            description = "IELTS interview simulation with pronunciation feedback",
            icon = Icons.Default.RecordVoiceOver,
            accent = MaterialTheme.colorScheme.secondary,
            label = "IELTS",
            onClick = onNavigateToSpeaking
        ),
        PracticeDestination(
            title = "Writing Grader",
            description = "Task 1 and Task 2 evaluation with Cambridge IELTS scoring",
            icon = Icons.Default.EditNote,
            accent = MaterialTheme.colorScheme.tertiary,
            label = "TASK 1 · 2",
            onClick = onNavigateToWriting
        ),
        PracticeDestination(
            title = "AI Language Tutor",
            description = "Fast grammar, vocabulary, usage, and nuance answers",
            icon = Icons.Default.Psychology,
            accent = Accent.accent,
            label = "AI",
            onClick = onNavigateToTutor
        ),
        PracticeDestination(
            title = "Level Placement",
            description = "Assess your CEFR level and personalize your study track",
            icon = Icons.Default.FitnessCenter,
            accent = Accent.success,
            label = "CEFR",
            onClick = onNavigateToDiagnostic
        ),
        PracticeDestination(
            title = "Mistake Notebook",
            description = "Review past errors and turn recurring weaknesses into strengths",
            icon = Icons.Default.Warning,
            accent = Accent.danger,
            label = "REVIEW",
            onClick = onNavigateToMistakes
        )
    )

    EnglishLtrLayout {
        Scaffold(
            topBar = { LinguaTopAppBar(title = "Practice Hub") },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(Dimens.screenGutter),
                horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap),
                verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = "Targeted Practice",
                        subtitle = "Specialized tools for each language skill."
                    )
                }
                items(
                    items = destinations,
                    key = { it.title }
                ) { destination ->
                    PracticeTile(destination = destination)
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AppInset(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        padding = Dimens.cardPaddingTight
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconMd)
                            )
                            Text(
                                text = "Daily habit: complete at least one focused session each day.",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeTile(destination: PracticeDestination) {
    AppCard(
        onClick = destination.onClick,
        padding = Dimens.cardPaddingTight
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = destination.icon,
                    tint = destination.accent
                )
                Spacer(modifier = Modifier.weight(1f))
                TagChip(
                    text = destination.label,
                    containerColor = destination.accent.copy(alpha = 0.12f),
                    contentColor = destination.accent
                )
            }
            Text(
                text = destination.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = destination.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = destination.accent,
                    modifier = Modifier.size(Dimens.iconSm)
                )
            }
        }
    }
}
