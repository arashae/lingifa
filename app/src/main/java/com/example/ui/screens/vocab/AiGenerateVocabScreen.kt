package com.example.ui.screens.vocab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.PrimaryBlue

@Composable
fun AiGenerateVocabScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var promptInput by remember { mutableStateOf("۵۰ لغت مهم آیلتس سطح B2 و C1 درباره محیط زیست و اقتصاد") }

    val samplePrompts = listOf(
        "۵۰ لغت مهم آیلتس سطح B2 درباره محیط زیست",
        "لغات ضروری برای IELTS Writing Task 2 درباره تکنولوژی",
        "لغات آکادمیک تافل ۲۰۲۶ برای مباحثه دانشگاهی",
        "کالوکیشن‌های سطح C1 برای افزایش نمره رایتینگ"
    )

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "ساخت بسته لغت با AI",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Prompt Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "موضوع و سطح واژگان مورد نظر را به فارسی بنویسید:",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Prompt chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            samplePrompts.forEach { p ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.clickable { promptInput = p }
                                ) {
                                    Text(
                                        text = p,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = promptInput,
                            onValueChange = { promptInput = it },
                            placeholder = { Text("مثال: ۳۰ لغت آکادمیک برای اسپیکینگ پارت ۳ آیلتس...") },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (promptInput.isNotBlank()) {
                                    viewModel.generateVocabWithAi(promptInput)
                                }
                            },
                            enabled = promptInput.isNotBlank() && !state.isAiGenerating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isAiGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال ساخت بسته لغت با جمینای...", fontSize = 13.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تولید بسته واژگان هوشمند", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Preview List or Empty State
                if (state.aiGeneratedPreview.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پیش‌نمایش (${state.aiGeneratedPreview.count { it.isSelected }} از ${state.aiGeneratedPreview.size} انتخاب‌شده)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row {
                            OutlinedButton(
                                onClick = { viewModel.selectAllAiPreview(true) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("همه", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { viewModel.selectAllAiPreview(false) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("هیچکدام", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(state.aiGeneratedPreview) { index, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAiPreviewItem(index) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isSelected,
                                        onCheckedChange = { viewModel.toggleAiPreviewItem(index) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.word,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            CefrBadge(level = item.cefrLevel)
                                        }
                                        Text(
                                            text = item.persianMeaning,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        if (item.example.isNotEmpty()) {
                                            Text(
                                                text = item.example,
                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.confirmImportAiList()
                            onBack()
                        },
                        enabled = state.aiGeneratedPreview.any { it.isSelected },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "افزودن ${state.aiGeneratedPreview.count { it.isSelected }} لغت به لغات من",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "درخواست خود را بنویسید و دکمه تولید را بزنید تا پیش‌نمایش واژگان هوشمند ظاهر شود.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }
    }
}
