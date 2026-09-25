package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.StreakInfo
import com.example.ui.theme.Accent
import com.example.ui.theme.Dimens

@Composable
fun MinimalStreakCard(
    streakInfo: StreakInfo,
    onLogPracticeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMilestoneDialog by remember { mutableStateOf(false) }

    AppCard(
        modifier = modifier,
        onClick = { showMilestoneDialog = true },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        borderColor = Accent.warning.copy(alpha = 0.28f)
    ) {
        // Header: flame + count, level name, XP pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconTile(
                    icon = Icons.Default.LocalFireDepartment,
                    tint = Accent.streak,
                    size = Dimens.iconTileMd,
                    iconSize = Dimens.iconMd,
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space2)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${streakInfo.currentStreak} day streak",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (streakInfo.isTodayCompleted) {
                            Spacer(modifier = Modifier.width(Dimens.space6))
                            TagChip(
                                text = "Done today",
                                containerColor = Accent.successSoft,
                                contentColor = Accent.successOnSoft
                            )
                        }
                    }
                    Text(
                        text = streakInfo.streakLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.space8))
            Surface(
                shape = RoundedCornerShape(Dimens.radiusPill),
                color = Accent.warning.copy(alpha = 0.12f),
                border = BorderStroke(Dimens.hairline, Accent.warning.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = Dimens.space10,
                        vertical = Dimens.space6
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space4)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = Accent.warning,
                        modifier = Modifier.size(Dimens.iconSm)
                    )
                    Text(
                        text = "${streakInfo.totalXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent.warning
                    )
                }
            }
        }

        // 7-day tracker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.radiusSm))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = Dimens.space8, horizontal = Dimens.space6),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            streakInfo.weeklyDays.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.space4)
                ) {
                    Text(
                        text = day.dayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day.isCompleted -> Accent.success
                                    day.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                }
                            )
                            .then(
                                if (day.isToday && !day.isCompleted) {
                                    Modifier.border(
                                        Dimens.hairline,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            Text(
                                text = day.dayNumber,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (day.isToday) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = streakInfo.motivationalMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showMilestoneDialog) {
        AlertDialog(
            onDismissRequest = { showMilestoneDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Accent.warning
                    )
                    Text(
                        "Streak Milestones",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    Text(
                        text = "Practise every day to unlock new badges and multiply your learning speed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    streakInfo.unlockedMilestones.forEach { m ->
                        MilestoneRow(
                            title = m.title,
                            description = m.description,
                            unlocked = true
                        )
                    }

                    streakInfo.nextMilestone?.let { nm ->
                        MilestoneRow(
                            title = "Next: ${nm.title} (${nm.requiredDays} days)",
                            description = nm.description,
                            unlocked = false
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showMilestoneDialog = false },
                    modifier = Modifier.heightIn(min = Dimens.minTapTarget)
                ) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun MilestoneRow(
    title: String,
    description: String,
    unlocked: Boolean
) {
    val container = if (unlocked) {
        Accent.success.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val iconTint = if (unlocked) Accent.success else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.radiusSm))
            .background(container)
            .padding(Dimens.space10),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.space10)
    ) {
        Icon(
            imageVector = Icons.Default.MilitaryTech,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(Dimens.iconMd)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.space2)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
