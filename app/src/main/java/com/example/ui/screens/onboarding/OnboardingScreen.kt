package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PersianRtlLayout
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.theme.PrimaryBlue

@Composable
fun OnboardingScreen(
    profileViewModel: ProfileViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Goal, 2: Level, 3: Daily Time, 4: Weakest Skill, 5: Target Band

    var selectedGoal by remember { mutableStateOf("آیلتس") }
    var selectedLevel by remember { mutableStateOf("B1") }
    var selectedTimeMinutes by remember { mutableStateOf(30) }
    var selectedWeakestSkill by remember { mutableStateOf("Speaking") }
    var targetScoreInput by remember { mutableStateOf("7.5") }

    PersianRtlLayout {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Header progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "گام $step از ۵",
                            style = MaterialTheme.typography.titleSmall.copy(color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "شخصی‌سازی یادگیری",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (step / 5f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    when (step) {
                        1 -> {
                            Text(
                                text = "هدفت از یادگیری زبان چیه؟",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "برنامه آموزشی و لغات پیشنهادی بر اساس هدف شما بهینه‌سازی خواهند شد.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val goals = listOf("آیلتس", "تافل", "مهاجرت", "دانشگاه", "کار", "مکالمه", "زبان عمومی", "افزایش دایره لغات")
                            goals.forEach { g ->
                                SelectableOptionCard(
                                    title = g,
                                    isSelected = selectedGoal == g,
                                    onClick = { selectedGoal = g }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        2 -> {
                            Text(
                                text = "سطح تقریبی فعلی‌ات چقدره؟",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "در صورتی که مطمئن نیستید، می‌توانید گزینه 'نمی‌دانم' را انتخاب کرده و آزمون تعیین سطح بدهید.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val levels = listOf(
                                "مبتدی (Beginner)" to "A1",
                                "پایه (Elementary)" to "A2",
                                "متوسط (Intermediate)" to "B1",
                                "متوسط رو به بالا (Upper-Intermediate)" to "B2",
                                "پیشرفته (Advanced)" to "C1",
                                "حرفه‌ای (Mastery)" to "C2",
                                "دقیقاً نمی‌دانم (آزمون تعیین سطح)" to "B1"
                            )
                            levels.forEach { (label, code) ->
                                SelectableOptionCard(
                                    title = label,
                                    isSelected = selectedLevel == code,
                                    onClick = { selectedLevel = code }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        3 -> {
                            Text(
                                text = "روزانه چقدر زمان برای مطالعه داری؟",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "برنامه هوشمند مرور و درس‌ها طبق این زمان به بازه‌های کوتاه تقسیم می‌شوند.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val times = listOf(
                                "۱۵ دقیقه در روز" to 15,
                                "۳۰ دقیقه در روز" to 30,
                                "۴۵ دقیقه در روز" to 45,
                                "۶۰ دقیقه در روز" to 60,
                                "بیشتر از یک ساعت" to 75
                            )
                            times.forEach { (label, mins) ->
                                SelectableOptionCard(
                                    title = label,
                                    isSelected = selectedTimeMinutes == mins,
                                    onClick = { selectedTimeMinutes = mins }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        4 -> {
                            Text(
                                text = "کدام مهارتت ضعیف‌تر است و نیاز به تمرکز دارد؟",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val skills = listOf("لغت (Vocabulary)", "گرامر (Grammar)", "Reading", "Listening", "Speaking", "Writing", "همه مهارت‌ها یکسان")
                            skills.forEach { sk ->
                                SelectableOptionCard(
                                    title = sk,
                                    isSelected = selectedWeakestSkill == sk.takeWhile { it != ' ' },
                                    onClick = { selectedWeakestSkill = sk.takeWhile { it != ' ' } }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        5 -> {
                            Text(
                                text = "نمره هدف شما در آزمون چقدر است؟",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "مثال: نمره ۷.۵ در آیلتس، نمره ۱۰۰ در تافل یا سطح C1",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = targetScoreInput,
                                onValueChange = { targetScoreInput = it },
                                label = { Text("نمره هدف (Target Score / Band)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Action Buttons
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { step -= 1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("مرحله قبل")
                            }
                        }

                        Button(
                            onClick = {
                                if (step < 5) {
                                    step += 1
                                } else {
                                    profileViewModel.updateProfile(
                                        name = "کاربر عزیز",
                                        goal = selectedGoal,
                                        level = selectedLevel,
                                        dailyMinutes = selectedTimeMinutes,
                                        targetScore = targetScoreInput
                                    )
                                    onComplete()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(if (step > 1) 2f else 1f)
                        ) {
                            Text(
                                text = if (step == 5) "ورود به LinguaFa AI" else "مرحله بعد",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableOptionCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryBlue) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
