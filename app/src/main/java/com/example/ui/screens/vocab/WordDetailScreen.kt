package com.example.ui.screens.vocab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.audio.TtsManager
import com.example.ui.components.CefrBadge
import com.example.ui.components.LinguaTopAppBar
import com.example.ui.components.PersianRtlLayout
import com.example.ui.theme.AccentGold

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
    DisposableEffect(tts) {
        onDispose { tts.shutdown() }
    }
    var selectedLangTab by remember { mutableStateOf(0) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    PersianRtlLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                LinguaTopAppBar(
                    title = "جزئیات واژه",
                    onBack = onBack,
                    actions = {
                        item?.let { current ->
                            IconButton(onClick = { viewModel.toggleFavorite(current) }) {
                                Icon(
                                    imageVector = if (current.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "نشان‌کردن",
                                    tint = if (current.isFavorite) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            val currentWord = item
            if (currentWord == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "در حال بارگذاری واژه…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WordHeroCard(
                    word = currentWord.word,
                    ipa = currentWord.ipa,
                    level = currentWord.cefrLevel,
                    onPronounce = { tts.speak(currentWord.word) }
                )

                LanguageTabs(
                    selectedTab = selectedLangTab,
                    onSelected = { selectedLangTab = it }
                )

                DefinitionCard(
                    isPersian = selectedLangTab == 0,
                    persianMeaning = currentWord.persianMeaning,
                    englishDefinition = currentWord.englishDefinition
                )

                if (currentWord.example.isNotBlank()) {
                    ExampleCard(
                        example = currentWord.example,
                        translation = currentWord.examplePersian,
                        onPronounce = { tts.speak(currentWord.example) }
                    )
                }

                if (currentWord.collocations.isNotEmpty()) {
                    DetailListCard(
                        title = "کالوکیشن‌ها",
                        subtitle = "ترکیب‌های طبیعی و پرتکرار",
                        items = currentWord.collocations
                    )
                }

                if (currentWord.synonyms.isNotEmpty()) {
                    DetailListCard(
                        title = "مترادف‌ها",
                        subtitle = "واژه‌های نزدیک از نظر معنا",
                        items = currentWord.synonyms
                    )
                }

                if (currentWord.commonMistakes.isNotBlank()) {
                    CommonMistakeCard(text = currentWord.commonMistakes)
                }

                ExamRelevanceCard(
                    ielts = currentWord.ieltsRelevance,
                    toefl = currentWord.toeflRelevance
                )

                ReviewActionCard(
                    feedbackMessage = feedbackMessage,
                    onKnown = { feedbackMessage = "این واژه به‌عنوان «بلدم» ثبت شد." },
                    onPractice = { feedbackMessage = "برای مرور نزدیک علامت‌گذاری شد." },
                    onHard = { feedbackMessage = "در مرورهای با اولویت بالاتر قرار گرفت." }
                )

                Spacer(modifier = Modifier.height(22.dp))
            }
        }
    }
}

@Composable
private fun WordHeroCard(
    word: String,
    ipa: String,
    level: String,
    onPronounce: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (ipa.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ipa,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.68f)
                            )
                        }
                    }
                    CefrBadge(level = level)
                }
            }

            Surface(
                onClick = onPronounce,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text("پخش تلفظ", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun LanguageTabs(
    selectedTab: Int,
    onSelected: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onSelected(0) },
            text = { Text("فارسی", style = MaterialTheme.typography.labelLarge) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onSelected(1) },
            text = { Text("English", style = MaterialTheme.typography.labelLarge) }
        )
    }
}

@Composable
private fun DefinitionCard(
    isPersian: Boolean,
    persianMeaning: String,
    englishDefinition: String
) {
    SectionCard(title = if (isPersian) "معنی و تعریف" else "Definition") {
        if (isPersian) {
            Text(
                text = persianMeaning,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (englishDefinition.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = englishDefinition,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        } else {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = englishDefinition.ifBlank { persianMeaning },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun ExampleCard(
    example: String,
    translation: String,
    onPronounce: () -> Unit
) {
    SectionCard(
        title = "مثال",
        trailing = {
            IconButton(onClick = onPronounce, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "پخش مثال",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = example,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
        if (translation.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailListCard(
    title: String,
    subtitle: String,
    items: List<String>
) {
    SectionCard(title = title, subtitle = subtitle) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommonMistakeCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PriorityHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "اشتباه رایج",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ExamRelevanceCard(
    ielts: String,
    toefl: String
) {
    SectionCard(title = "کاربرد در آزمون‌ها") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExamRow(exam = "IELTS", relevance = ielts)
            ExamRow(exam = "TOEFL", relevance = toefl)
        }
    }
}

@Composable
private fun ExamRow(exam: String, relevance: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(11.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exam,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = relevance.ifBlank { "—" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ReviewActionCard(
    feedbackMessage: String?,
    onKnown: () -> Unit,
    onPractice: () -> Unit,
    onHard: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "وضعیت یادگیری",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "برای مرورهای بعدی مشخص کن این واژه چقدر برات آشناست.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onKnown,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("بلدم")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPractice,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("نیاز به تمرین")
                }
                OutlinedButton(
                    onClick = onHard,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                ) {
                    Text("سخت بود", color = MaterialTheme.colorScheme.error)
                }
            }

            if (feedbackMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (trailing != null) trailing()
            }
            Spacer(modifier = Modifier.height(11.dp))
            content()
        }
    }
}
