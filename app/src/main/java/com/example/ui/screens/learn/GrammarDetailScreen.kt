package com.example.ui.screens.learn

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.GrammarTopic
import com.example.data.seed.GrammarSeed
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun GrammarDetailScreen(
    topicId: String,
    onBack: () -> Unit
) {
    val topic = remember(topicId) {
        GrammarSeed.getTopics().find { it.id == topicId } ?: GrammarSeed.getTopics().first()
    }

    var selectedOptionIndices by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var isAnswerChecked by remember { mutableStateOf(mutableMapOf<Int, Boolean>()) }

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = topic.titleFa,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topic.titleEn,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            )
                            CefrBadge(level = topic.level)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = topic.descriptionFa,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }

                // Rules & Structure
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "قواعد و ساختار گرامری:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = topic.rulesFa,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp)
                        )
                    }
                }

                // Examples
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "مثال‌های آکادمیک و کاربردی:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        topic.examplesEn.forEachIndexed { index, exEn ->
                            val exFa = topic.examplesFa.getOrNull(index) ?: ""
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = exEn,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (exFa.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ترجمه: $exFa",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Iranian Learner Common Mistakes
                if (topic.iranianCommonMistakesFa.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PriorityHigh, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اشتباهات رایج زبان‌آموزان ایرانی:",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = ErrorRed
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = topic.iranianCommonMistakesFa,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF991B1B), lineHeight = 22.sp)
                            )
                        }
                    }
                }

                // Interactive Quiz
                if (topic.quizQuestions.isNotEmpty()) {
                    Text(
                        text = "آزمونک تثبیت گرامر:",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    topic.quizQuestions.forEachIndexed { qIndex, q ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "سوال ${qIndex + 1}: ${q.questionEn}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val userSelected = selectedOptionIndices[qIndex]
                                val checked = isAnswerChecked[qIndex] == true

                                q.options.forEachIndexed { optIndex, optText ->
                                    val isThisSelected = userSelected == optIndex
                                    val isCorrect = optIndex == q.correctIndex

                                    val bg = when {
                                        !checked -> if (isThisSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        isCorrect -> Color(0xFFDCFCE7)
                                        isThisSelected -> Color(0xFFFEE2E2)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Surface(
                                        color = bg,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable(enabled = !checked) {
                                                val m = selectedOptionIndices.toMutableMap()
                                                m[qIndex] = optIndex
                                                selectedOptionIndices = m
                                            }
                                    ) {
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isThisSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (checked && isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (!checked && userSelected != null) {
                                    Button(
                                        onClick = {
                                            val m = isAnswerChecked.toMutableMap()
                                            m[qIndex] = true
                                            isAnswerChecked = m
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                    ) {
                                        Text("بررسی پاسخ", fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (checked) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "توضیح فارسی: ${q.explanationFa}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
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
