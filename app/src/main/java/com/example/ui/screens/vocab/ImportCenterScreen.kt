package com.example.ui.screens.vocab

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.importer.DuplicateAction
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.components.PersianSectionHeader
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCenterScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit,
    onNavigateToAddManual: () -> Unit,
    onNavigateToAiGenerate: () -> Unit,
    onNavigateToPacks: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedMethod by remember { mutableStateOf("PASTE") } // PASTE, CSV, JSON, EXCEL
    var rawInputText by remember { mutableStateOf("") }
    var selectedDuplicateAction by remember { mutableStateOf(DuplicateAction.MERGE) }
    var duplicateDropdownExpanded by remember { mutableStateOf(false) }

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "ورود متمرکز لغات (Import Center)",
                    onBack = onBack
                )
            }
        ) { paddingValues ->
            if (state.fileImportPreview.isNotEmpty()) {
                // Show Import Preview Table
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    // Preview Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "پیش‌نمایش استخراج لغات (${state.fileImportPreview.count { it.isSelected }} از ${state.fileImportPreview.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row {
                                    OutlinedButton(
                                        onClick = { viewModel.selectAllFilePreview(true) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("انتخاب همه", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { viewModel.selectAllFilePreview(false) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("عدم انتخاب", fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Duplicate Strategy Selector
                            ExposedDropdownMenuBox(
                                expanded = duplicateDropdownExpanded,
                                onExpandedChange = { duplicateDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = when (selectedDuplicateAction) {
                                        DuplicateAction.MERGE -> "ادغام با لغات قبلی (Merge - پیشنهادی)"
                                        DuplicateAction.SKIP -> "نادیده‌گرفتن تکراری‌ها (Skip)"
                                        DuplicateAction.UPDATE -> "به‌روزرسانی با اطلاعات جدید (Update)"
                                        DuplicateAction.KEEP_BOTH -> "ثبت مجدد هر دو نسخه (Keep Both)"
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("شیوه مدیریت لغات تکراری") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = duplicateDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = duplicateDropdownExpanded,
                                    onDismissRequest = { duplicateDropdownExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("ادغام با لغات قبلی (Merge - پیشنهادی)") },
                                        onClick = {
                                            selectedDuplicateAction = DuplicateAction.MERGE
                                            duplicateDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("نادیده‌گرفتن تکراری‌ها (Skip)") },
                                        onClick = {
                                            selectedDuplicateAction = DuplicateAction.SKIP
                                            duplicateDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("به‌روزرسانی با اطلاعات جدید (Update)") },
                                        onClick = {
                                            selectedDuplicateAction = DuplicateAction.UPDATE
                                            duplicateDropdownExpanded = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("ثبت مجدد هر دو نسخه (Keep Both)") },
                                        onClick = {
                                            selectedDuplicateAction = DuplicateAction.KEEP_BOTH
                                            duplicateDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Items List Preview
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(state.fileImportPreview) { index, pItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleFilePreviewItem(index) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (pItem.isDuplicate) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surface
                                ),
                                border = if (pItem.isDuplicate) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = pItem.isSelected,
                                        onCheckedChange = { viewModel.toggleFilePreviewItem(index) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = pItem.word,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            CefrBadge(level = pItem.cefrLevel)
                                            if (pItem.isDuplicate) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = Color(0xFFFEF3C7),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "قبلاً ثبت شده",
                                                        color = Color(0xFFB45309),
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        if (pItem.persianMeaning.isNotEmpty()) {
                                            Text(
                                                text = pItem.persianMeaning,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Import Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearFilePreview() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("انصراف")
                        }

                        Button(
                            onClick = {
                                viewModel.confirmImportFileList(selectedDuplicateAction)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.weight(2f)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تایید و واردسازی (${state.fileImportPreview.count { it.isSelected }} واژه)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Method Selection & Inputs
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Action Buttons
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "روش‌های افزودن سریع:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                QuickMethodButton(
                                    icon = Icons.Default.Add,
                                    title = "افزودن دستی",
                                    color = PrimaryBlue,
                                    onClick = onNavigateToAddManual,
                                    modifier = Modifier.weight(1f)
                                )
                                QuickMethodButton(
                                    icon = Icons.Default.AutoAwesome,
                                    title = "ساخت با AI",
                                    color = PrimaryBlue,
                                    onClick = onNavigateToAiGenerate,
                                    modifier = Modifier.weight(1f)
                                )
                                QuickMethodButton(
                                    icon = Icons.Default.Inventory2,
                                    title = "بسته‌های لغت",
                                    color = PrimaryBlue,
                                    onClick = onNavigateToPacks,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Format Selection Tabs
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "واردسازی از فایل و متن:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FormatTabButton(
                                    icon = Icons.Default.ContentPaste,
                                    label = "Paste متن",
                                    isSelected = selectedMethod == "PASTE",
                                    onClick = {
                                        selectedMethod = "PASTE"
                                        rawInputText = "mitigate = کاهش دادن\nsubstantial = قابل توجه\nallocate = اختصاص دادن\ninevitable = اجتناب‌ناپذیر\nenhance = بهبود بخشیدن"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FormatTabButton(
                                    icon = Icons.Default.Description,
                                    label = "فایل CSV",
                                    isSelected = selectedMethod == "CSV",
                                    onClick = {
                                        selectedMethod = "CSV"
                                        rawInputText = "word,persianMeaning,level\nmitigate,کاهش دادن,C1\nallocate,اختصاص دادن,B2\nsubstantial,قابل توجه,B2\ninevitable,اجتناب‌ناپذیر,B2\nenhance,بهبود دادن,B2"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FormatTabButton(
                                    icon = Icons.Default.DataObject,
                                    label = "فایل JSON",
                                    isSelected = selectedMethod == "JSON",
                                    onClick = {
                                        selectedMethod = "JSON"
                                        rawInputText = """[
  {"word": "mitigate", "persianMeaning": "کاهش دادن", "cefrLevel": "C1", "tags": ["IELTS"]},
  {"word": "allocate", "persianMeaning": "اختصاص دادن", "cefrLevel": "B2", "tags": ["Academic"]},
  {"word": "substantial", "persianMeaning": "قابل توجه", "cefrLevel": "B2", "tags": ["Data"]}
]"""
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Format Hint
                            val hintText = when (selectedMethod) {
                                "PASTE" -> "می‌توانید کلمات را خط به خط، یا با فرمت 'word = معنی' کپی و جای‌گذاری نمایید."
                                "CSV" -> "فرمت استاندارد: word,persianMeaning,level ستون‌ها به طور خودکار شناسایی می‌شوند."
                                "JSON" -> "فرمت استاندارد تولیدشده توسط ChatGPT، Gemini یا خروجی همین اپلیکیشن."
                                else -> ""
                            }
                            Text(
                                text = hintText,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Text Area
                            OutlinedTextField(
                                value = rawInputText,
                                onValueChange = { rawInputText = it },
                                placeholder = { Text("محتوای متنی یا فایل خود را اینجا بچسبانید (Paste)...") },
                                minLines = 7,
                                maxLines = 14,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    if (rawInputText.isNotBlank()) {
                                        viewModel.parseImportContent(rawInputText, selectedMethod)
                                    }
                                },
                                enabled = rawInputText.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("پردازش و نمایش پیش‌نمایش", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickMethodButton(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun FormatTabButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
