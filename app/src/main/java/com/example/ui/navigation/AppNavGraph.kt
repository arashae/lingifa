package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.EnglishLtrLayout
import com.example.ui.theme.Dimens
import com.example.ui.screens.exams.DiagnosticTestScreen
import com.example.ui.screens.exams.ExamsViewModel
import com.example.ui.screens.exams.SpeakingPracticeScreen
import com.example.ui.screens.exams.WritingGraderScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.learn.GrammarDetailScreen
import com.example.ui.screens.learn.LearnHomeScreen
import com.example.ui.screens.learn.LearnViewModel
import com.example.ui.screens.learn.ListeningDetailScreen
import com.example.ui.screens.learn.ReadingDetailScreen
import com.example.ui.screens.mistakes.MistakeNotebookScreen
import com.example.ui.screens.onboarding.OnboardingScreen
import com.example.ui.screens.practice.PracticeScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.review.ReviewViewModel
import com.example.ui.screens.review.SrsReviewScreen
import com.example.ui.screens.tutor.AiTutorScreen
import com.example.ui.screens.tutor.TutorViewModel
import com.example.ui.screens.vocab.AddWordScreen
import com.example.ui.screens.vocab.AiGenerateVocabScreen
import com.example.ui.screens.vocab.AiVocabCardScreen
import com.example.ui.screens.vocab.ExamTrackScreen
import com.example.ui.screens.vocab.ImportCenterScreen
import com.example.ui.screens.vocab.VocabLibraryScreen
import com.example.ui.screens.vocab.VocabPacksScreen
import com.example.ui.screens.vocab.VocabViewModel
import com.example.ui.screens.vocab.WordDetailScreen

private data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
    BottomNavItem("Vocab", Screen.Vocab.route, Icons.Default.AutoStories),
    BottomNavItem("Exams", Screen.ExamTracks.route, Icons.Default.School),
    BottomNavItem("Review", Screen.SrsReview.route, Icons.Default.FitnessCenter),
    BottomNavItem("Profile", Screen.Profile.route, Icons.Default.Person)
)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute != null && bottomNavItems.any { it.route == currentRoute }
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    EnglishLtrLayout {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        tonalElevation = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.drawBehind {
                            drawLine(
                                color = borderColor,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        modifier = Modifier.size(Dimens.iconMd)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = viewModel<HomeViewModel>(),
                        onNavigateToVocab = { navController.navigate(Screen.Vocab.route) },
                        onNavigateToReview = { navController.navigate(Screen.SrsReview.route) },
                        onNavigateToTutor = { navController.navigate(Screen.AiTutor.route) },
                        onNavigateToLearn = { navController.navigate(Screen.Learn.route) },
                        onNavigateToSpeaking = { navController.navigate(Screen.SpeakingPractice.route) },
                        onNavigateToWriting = { navController.navigate(Screen.WritingGrader.route) },
                        onNavigateToDiagnostic = { navController.navigate(Screen.DiagnosticTest.route) },
                        onNavigateToMistakes = { navController.navigate(Screen.MistakeNotebook.route) },
                        onNavigateToAiVocabCard = { navController.navigate(Screen.AiVocabCard.route) },
                        onNavigateToExamTracks = { navController.navigate(Screen.ExamTracks.route) }
                    )
                }

                composable(Screen.Vocab.route) {
                    VocabLibraryScreen(
                        viewModel = viewModel<VocabViewModel>(),
                        onNavigateToDetail = { vocabId ->
                            navController.navigate(Screen.WordDetail.createRoute(vocabId))
                        },
                        onNavigateToAddWord = { navController.navigate(Screen.AddWord.route) },
                        onNavigateToAiGenerate = { navController.navigate(Screen.AiGenerateVocab.route) },
                        onNavigateToImportCenter = { navController.navigate(Screen.ImportCenter.route) },
                        onNavigateToPacks = { navController.navigate(Screen.VocabPacks.route) },
                        onNavigateToAiVocabCard = { navController.navigate(Screen.AiVocabCard.route) },
                        onNavigateToExamTracks = { navController.navigate(Screen.ExamTracks.route) },
                        onNavigateToReview = { navController.navigate(Screen.SrsReview.route) }
                    )
                }

                composable(Screen.Learn.route) {
                    LearnHomeScreen(
                        viewModel = viewModel<LearnViewModel>(),
                        onNavigateToGrammarDetail = { topicId ->
                            navController.navigate(Screen.GrammarDetail.createRoute(topicId))
                        },
                        onNavigateToReadingDetail = { passageId ->
                            navController.navigate(Screen.ReadingDetail.createRoute(passageId))
                        },
                        onNavigateToListeningDetail = { exerciseId ->
                            navController.navigate(Screen.ListeningDetail.createRoute(exerciseId))
                        }
                    )
                }

                composable(Screen.Practice.route) {
                    PracticeScreen(
                        onNavigateToReview = { navController.navigate(Screen.SrsReview.route) },
                        onNavigateToSpeaking = { navController.navigate(Screen.SpeakingPractice.route) },
                        onNavigateToWriting = { navController.navigate(Screen.WritingGrader.route) },
                        onNavigateToDiagnostic = { navController.navigate(Screen.DiagnosticTest.route) },
                        onNavigateToMistakes = { navController.navigate(Screen.MistakeNotebook.route) },
                        onNavigateToTutor = { navController.navigate(Screen.AiTutor.route) }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = viewModel<ProfileViewModel>(),
                        onNavigateToMistakes = { navController.navigate(Screen.MistakeNotebook.route) }
                    )
                }

                composable(
                    route = Screen.WordDetail.route,
                    arguments = listOf(navArgument("vocabId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val vocabId = backStackEntry.arguments?.getLong("vocabId") ?: 0L
                    WordDetailScreen(
                        vocabId = vocabId,
                        viewModel = viewModel<VocabViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AddWord.route) {
                    AddWordScreen(
                        viewModel = viewModel<VocabViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AiGenerateVocab.route) {
                    AiGenerateVocabScreen(
                        viewModel = viewModel<VocabViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AiVocabCard.route) {
                    AiVocabCardScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.ImportCenter.route) {
                    ImportCenterScreen(
                        viewModel = viewModel<VocabViewModel>(),
                        onBack = { navController.popBackStack() },
                        onNavigateToAddManual = { navController.navigate(Screen.AddWord.route) },
                        onNavigateToAiGenerate = { navController.navigate(Screen.AiGenerateVocab.route) },
                        onNavigateToPacks = { navController.navigate(Screen.VocabPacks.route) }
                    )
                }

                composable(Screen.VocabPacks.route) {
                    VocabPacksScreen(
                        viewModel = viewModel<VocabViewModel>(),
                        onBack = { navController.popBackStack() },
                        onFilterByPack = { navController.popBackStack() },
                        onStartCefrLevel = { navController.popBackStack() },
                        onNavigateToExamTracks = { navController.navigate(Screen.ExamTracks.route) }
                    )
                }

                composable(Screen.ExamTracks.route) {
                    ExamTrackScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SrsReview.route) {
                    SrsReviewScreen(
                        viewModel = viewModel<ReviewViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AiTutor.route) {
                    AiTutorScreen(
                        viewModel = viewModel<TutorViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.GrammarDetail.route,
                    arguments = listOf(navArgument("topicId") { type = NavType.StringType })
                ) { backStackEntry ->
                    GrammarDetailScreen(
                        topicId = backStackEntry.arguments?.getString("topicId") ?: "",
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ReadingDetail.route,
                    arguments = listOf(navArgument("passageId") { type = NavType.StringType })
                ) { backStackEntry ->
                    ReadingDetailScreen(
                        passageId = backStackEntry.arguments?.getString("passageId") ?: "",
                        viewModel = viewModel<LearnViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ListeningDetail.route,
                    arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    ListeningDetailScreen(
                        exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: "",
                        viewModel = viewModel<LearnViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.WritingGrader.route) {
                    WritingGraderScreen(
                        viewModel = viewModel<ExamsViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SpeakingPractice.route) {
                    SpeakingPracticeScreen(
                        viewModel = viewModel<ExamsViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.MistakeNotebook.route) {
                    MistakeNotebookScreen(
                        viewModel = viewModel<ProfileViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.DiagnosticTest.route) {
                    DiagnosticTestScreen(
                        viewModel = viewModel<ExamsViewModel>(),
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        profileViewModel = viewModel<ProfileViewModel>(),
                        onComplete = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
