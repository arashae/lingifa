package com.example.ui.screens.home

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
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.components.CefrBadge
import com.example.ui.components.MinimalStreakCard
import com.example.ui.components.PersianRtlLayout
import com.example.ui.components.PersianSectionHeader
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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HomeHeader(
                    name = state.userProfile.userName,
                    goal = state.userProfile.targetGoal,
                    level = state.userProfile.currentLevel
                )
            }

            item {
                FocusCard(
                    dueCount = state.dueWordsCount,
                    onReviewClick = onNavigateToReview,
                    onTutorClick = onNavigateToTutor
                )
            }

            item {
                StatsRow(
                    words = state.totalWordsCount,
                    learned = state.learnedWordsCount,
                    xp = state.userProfile.xp
                )
            }

            item {
                MinimalStreakCard(
                    streakInfo = streakInfo,
                    onLogPracticeClick = onNavigateToReview
                )
            }

            item {
                TodayPlanCard(
                    state = state,
                    onNavigateToLearn = onNavigateToLearn,
                    onNavigateToVocab = onNavigateToVocab
                )
            }

            item {
                PersianSectionHeader(
                    title = "ابزارهای یادگیری",
                    subtitle = "هر ابزار برای یک مرحله از مسیر یادگیری"
                )
            }

            item {
                ActionGrid(
                    onNavigateToTutor = onNavigateToTutor,
                    onNavigateToSpeaking = onNavigateToSpeaking,
                    onNavigateToWriting = onNavigateToWriting,
                    onNavigateToMistakes = onNavigateToMistakes
                )
            }

            item {
                CompactTools(
                    onNavigateToAiVocabCard = onNavigateToAiVocabCard,
                    onNavigateToExamTracks = onNavigateToExamTracks,
                    onNavigateToDiagnostic = onNavigateToDiagnostic
                )
            }

            item {
                WeakSkillCard(
                    weakestSkill = state.userProfile.weakestSkill,
                    onClick = onNavigateToDiagnostic
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    name: String,
    goal: String,
    level: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "سلام، $name",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "مسیر $goal را امروز هم جلو ببر",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CefrBadge(level = level)
    }
}

@Composable
private fun FocusCard(
    dueCount: Int,
    onReviewClick: () -> Unit,
    onTutorClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = if (dueCount > 0) "نوبت مرور امروز" else "آماده‌ای برای یک مرور کوتاه؟",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (dueCount > 0) "$dueCount واژه برای تثبیت در حافظه آماده است." else "یک جلسه سبک، حتی وقتی مرور امروز خالی است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onReviewClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        contentColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("شروع مرور", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onTutorClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("پرسش از AI", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    words: Int,
    learned: Int,
    xp: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatTile(
            value = words.toString(),
            label = "کل لغات",
            color = PrimaryBlue,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = learned.toString(),
            label = "یادگرفته",
            color = SuccessGreen,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = "$xp XP",
            label = "امتیاز",
            color = AccentGold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TodayPlanCard(
    state: HomeUiState,
    onNavigateToLearn: () -> Unit,
    onNavigateToVocab: () -> Unit
) {
    val progress = (state.dailyMinutesCompleted.toFloat() / state.dailyMinutesPlanned.coerceAtLeast(1)).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("برنامه امروز", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${state.dailyMinutesCompleted} از ${state.dailyMinutesPlanned} دقیقه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}٪",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlanAction(
                    icon = Icons.Default.AutoStories,
                    title = "مرور واژگان",
                    onClick = onNavigateToVocab,
                    modifier = Modifier.weight(1f)
                )
                PlanAction(
                    icon = Icons.Default.School,
                    title = "درس کوتاه",
                    onClick = onNavigateToLearn,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlanAction(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ActionGrid(
    onNavigateToTutor: () -> Unit,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToWriting: () -> Unit,
    onNavigateToMistakes: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                icon = Icons.Default.Psychology,
                title = "معلم هوشمند",
                subtitle = "پاسخ سریع و دقیق",
                onClick = onNavigateToTutor,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                icon = Icons.Default.RecordVoiceOver,
                title = "Speaking",
                subtitle = "تمرین مکالمه",
                onClick = onNavigateToSpeaking,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionTile(
                icon = Icons.Default.EditNote,
                title = "Writing",
                subtitle = "تصحیح مقاله",
                onClick = onNavigateToWriting,
                modifier = Modifier.weight(1f)
            )
            ActionTile(
                icon = Icons.Default.Warning,
                title = "اشتباهات",
                subtitle = "نقاط ضعف",
                onClick = onNavigateToMistakes,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactTools(
    onNavigateToAiVocabCard: () -> Unit,
    onNavigateToExamTracks: () -> Unit,
    onNavigateToDiagnostic: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolRow(
            icon = Icons.Default.AutoAwesome,
            title = "کارت واژه هوشمند",
            subtitle = "تلفظ، ترجمه و ثبت سریع",
            onClick = onNavigateToAiVocabCard
        )
        ToolRow(
            icon = Icons.Default.LibraryBooks,
            title = "مسیرهای آزمون",
            subtitle = "IELTS · TOEFL · GRE",
            onClick = onNavigateToExamTracks
        )
        ToolRow(
            icon = Icons.Default.FitnessCenter,
            title = "تعیین سطح هوشمند",
            subtitle = "برنامه مناسب سطح تو",
            onClick = onNavigateToDiagnostic
        )
    }
}

@Composable
private fun ToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("باز کردن", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun WeakSkillCard(
    weakestSkill: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(ErrorRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("نقطه قابل بهبود: $weakestSkill", style = MaterialTheme.typography.titleSmall)
                Text("با آزمون کوتاه، تمرین بعدی را دقیق‌تر انتخاب کن.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("شروع", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
