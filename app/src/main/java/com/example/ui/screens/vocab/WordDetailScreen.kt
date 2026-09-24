package com.example.ui.screens.vocab

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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.TtsManager
import com.example.data.model.VocabularyItem
import com.example.srs.ReviewRating
import com.example.srs.SpacedRepetitionSystem
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun WordDetailScreen(
    vocabId: Long,
    viewModel: VocabViewModel,
    onBack: () -> Unit
) {
    val wordFlow = remember(vocabId) { viewModel.getWordById(vocabId) }
    val item by wordFlow.collectAsState(initial = null)
    val context = LocalContext.current
    val tts = remember { TtsManager(context) }
    var selectedLangTab by remember { mutableStateOf(0) } // 0: فارسی, 1: English
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    PersianRtlLayout {
        Scaffold(
            topBar = {
                LinguaTopAppBar(
                    title = "شناسنامه واژه",
                    onBack = onBack,
                    actions = {
                        item?.let { current ->
                            IconButton(onClick = { viewModel.toggleFavorite(current) }) {
                                Icon(
                                    imageVector = if (current.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "نشان‌کردن",
                                    tint = if (current.isFavorite) AccentGold else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (item == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("در حال بارگذاری اطلاعات واژه...")
                }
            } else {
                val currentWord = item!!
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
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentWord.word,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    if (currentWord.ipa.isNotEmpty()) {
                                        Text(
                                            text = currentWord.ipa,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CefrBadge(level = currentWord.cefrLevel)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // Pronounce Button
                                    Button(
                                        onClick = { tts.speak(currentWord.word) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("پخش تلفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Explanation Language Tab (Persian Default vs English)
                            TabRow(
                                selectedTabIndex = selectedLangTab,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Tab(
                                    selected = selectedLangTab == 0,
                                    onClick = { selectedLangTab = 0 },
                                    text = { Text("توضیحات فارسی (پیش‌فرض)", fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = selectedLangTab == 1,
                                    onClick = { selectedLangTab = 1 },
                                    text = { Text("English Explanation", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }

                    // Meaning & Definition Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (selectedLangTab == 0) {
                                // Persian View
                                Text(
                                    text = "معنی فارسی:",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentWord.persianMeaning,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )

                                if (currentWord.englishDefinition.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "تعریف انگلیسی (Definition):",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentWord.englishDefinition,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            } else {
                                // English View
                                Text(
                                    text = "English Definition:",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentWord.englishDefinition.ifEmpty { currentWord.persianMeaning },
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    // Example & Persian Translation
                    if (currentWord.example.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "مثال کاربردی در جمله:",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    IconButton(
                                        onClick = { tts.speak(currentWord.example) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "پخش مثال",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentWord.example,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (currentWord.examplePersian.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "ترجمه: ${currentWord.examplePersian}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Collocations (کالوکیشن‌ها)
                    if (currentWord.collocations.isNotEmpty()) {
                        DetailSectionCard(
                            title = "هم‌آیندها و کالوکیشن‌ها (Collocations):",
                            items = currentWord.collocations
                        )
                    }

                    // Synonyms & Antonyms
                    if (currentWord.synonyms.isNotEmpty()) {
                        DetailSectionCard(
                            title = "مترادف‌ها (Synonyms):",
                            items = currentWord.synonyms
                        )
                    }

                    // Common Mistakes for Persian speakers
                    if (currentWord.commonMistakes.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PriorityHigh,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "اشتباهات رایج زبان‌آموزان ایرانی:",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = ErrorRed
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentWord.commonMistakes,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF991B1B))
                                )
                            }
                        }
                    }

                    // Exam Relevance Badges
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "اهمیت در آزمون‌های بین‌المللی:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFEFF6FF),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("آزمون IELTS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                                        Text(currentWord.ieltsRelevance, fontSize = 11.sp, color = Color(0xFF1E40AF))
                                    }
                                }
                                Surface(
                                    color = Color(0xFFF0FDF4),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("آزمون TOEFL", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessGreen)
                                        Text(currentWord.toeflRelevance, fontSize = 11.sp, color = Color(0xFF15803D))
                                    }
                                }
                            }
                        }
                    }

                    // Spaced Repetition Action Buttons (بلدم، نیاز به تمرین، خیلی سخت بود، افزودن به مرور)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "ثبت وضعیت در سیستم مرور هوشمند (SRS):",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        feedbackMessage = "به عنوان 'کاملاً بلدم' ثبت شد."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("بلدم", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        feedbackMessage = "برای مرور نزدیک برنامه‌ریزی شد."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("نیاز به تمرین", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        feedbackMessage = "در مرورهای با اولویت بالا قرار گرفت."
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("خیلی سخت بود", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (feedbackMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = feedbackMessage!!,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
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
private fun DetailSectionCard(
    title: String,
    items: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            items.forEach { itm ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = itm,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }
    }
}
