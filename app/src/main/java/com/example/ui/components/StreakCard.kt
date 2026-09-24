package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreakInfo
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun MinimalStreakCard(
    streakInfo: StreakInfo,
    onLogPracticeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMilestoneDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showMilestoneDialog = true },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Flame & Streak count + Level badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF6D00), Color(0xFFFFAB00))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${streakInfo.currentStreak} روز متوالی",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (streakInfo.isTodayCompleted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SuccessGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "امروز ثبت شد ✓",
                                        color = SuccessGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = streakInfo.streakLevelFa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                // XP pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentGold.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${streakInfo.totalXp} XP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = AccentGold
                        )
                    }
                }
            }

            // 7-day Weekly Micro Tracker Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                streakInfo.weeklyDays.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = day.dayNameFa,
                            fontSize = 11.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        day.isCompleted -> SuccessGreen
                                        day.isToday -> PrimaryBlue.copy(alpha = 0.12f)
                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    }
                                )
                                .then(
                                    if (day.isToday && !day.isCompleted) {
                                        Modifier.border(1.5.dp, PrimaryBlue, CircleShape)
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
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = day.dayNumber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (day.isToday) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Motivational quote / advice
            Text(
                text = streakInfo.motivationalMessageFa,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Milestones and Rewards Dialog
    if (showMilestoneDialog) {
        AlertDialog(
            onDismissRequest = { showMilestoneDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = AccentGold)
                    Text("مدال‌ها و دستاوردهای پیوستگی", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "با هر روز تمرین مستمر، نشان‌های جدید باز کنید و سرعت یادگیری زبان را چندبرابر کنید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    streakInfo.unlockedMilestones.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SuccessGreen.copy(alpha = 0.08f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = null, tint = SuccessGreen)
                            Column {
                                Text(text = m.titleFa, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = m.descriptionFa, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    streakInfo.nextMilestone?.let { nm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            Column {
                                Text(text = "قفل بعدی: ${nm.titleFa} (${nm.requiredDays} روز)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = nm.descriptionFa, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMilestoneDialog = false }) {
                    Text("متوجه شدم", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
