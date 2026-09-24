package com.example.ui.screens.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun WritingGraderScreen(
    viewModel: ExamsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val activePrompt = state.prompts.getOrNull(state.selectedPromptIndex)

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "تصحیح و ارزیابی رایتینگ هوشمند",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Topic Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.prompts.forEachIndexed { index, prompt ->
                        FilterChip(
                            selected = state.selectedPromptIndex == index,
                            onClick = { viewModel.selectPrompt(index) },
                            label = { Text(prompt.titleFa.take(28) + "...", fontSize = 11.sp) }
                        )
                    }
                }

                if (activePrompt != null) {
                    // Prompt Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = activePrompt.titleEn,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activePrompt.promptTextEn,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "راهنمای فارسی: ${activePrompt.guideFa}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    // Essay Text Field Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "متن مقاله شما (Essay):",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "تعداد واژگان: ${state.wordCount} کلمه (حداقل ۲۵۰ کلمه)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (state.wordCount >= 250) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = state.essayInput,
                                onValueChange = { viewModel.onEssayInputChanged(it) },
                                placeholder = { Text("Write your essay in English here...") },
                                minLines = 8,
                                maxLines = 16,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { viewModel.submitEssayForGrading() },
                                enabled = state.essayInput.isNotBlank() && !state.isEvaluatingWriting,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (state.isEvaluatingWriting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("در حال ارزیابی طبق ۴ معیار رسمی آیلتس...", fontSize = 12.sp)
                                } else {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ارزیابی و نمره‌دهی با هوش مصنوعی", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Evaluation Result Card
                    state.writingResult?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "نمره تخمینی آیلتس: Band ${result.estimatedBand}",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryBlue
                                            )
                                        )
                                        Text(
                                            text = "⚠️ این نمره تخمینی هوش مصنوعی است و نمره رسمی IELTS نیست.",
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // 4 Criteria Subscores
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ScoreBadge("Task Response", result.taskAchievementScore, Modifier.weight(1f))
                                    ScoreBadge("Coherence", result.coherenceScore, Modifier.weight(1f))
                                    ScoreBadge("Lexical", result.lexicalScore, Modifier.weight(1f))
                                    ScoreBadge("Grammar", result.grammarScore, Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Feedback Text
                                Text(
                                    text = "ارزیابی کلی به فارسی:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.overallFeedbackFa,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
                                )

                                if (result.strengthsFa.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "نقاط قوت مقاله:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                                    )
                                    result.strengthsFa.forEach { s ->
                                        Text("• $s", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }

                                if (result.mainIssuesFa.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "مشکلات اصلی نیازمند اصلاح:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = ErrorRed)
                                    )
                                    result.mainIssuesFa.forEach { issue ->
                                        Text("• $issue", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }

                                if (result.sentenceCorrections.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "جملاتی که باید اصلاح شوند:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    result.sentenceCorrections.forEach { corr ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("جمله اصلی: ${corr.original}", style = MaterialTheme.typography.bodySmall.copy(color = ErrorRed))
                                                Text("شکل اصلاح‌شده: ${corr.corrected}", style = MaterialTheme.typography.bodySmall.copy(color = SuccessGreen, fontWeight = FontWeight.Bold))
                                                if (corr.explanationFa.isNotEmpty()) {
                                                    Text("دلیل به فارسی: ${corr.explanationFa}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ScoreBadge(
    title: String,
    score: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = score, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue))
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), maxLines = 1)
        }
    }
}
