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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.seed.ReadingListeningSeed
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@Composable
fun ReadingDetailScreen(
    passageId: String,
    viewModel: LearnViewModel,
    onBack: () -> Unit
) {
    val passage = remember(passageId) {
        ReadingListeningSeed.getReadingPassages().find { it.id == passageId }
            ?: ReadingListeningSeed.getReadingPassages().first()
    }

    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    var userAnswers by remember { mutableStateOf(mutableMapOf<String, Int>()) }
    var checkedAnswers by remember { mutableStateOf(mutableMapOf<String, Boolean>()) }

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = passage.titleFa,
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = passage.titleEn,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            )
                            CefrBadge(level = passage.level)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "💡 راهنما: روی هر کلمه در متن که معنی آن را نمی‌دانید ضربه بزنید تا معنی، تلفظ و گزینه افزودن به لغات شما نمایان شود.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                // Interactive Passage Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "متن آکادمیک (${passage.exam}):",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Render clickable paragraphs
                        val paragraphs = passage.contentEn.split("\n\n")
                        paragraphs.forEach { pText ->
                            InteractiveParagraph(
                                text = pText,
                                onWordClicked = { word -> viewModel.inspectWord(word) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                // Comprehension Questions
                if (passage.questions.isNotEmpty()) {
                    Text(
                        text = "سوالات درک مطلب (Comprehension Questions):",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    passage.questions.forEachIndexed { qIdx, q ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "سوال ${qIdx + 1}: ${q.questionEn}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val selected = userAnswers[q.id]
                                val isChecked = checkedAnswers[q.id] == true

                                q.options.forEachIndexed { optIdx, optText ->
                                    val isThisSelected = selected == optIdx
                                    val isCorrect = optIdx == q.correctIndex

                                    val bg = when {
                                        !isChecked -> if (isThisSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
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
                                            .clickable(enabled = !isChecked) {
                                                val m = userAnswers.toMutableMap()
                                                m[q.id] = optIdx
                                                userAnswers = m
                                            }
                                    ) {
                                        Text(
                                            text = optText,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isChecked && isCorrect) SuccessGreen else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (isThisSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }

                                if (!isChecked && selected != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val m = checkedAnswers.toMutableMap()
                                            m[q.id] = true
                                            checkedAnswers = m
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                    ) {
                                        Text("بررسی پاسخ")
                                    }
                                }

                                if (isChecked) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "تحلیل فارسی: ${q.explanationFa}",
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

            // Tap Word Detail Dialog Popup
            state.popupWordDetail?.let { detail ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissWordPopup() },
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = detail.word, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            IconButton(onClick = { tts.speak(detail.word) }) {
                                Icon(imageVector = Icons.Default.VolumeUp, contentDescription = "پخش تلفظ")
                            }
                        }
                    },
                    text = {
                        Column {
                            if (detail.ipa.isNotEmpty()) {
                                Text(text = detail.ipa, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(
                                text = "معنی: ${detail.persianMeaning}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (detail.englishDefinition.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "تعریف: ${detail.englishDefinition}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (state.isWordSavedStatus != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.isWordSavedStatus!!,
                                    color = SuccessGreen,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.saveInspectedWord(detail) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("افزودن به لغات من")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissWordPopup() }) {
                            Text("بستن")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun InteractiveParagraph(
    text: String,
    onWordClicked: (String) -> Unit
) {
    val words = text.split(" ")
    // We display words so that user can tap any word individually
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        words.forEach { rawWord ->
            Text(
                text = "$rawWord ",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .clickable {
                        val clean = rawWord.trim().replace(Regex("[^a-zA-Z]"), "")
                        if (clean.isNotEmpty()) {
                            onWordClicked(clean)
                        }
                    }
                    .padding(vertical = 1.dp)
            )
        }
    }
}
