package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.PersianRtlLayout
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
import com.example.ui.screens.vocab.ImportCenterScreen
import com.example.ui.screens.vocab.VocabLibraryScreen
import com.example.ui.screens.vocab.VocabPacksScreen
import com.example.ui.screens.vocab.VocabViewModel
import com.example.ui.screens.vocab.WordDetailScreen

data class BottomNavItem(
    val titleFa: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun AppNavGraph(
    homeViewModel: HomeViewModel = viewModel(),
    vocabViewModel: VocabViewModel = viewModel(),
    reviewViewModel: ReviewViewModel = viewModel(),
    tutorViewModel: TutorViewModel = viewModel(),
    learnViewModel: LearnViewModel = viewModel(),
    examsViewModel: ExamsViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomItems = listOf(
        BottomNavItem("خانه", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("لغات من", Screen.Vocab.route, Icons.Default.AutoStories),
        BottomNavItem("یادگیری", Screen.Learn.route, Icons.Default.School),
        BottomNavItem("تمرین", Screen.Practice.route, Icons.Default.FitnessCenter),
        BottomNavItem("پیشرفت", Screen.Profile.route, Icons.Default.Person)
    )

    val showBottomBar = currentRoute in bottomItems.map { it.route }

    PersianRtlLayout {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        bottomItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(imageVector = item.icon, contentDescription = item.titleFa)
                                },
                                label = {
                                    Text(text = item.titleFa, fontSize = 11.sp)
                                }
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
                // Bottom Bar Screens
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToVocab = { navController.navigate(Screen.Vocab.route) },
                        onNavigateToReview = {
                            reviewViewModel.startSession()
                            navController.navigate(Screen.SrsReview.route)
                        },
                        onNavigateToTutor = { navController.navigate(Screen.AiTutor.route) },
                        onNavigateToLearn = { navController.navigate(Screen.Learn.route) },
                        onNavigateToSpeaking = { navController.navigate(Screen.SpeakingPractice.route) },
                        onNavigateToWriting = { navController.navigate(Screen.WritingGrader.route) },
                        onNavigateToDiagnostic = { navController.navigate(Screen.DiagnosticTest.route) },
                        onNavigateToMistakes = { navController.navigate(Screen.MistakeNotebook.route) },
                        onNavigateToAiVocabCard = { navController.navigate(Screen.AiVocabCard.route) }
                    )
                }

                composable(Screen.Vocab.route) {
                    VocabLibraryScreen(
                        viewModel = vocabViewModel,
                        onNavigateToDetail = { vocabId ->
                            navController.navigate(Screen.WordDetail.createRoute(vocabId))
                        },
                        onNavigateToAddWord = { navController.navigate(Screen.AddWord.route) },
                        onNavigateToAiGenerate = { navController.navigate(Screen.AiGenerateVocab.route) },
                        onNavigateToImportCenter = { navController.navigate(Screen.ImportCenter.route) },
                        onNavigateToPacks = { navController.navigate(Screen.VocabPacks.route) },
                        onNavigateToAiVocabCard = { navController.navigate(Screen.AiVocabCard.route) }
                    )
                }

                composable(Screen.Learn.route) {
                    LearnHomeScreen(
                        viewModel = learnViewModel,
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
                        onNavigateToReview = {
                            reviewViewModel.startSession()
                            navController.navigate(Screen.SrsReview.route)
                        },
                        onNavigateToSpeaking = { navController.navigate(Screen.SpeakingPractice.route) },
                        onNavigateToWriting = { navController.navigate(Screen.WritingGrader.route) },
                        onNavigateToDiagnostic = { navController.navigate(Screen.DiagnosticTest.route) },
                        onNavigateToMistakes = { navController.navigate(Screen.MistakeNotebook.route) },
                        onNavigateToTutor = { navController.navigate(Screen.AiTutor.route) }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onNavigateToMistakes = { navController.navigate(Screen.MistakeNotebook.route) }
                    )
                }

                // Sub-screens
                composable(
                    route = Screen.WordDetail.route,
                    arguments = listOf(navArgument("vocabId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val vocabId = backStackEntry.arguments?.getLong("vocabId") ?: 0L
                    WordDetailScreen(
                        vocabId = vocabId,
                        viewModel = vocabViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AddWord.route) {
                    AddWordScreen(
                        viewModel = vocabViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AiGenerateVocab.route) {
                    AiGenerateVocabScreen(
                        viewModel = vocabViewModel,
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
                        viewModel = vocabViewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToAddManual = { navController.navigate(Screen.AddWord.route) },
                        onNavigateToAiGenerate = { navController.navigate(Screen.AiGenerateVocab.route) },
                        onNavigateToPacks = { navController.navigate(Screen.VocabPacks.route) }
                    )
                }

                composable(Screen.VocabPacks.route) {
                    VocabPacksScreen(
                        viewModel = vocabViewModel,
                        onBack = { navController.popBackStack() },
                        onFilterByPack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.SrsReview.route) {
                    SrsReviewScreen(
                        viewModel = reviewViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.AiTutor.route) {
                    AiTutorScreen(
                        viewModel = tutorViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.GrammarDetail.route,
                    arguments = listOf(navArgument("topicId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val topicId = backStackEntry.arguments?.getString("topicId") ?: ""
                    GrammarDetailScreen(
                        topicId = topicId,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ReadingDetail.route,
                    arguments = listOf(navArgument("passageId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val passageId = backStackEntry.arguments?.getString("passageId") ?: ""
                    ReadingDetailScreen(
                        passageId = passageId,
                        viewModel = learnViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.ListeningDetail.route,
                    arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
                    ListeningDetailScreen(
                        exerciseId = exerciseId,
                        viewModel = learnViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.WritingGrader.route) {
                    WritingGraderScreen(
                        viewModel = examsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.SpeakingPractice.route) {
                    SpeakingPracticeScreen(
                        viewModel = examsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.MistakeNotebook.route) {
                    MistakeNotebookScreen(
                        viewModel = profileViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.DiagnosticTest.route) {
                    DiagnosticTestScreen(
                        viewModel = examsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        profileViewModel = profileViewModel,
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
