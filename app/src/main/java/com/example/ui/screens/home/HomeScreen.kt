package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.model.StreakInfo
import com.example.ui.components.AppCard
import com.example.ui.components.AppInset
import com.example.ui.components.CefrBadge
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.IconTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.TagChip
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToVocab: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToTutor: () -> Unit,
    onNavigateToLearn: () -> Unit,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToWriting: () -> Unit,
    onNavigateToDiagnostic: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    onNavigateToAiVocabCard: () -> Unit = {},
    onNavigateToExamTracks: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val streakInfo by viewModel.streakInfo.collectAsState()

    EnglishLtrLayout {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = Dimens.screenGutter,
                top = Dimens.space12,
                end = Dimens.screenGutter,
                bottom = Dimens.space12
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
        ) {
            item {
                HomeHeader(
                    name = state.userProfile.userName.ifBlank { "Learner" },
                    goal = state.userProfile.targetGoal,
                    level = state.userProfile.currentLevel,
                    streakInfo = streakInfo
                )
            }

            item {
                ExamSprintDashboardCard(
                    targetExam = state.userProfile.targetGoal.ifBlank { "IELTS" },
                    targetScore = state.userProfile.targetBandOrScore.ifBlank { "7.5" },
                    daysRemaining = state.daysRemaining,
                    dueCount = state.dueWordsCount,
                    weakCount = state.weakWordsCount,
                    newCount = state.dailyNewWordsLimit,
                    onStartSprint = onNavigateToReview,
                    onOpenTutor = onNavigateToTutor
                )
            }

            item {
                LearningAndToolsSection(
                    onNavigateToExamTracks = onNavigateToExamTracks,
                    onNavigateToSpeaking = onNavigateToSpeaking,
                    onNavigateToWriting = onNavigateToWriting,
                    onNavigateToMistakes = onNavigateToMistakes,
                    onNavigateToTutor = onNavigateToTutor,
                    onNavigateToAiVocabCard = onNavigateToAiVocabCard,
                    onNavigateToDiagnostic = onNavigateToDiagnostic
                )
            }

            item {
                CompactDailyPlanBar(
                    completedMinutes = state.dailyMinutesCompleted,
                    plannedMinutes = state.dailyMinutesPlanned,
                    onOpenVocab = onNavigateToVocab,
                    onOpenLearn = onNavigateToLearn
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    name: String,
    goal: String,
    level: String,
    streakInfo: StreakInfo
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
        ) {
            Text(
                text = "Welcome, $name",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Target goal: $goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            TagChip(
                text = "${streakInfo.currentStreak} day streak",
                containerColor = Accent.warningSoft,
                contentColor = Accent.warningOnSoft
            )
            CefrBadge(level = level)
        }
    }
}

@Composable
private fun ExamSprintDashboardCard(
    targetExam: String,
    targetScore: String,
    daysRemaining: Int?,
    dueCount: Int,
    weakCount: Int,
    newCount: Int,
    onStartSprint: () -> Unit,
    onOpenTutor: () -> Unit
) {
    val totalSprintQueue = dueCount + weakCount + newCount

    AppCard(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.space2)
            ) {
                Text(
                    text = "$targetExam Sprint Plan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (daysRemaining != null) {
                        "$daysRemaining days remaining until test day"
                    } else {
                        "Intensive vocabulary preparation"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Dimens.space8))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                TagChip(
                    text = "Target: $targetScore",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TagChip(
                    text = if (dueCount > 0) "$dueCount due" else "On track",
                    containerColor = if (dueCount > 0) {
                        Accent.dangerSoft
                    } else {
                        Accent.successSoft
                    },
                    contentColor = if (dueCount > 0) {
                        Accent.dangerOnSoft
                    } else {
                        Accent.successOnSoft
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.blockGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
        ) {
            SprintStagePill(
                title = "Due",
                count = dueCount,
                accentColor = Accent.danger,
                modifier = Modifier.weight(1f)
            )
            SprintStagePill(
                title = "Weak",
                count = weakCount,
                accentColor = Accent.warning,
                modifier = Modifier.weight(1f)
            )
            SprintStagePill(
                title = "New",
                count = newCount,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Dimens.blockGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
        ) {
            Button(
                onClick = onStartSprint,
                modifier = Modifier
                    .weight(1.3f)
                    .height(Dimens.minTapTarget),
                shape = RoundedCornerShape(Dimens.radiusSm)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSm)
                )
                Spacer(modifier = Modifier.width(Dimens.space6))
                Text(
                    text = "Start Sprint ($totalSprintQueue)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onOpenTutor,
                modifier = Modifier
                    .weight(0.9f)
                    .height(Dimens.minTapTarget),
                shape = RoundedCornerShape(Dimens.radiusSm),
                border = BorderStroke(
                    Dimens.hairline,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSm)
                )
                Spacer(modifier = Modifier.width(Dimens.space4))
                Text(
                    text = "AI Tutor",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SprintStagePill(
    title: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AppInset(
        modifier = modifier,
        padding = Dimens.space8,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = accentColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LearningAndToolsSection(
    onNavigateToExamTracks: () -> Unit,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToWriting: () -> Unit,
    onNavigateToMistakes: () -> Unit,
    onNavigateToTutor: () -> Unit,
    onNavigateToAiVocabCard: () -> Unit,
    onNavigateToDiagnostic: () -> Unit
) {
    AppCard {
        SectionHeader(
            title = "Learning & AI Tools",
            subtitle = "Core practice and smart assistance in one place"
        )
        Spacer(modifier = Modifier.height(Dimens.blockGap))
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                CompactActionTile(
                    icon = Icons.Default.School,
                    title = "Exam Tracks",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToExamTracks,
                    modifier = Modifier.weight(1f)
                )
                CompactActionTile(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Speaking",
                    tint = Accent.success,
                    onClick = onNavigateToSpeaking,
                    modifier = Modifier.weight(1f)
                )
                CompactActionTile(
                    icon = Icons.Default.EditNote,
                    title = "Writing",
                    tint = Accent.accent,
                    onClick = onNavigateToWriting,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space6)
            ) {
                CompactActionTile(
                    icon = Icons.Default.Warning,
                    title = "Mistakes",
                    tint = Accent.danger,
                    onClick = onNavigateToMistakes,
                    modifier = Modifier.weight(1f)
                )
                CompactActionTile(
                    icon = Icons.Default.Psychology,
                    title = "AI Tutor",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToTutor,
                    modifier = Modifier.weight(1f)
                )
                CompactActionTile(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Cards",
                    tint = Accent.accent,
                    onClick = onNavigateToAiVocabCard,
                    modifier = Modifier.weight(1f)
                )
                CompactActionTile(
                    icon = Icons.Default.FitnessCenter,
                    title = "Diagnostic",
                    tint = Accent.info,
                    onClick = onNavigateToDiagnostic,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactActionTile(
    icon: ImageVector,
    title: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        onClick = onClick,
        modifier = modifier.defaultMinSize(
            minHeight = Dimens.minTapTarget + Dimens.space12
        ),
        padding = Dimens.space6,
        shape = RoundedCornerShape(Dimens.radiusSm),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.space6)
        ) {
            IconTile(
                icon = icon,
                tint = tint,
                size = Dimens.iconTileSm,
                iconSize = Dimens.iconSm
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactDailyPlanBar(
    completedMinutes: Int,
    plannedMinutes: Int,
    onOpenVocab: () -> Unit,
    onOpenLearn: () -> Unit
) {
    val target = plannedMinutes.coerceAtLeast(1)
    val progress = (completedMinutes.toFloat() / target).coerceIn(0f, 1f)

    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
            SectionHeader(
                title = "Daily Plan",
                subtitle = "$completedMinutes of $plannedMinutes minutes complete"
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.progressHeight)
                    .clip(RoundedCornerShape(Dimens.radiusPill)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
            ) {
                OutlinedButton(
                    onClick = onOpenVocab,
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.minTapTarget),
                    shape = RoundedCornerShape(Dimens.radiusSm)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space6))
                    Text(
                        text = "Vocabulary",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onOpenLearn,
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.minTapTarget),
                    shape = RoundedCornerShape(Dimens.radiusSm)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space6))
                    Text(
                        text = "Skills & Grammar",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
