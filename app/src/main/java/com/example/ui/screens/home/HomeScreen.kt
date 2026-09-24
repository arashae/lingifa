package com.example.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StreakInfo
import com.example.ui.components.CefrBadge
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

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

    PersianRtlLayout {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Streamlined Header with Name, Level, and Streak Pill
            item {
                HomeHeader(
                    name = state.userProfile.userName,
                    goal = state.userProfile.targetGoal,
                    level = state.userProfile.currentLevel,
                    streakInfo = streakInfo
                )
            }

            // 2. Focused Daily Hero Card (Today's SRS Review CTA)
            item {
                ModernFocusHeroCard(
                    dueCount = state.dueWordsCount,
                    totalWords = state.totalWordsCount,
                    onStartReview = onNavigateToReview,
                    onOpenTutor = onNavigateToTutor
                )
            }

            // 3. Compact 3-Column Stats Row
            item {
                MinimalStatsBar(
                    words = state.totalWordsCount,
                    learned = state.learnedWordsCount,
                    xp = state.userProfile.xp
                )
            }

            // 4. Primary Learning Hub (2x2 Grid)
            item {
                Text(
                    text = "بخش‌های اصلی یادگیری",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                PrimaryActionGrid(
                    onNavigateToExamTracks = onNavigateToExamTracks,
                    onNavigateToSpeaking = onNavigateToSpeaking,
                    onNavigateToWriting = onNavigateToWriting,
                    onNavigateToMistakes = onNavigateToMistakes
                )
            }

            // 5. Daily Plan Progress Bar
            item {
                CompactDailyPlanBar(
                    completedMinutes = state.dailyMinutesCompleted,
                    plannedMinutes = state.dailyMinutesPlanned,
                    onOpenVocab = onNavigateToVocab,
                    onOpenLearn = onNavigateToLearn
                )
            }

            // 6. Auxiliary Quick Tools Strip
            item {
                Text(
                    text = "ابزارهای کمکی",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                QuickToolsHorizontalStrip(
                    onNavigateToTutor = onNavigateToTutor,
                    onNavigateToAiVocabCard = onNavigateToAiVocabCard,
                    onNavigateToDiagnostic = onNavigateToDiagnostic
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
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "سلام، $name",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "مسیر هدف: $goal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Streak Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AccentGold.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "${streakInfo.currentStreak} روز",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309)
                    )
                }
            }

            CefrBadge(level = level)
        }
    }
}

@Composable
private fun ModernFocusHeroCard(
    dueCount: Int,
    totalWords: Int,
    onStartReview: () -> Unit,
    onOpenTutor: () -> Unit
) {
    val hasDue = dueCount > 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasDue) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        border = BorderStroke(
            1.dp,
            if (hasDue) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (hasDue) "مرور هوشمند امروز (SRS)" else "برنامه امروز تکمیل است! 🎉",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (hasDue) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasDue) "$dueCount واژه برای تثبیت حافظه آماده مرور است."
                        else "تمام واژه‌های موعد امروز را مرور کرده‌اید. آماده کلمات جدید هستید؟",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasDue) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasDue) MaterialTheme.colorScheme.primary else SuccessGreen,
                    contentColor = Color.White
                ) {
                    Text(
                        text = if (hasDue) "$dueCount واژه" else "انجام شد",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartReview,
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasDue) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface,
                        contentColor = if (hasDue) Color.White else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasDue) "شروع مرور هوشمند" else "تمرین آزاد واژگان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenTutor,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معلم AI", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MinimalStatsBar(
    words: Int,
    learned: Int,
    xp: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactStatPill(
            icon = Icons.Default.AutoStories,
            value = words.toString(),
            label = "بانک واژگان",
            color = PrimaryBlue,
            modifier = Modifier.weight(1f)
        )
        CompactStatPill(
            icon = Icons.Default.School,
            value = learned.toString(),
            label = "مسلط شده",
            color = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        CompactStatPill(
            icon = Icons.Default.FitnessCenter,
            value = "$xp",
            label = "امتیاز XP",
            color = AccentGold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactStatPill(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PrimaryActionGrid(
    onNavigateToExamTracks: () -> Unit,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToWriting: () -> Unit,
    onNavigateToMistakes: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernActionTile(
                icon = Icons.Default.School,
                title = "مسیر آزمون‌ها",
                subtitle = "IELTS · TOEFL · GRE",
                accentColor = PrimaryBlue,
                onClick = onNavigateToExamTracks,
                modifier = Modifier.weight(1f)
            )
            ModernActionTile(
                icon = Icons.Default.RecordVoiceOver,
                title = "اسپیکینگ & مکالمه",
                subtitle = "شبیه‌ساز هوشمند صوتی",
                accentColor = SuccessGreen,
                onClick = onNavigateToSpeaking,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernActionTile(
                icon = Icons.Default.EditNote,
                title = "رایتینگ & نگارش",
                subtitle = "تصحیح و نمره‌دهی فوری",
                accentColor = Color(0xFF8B5CF6),
                onClick = onNavigateToWriting,
                modifier = Modifier.weight(1f)
            )
            ModernActionTile(
                icon = Icons.Default.Warning,
                title = "دفترچه اشتباهات",
                subtitle = "مرور و حل خطاهای قبلی",
                accentColor = ErrorRed,
                onClick = onNavigateToMistakes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModernActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(19.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    text = "برنامه روزانه: $completedMinutes از $plannedMinutes دقیقه",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(progress * 100).toInt()}٪",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onOpenVocab,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Text("کتابخانه واژگان", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Surface(
                    onClick = onOpenLearn,
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Text("درس‌های مهارت", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolsHorizontalStrip(
    onNavigateToTutor: () -> Unit,
    onNavigateToAiVocabCard: () -> Unit,
    onNavigateToDiagnostic: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickToolChip(
            icon = Icons.Default.Psychology,
            title = "معلم AI",
            onClick = onNavigateToTutor
        )
        QuickToolChip(
            icon = Icons.Default.AutoAwesome,
            title = "کارت واژه هوشمند",
            onClick = onNavigateToAiVocabCard
        )
        QuickToolChip(
            icon = Icons.Default.FitnessCenter,
            title = "آزمون تعیین سطح",
            onClick = onNavigateToDiagnostic
        )
    }
}

@Composable
private fun QuickToolChip(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
