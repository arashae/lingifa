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
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.theme.PrimaryBlue

@Composable
fun OnboardingScreen(
    profileViewModel: ProfileViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Goal, 2: Level, 3: Daily Time, 4: Weakest Skill, 5: Target Band

    var selectedGoal by remember { mutableStateOf("IELTS Exam") }
    var selectedLevel by remember { mutableStateOf("B1") }
    var selectedTimeMinutes by remember { mutableStateOf(30) }
    var selectedWeakestSkill by remember { mutableStateOf("Speaking") }
    var targetScoreInput by remember { mutableStateOf("7.5") }

    EnglishLtrLayout {
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
                            text = "Step $step of 5",
                            style = MaterialTheme.typography.titleSmall.copy(color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Personalize Your Plan",
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
                                text = "What is your main language learning goal?",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your study path and vocabulary recommendations will be personalized to your target.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val goals = listOf("IELTS Exam", "TOEFL Exam", "GRE Exam", "Immigration", "University Study", "Career & Work", "Daily Conversation", "Vocabulary Mastery")
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
                                text = "What is your approximate English level?",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "If you are unsure, you can choose 'Not Sure' and take the Smart Placement Test.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val levels = listOf(
                                "A1 - Beginner" to "A1",
                                "A2 - Elementary" to "A2",
                                "B1 - Intermediate" to "B1",
                                "B2 - Upper-Intermediate" to "B2",
                                "C1 - Advanced" to "C1",
                                "C2 - Mastery" to "C2",
                                "Not Sure (Placement Test)" to "B1"
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
                                text = "How much time can you study daily?",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your smart SRS reviews and lessons will be scheduled into manageable daily bite-sized sessions.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val times = listOf(
                                "15 minutes per day" to 15,
                                "30 minutes per day" to 30,
                                "45 minutes per day" to 45,
                                "60 minutes per day" to 60,
                                "More than 1 hour" to 75
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
                                text = "Which skill needs the most improvement?",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val skills = listOf("Vocabulary", "Grammar", "Reading", "Listening", "Speaking", "Writing", "All Balanced")
                            skills.forEach { sk ->
                                SelectableOptionCard(
                                    title = sk,
                                    isSelected = selectedWeakestSkill == sk,
                                    onClick = { selectedWeakestSkill = sk }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        5 -> {
                            Text(
                                text = "What is your target exam score / band?",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Example: Band 7.5 in IELTS, 100 in TOEFL, or CEFR C1",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = targetScoreInput,
                                onValueChange = { targetScoreInput = it },
                                label = { Text("Target Score / Band") },
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
                                Text("Previous")
                            }
                        }

                        Button(
                            onClick = {
                                if (step < 5) {
                                    step += 1
                                } else {
                                    profileViewModel.updateProfile(
                                        name = "Learner",
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
                                text = if (step == 5) "Get Started with LinguaFa" else "Next Step",
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
