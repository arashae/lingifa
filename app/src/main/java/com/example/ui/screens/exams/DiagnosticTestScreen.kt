package com.example.ui.screens.exams

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun DiagnosticTestScreen(
    viewModel: ExamsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "آزمون تعیین سطح هوشمند",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            if (state.diagnosticFinished) {
                // Result screen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDCFCE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "آزمون تعیین سطح به پایان رسید!",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "سطح تخمینی شما بر اساس چارچوب اروپایی CEFR:",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = state.estimatedCefrLevel ?: "B2",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "امتیاز: ${state.diagnosticScore} از ${state.diagnosticQuestions.size} پاسخ صحیح",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "سطح پروفایل شما به‌روزرسانی شد و تمرین‌ها بر این اساس شخصی‌سازی می‌شوند.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = PrimaryBlue),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = onBack,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تایید و بازگشت به یادگیری", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                val currentQ = state.diagnosticQuestions.getOrNull(state.diagnosticCurrentIndex)

                if (currentQ != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                        // Progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "سوال ${state.diagnosticCurrentIndex + 1} از ${state.diagnosticQuestions.size}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            CefrBadge(level = currentQ.testedLevel)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { ((state.diagnosticCurrentIndex + 1).toFloat() / state.diagnosticQuestions.size).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PrimaryBlue,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = currentQ.questionEn,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Options
                        currentQ.options.forEachIndexed { optIndex, optText ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clickable { viewModel.answerDiagnosticQuestion(optIndex) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "${optIndex + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
