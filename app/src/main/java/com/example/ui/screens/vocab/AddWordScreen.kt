package com.example.ui.screens.vocab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var wordInput by remember { mutableStateOf("") }
    var meaningInput by remember { mutableStateOf("") }
    var definitionInput by remember { mutableStateOf("") }
    var exampleInput by remember { mutableStateOf("") }
    var exampleFaInput by remember { mutableStateOf("") }
    var selectedLevel by remember { mutableStateOf("B2") }
    var levelDropdownExpanded by remember { mutableStateOf(false) }

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "افزودن لغت جدید",
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "مشخصات اصلی واژه",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // English Word
                        OutlinedTextField(
                            value = wordInput,
                            onValueChange = { wordInput = it },
                            label = { Text("واژه انگلیسی (English Word)*") },
                            placeholder = { Text("مثال: mitigate") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Persian Meaning
                        OutlinedTextField(
                            value = meaningInput,
                            onValueChange = { meaningInput = it },
                            label = { Text("معنی فارسی (اختیاری در صورت استفاده از AI)") },
                            placeholder = { Text("مثال: کاهش دادن، تعدیل کردن") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // AI Auto-Complete Button
                        Button(
                            onClick = {
                                if (wordInput.isNotBlank()) {
                                    viewModel.enrichWordWithAi(wordInput, meaningInput) { enriched ->
                                        if (enriched != null) {
                                            meaningInput = enriched.persianMeaning
                                            definitionInput = enriched.englishDefinition
                                            exampleInput = enriched.example
                                            exampleFaInput = enriched.examplePersian
                                            selectedLevel = enriched.cefrLevel
                                        }
                                    }
                                }
                            },
                            enabled = wordInput.isNotBlank() && !state.isAiGenerating,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isAiGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال پردازش هوش مصنوعی...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تکمیل خودکار اطلاعات با هوش مصنوعی (Gemini)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Detailed optional fields
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "تعریف، مثال و سطح آموزشی",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // English definition
                        OutlinedTextField(
                            value = definitionInput,
                            onValueChange = { definitionInput = it },
                            label = { Text("تعریف انگلیسی (Definition)") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Example Sentence
                        OutlinedTextField(
                            value = exampleInput,
                            onValueChange = { exampleInput = it },
                            label = { Text("جمله مثال به انگلیسی") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Example Persian translation
                        OutlinedTextField(
                            value = exampleFaInput,
                            onValueChange = { exampleFaInput = it },
                            label = { Text("ترجمه فارسی مثال") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // CEFR Level Selector
                        ExposedDropdownMenuBox(
                            expanded = levelDropdownExpanded,
                            onExpandedChange = { levelDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedLevel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("سطح CEFR") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = levelDropdownExpanded,
                                onDismissRequest = { levelDropdownExpanded = false }
                            ) {
                                listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { lvl ->
                                    DropdownMenuItem(
                                        text = { Text(lvl) },
                                        onClick = {
                                            selectedLevel = lvl
                                            levelDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Submit Button
                Button(
                    onClick = {
                        if (wordInput.isNotBlank()) {
                            viewModel.addWordManually(
                                word = wordInput,
                                meaning = meaningInput.ifEmpty { "بدون ترجمه اولیه" },
                                definition = definitionInput,
                                example = exampleInput,
                                exampleFa = exampleFaInput,
                                level = selectedLevel
                            )
                            onBack()
                        }
                    },
                    enabled = wordInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ذخیره در لغات من", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
