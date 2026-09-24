package com.example.ui.screens.practice

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HistoryEdu
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
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
    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "مرکز تمرین و آزمون هوشمند"
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "تمرینات تعاملی، آزمون‌های شبیه‌ساز و پارتنرهای گفتاری هوش مصنوعی:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                item {
                    PracticeHubCard(
                        title = "مرور هوشمند و فعال (Active Recall SRS)",
                        desc = "جلسه مرور فلش‌کارت‌ها، تست‌های چندگزینه‌ای و تایپ کلمات در فواصل علمی",
                        icon = Icons.Default.Timer,
                        color = PrimaryBlue,
                        badge = "روزانه",
                        onClick = onNavigateToReview
                    )
                }

                item {
                    PracticeHubCard(
                        title = "آزمون و تمرین اسپیکینگ آیلتس (IELTS Speaking)",
                        desc = "شبیه‌ساز رسمی پارت ۱، ۲ (کیو کارت با تایمر) و ۳ با ممتحن صوتی هوش مصنوعی و بازخورد بلادرنگ فارسی",
                        icon = Icons.Default.RecordVoiceOver,
                        color = Color(0xFF7C3AED),
                        badge = "IELTS AI",
                        onClick = onNavigateToSpeaking
                    )
                }

                item {
                    PracticeHubCard(
                        title = "تصحیح رایتینگ آیلتس (Writing AI Grader)",
                        desc = "ارزیابی مقالات طبق معیارهای چهارگانه تسک ۱ و ۲ با اصلاح خط به خط",
                        icon = Icons.Default.EditNote,
                        color = Color(0xFFF59E0B),
                        badge = "Task 1 & 2",
                        onClick = onNavigateToWriting
                    )
                }

                item {
                    PracticeHubCard(
                        title = "معلم هوشمند من (AI Language Tutor)",
                        desc = "پاسخگویی به سوالات مفهومی، تفاوت کلمات، نکات گرامری و سوال پرسیدن از شما",
                        icon = Icons.Default.Psychology,
                        color = Color(0xFF8B5CF6),
                        badge = "فارسی",
                        onClick = onNavigateToTutor
                    )
                }

                item {
                    PracticeHubCard(
                        title = "آزمون تعیین سطح هوشمند (Diagnostic Test)",
                        desc = "سنجش دقیق تسلط دستوری و واژگانی بر مبنای سطوح استاندارد CEFR",
                        icon = Icons.Default.FitnessCenter,
                        color = SuccessGreen,
                        badge = "CEFR A1-C2",
                        onClick = onNavigateToDiagnostic
                    )
                }

                item {
                    PracticeHubCard(
                        title = "دفترچه اشتباهات من (Mistake Notebook)",
                        desc = "مرور هدفمند خطاهای ثبت‌شده در آزمون‌ها و جلسات مرور به همراه تحلیل علت",
                        icon = Icons.Default.Warning,
                        color = ErrorRed,
                        badge = "تحلیلی",
                        onClick = onNavigateToMistakes
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun PracticeHubCard(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            color = color,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
