package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.components.FieldLabel
import com.example.ui.components.SelectChip
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.theme.Dimens

private val goalOptions = listOf(
    "IELTS Exam" to "IELTS Exam",
    "TOEFL Exam" to "TOEFL Exam",
    "GRE Exam" to "GRE Exam",
    "Immigration" to "Immigration",
    "University Study" to "University Study",
    "Career & Work" to "Career & Work",
    "Daily Conversation" to "Daily Conversation",
    "Vocabulary Mastery" to "Vocabulary Mastery"
)

private val levelOptions = listOf(
    "A1 - Beginner" to "A1",
    "A2 - Elementary" to "A2",
    "B1 - Intermediate" to "B1",
    "B2 - Upper-Intermediate" to "B2",
    "C1 - Advanced" to "C1",
    "C2 - Mastery" to "C2",
    "Not Sure (Placement Test)" to "B1"
)

private val timeOptions = listOf(
    "15 minutes per day" to "15",
    "30 minutes per day" to "30",
    "45 minutes per day" to "45",
    "60 minutes per day" to "60",
    "More than 1 hour" to "75"
)

private val skillOptions = listOf(
    "Vocabulary" to "Vocabulary",
    "Grammar" to "Grammar",
    "Reading" to "Reading",
    "Listening" to "Listening",
    "Speaking" to "Speaking",
    "Writing" to "Writing",
    "All Balanced" to "All Balanced"
)

@Composable
fun OnboardingScreen(
    profileViewModel: ProfileViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedGoal by remember { mutableStateOf("IELTS Exam") }
    var selectedLevel by remember { mutableStateOf("B1") }
    var selectedTimeMinutes by remember { mutableStateOf("30") }
    var selectedWeakestSkill by remember { mutableStateOf("Speaking") }
    var targetScoreInput by remember { mutableStateOf("7.5") }

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(
                                start = Dimens.screenGutter,
                                end = Dimens.screenGutter,
                                top = Dimens.blockGap,
                                bottom = Dimens.blockGap
                            ),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.blockGap)
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { step -= 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = Dimens.minTapTarget),
                                shape = RoundedCornerShape(Dimens.radiusMd)
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
                                        dailyMinutes = selectedTimeMinutes.toInt(),
                                        targetScore = targetScoreInput
                                    )
                                    onComplete()
                                }
                            },
                            modifier = Modifier
                                .weight(if (step > 1) 2f else 1f)
                                .heightIn(min = Dimens.minTapTarget),
                            shape = RoundedCornerShape(Dimens.radiusMd)
                        ) {
                            Text(
                                text = if (step == 5) "Get Started with LinguaFa" else "Next Step",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.screenGutter),
                verticalArrangement = Arrangement.spacedBy(Dimens.sectionGap)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Step $step of 5",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Personalize Your Plan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LinearProgressIndicator(
                    progress = { (step / 5f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.progressHeight)
                        .clip(RoundedCornerShape(Dimens.radiusPill)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                when (step) {
                    1 -> OnboardingStep(
                        title = "What is your main language learning goal?",
                        subtitle = "Your study path and vocabulary recommendations will be personalized to your target."
                    ) {
                        ChoiceList(
                            options = goalOptions,
                            selectedValue = selectedGoal,
                            onSelected = { selectedGoal = it }
                        )
                    }
                    2 -> OnboardingStep(
                        title = "What is your approximate English level?",
                        subtitle = "If you are unsure, choose Not Sure and take the Smart Placement Test."
                    ) {
                        ChoiceList(
                            options = levelOptions,
                            selectedValue = selectedLevel,
                            onSelected = { selectedLevel = it }
                        )
                    }
                    3 -> OnboardingStep(
                        title = "How much time can you study daily?",
                        subtitle = "Your reviews and lessons will be scheduled into manageable daily sessions."
                    ) {
                        ChoiceList(
                            options = timeOptions,
                            selectedValue = selectedTimeMinutes,
                            onSelected = { selectedTimeMinutes = it }
                        )
                    }
                    4 -> OnboardingStep(
                        title = "Which skill needs the most improvement?"
                    ) {
                        ChoiceList(
                            options = skillOptions,
                            selectedValue = selectedWeakestSkill,
                            onSelected = { selectedWeakestSkill = it }
                        )
                    }
                    5 -> OnboardingStep(
                        title = "What is your target exam score or band?",
                        subtitle = "Example: Band 7.5 in IELTS, 100 in TOEFL, or CEFR C1"
                    ) {
                        FieldLabel(text = "Target Score or Band")
                        OutlinedTextField(
                            value = targetScoreInput,
                            onValueChange = { targetScoreInput = it },
                            singleLine = true,
                            shape = RoundedCornerShape(Dimens.radiusMd),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.heightIn(min = Dimens.space8))
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.blockGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

@Composable
private fun ChoiceList(
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        options.forEach { (label, value) ->
            SelectChip(
                text = label,
                selected = selectedValue == value,
                onClick = { onSelected(value) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
