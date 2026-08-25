package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.*
import com.example.ui.viewmodel.MarketplaceViewModel

/**
 * Phase 1.6: Navigation-Compose graph (prepared for Phase 2 Hilt navigation).
 * Currently MarketplaceMainScreen uses AnimatedContent on ViewModel.currentTab string.
 * This NavHost demonstrates the typed route counterpart using MarketplaceTab/MarketplaceScreen.
 * MainActivity could switch to this host in Phase 2 by passing navController.
 */
@Composable
fun MarketplaceNavHost(
    navController: NavHostController,
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier
) {
    val currentTabRoute by viewModel.currentTab.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = MarketplaceTab.APPS.route,
        modifier = modifier
    ) {
        composable(MarketplaceTab.APPS.route) { DashboardGridScreen(viewModel) }
        composable(MarketplaceTab.GAMES.route) { DashboardGridScreen(viewModel) }
        composable(MarketplaceTab.BOOKS.route) { DashboardGridScreen(viewModel) }
        composable(MarketplaceTab.MOVIES.route) { DashboardGridScreen(viewModel) }
        composable(MarketplaceTab.MUSIC.route) { DashboardGridScreen(viewModel) }
        composable(MarketplaceTab.LIBRARY.route) { DashboardGridScreen(viewModel) }
        composable(MarketplaceTab.ASSISTANT.route) { AiAssistantScreen(viewModel) }
        composable(MarketplaceTab.READ_BOOK_VIEW.route) { BookReaderScreen(viewModel) }
        composable(MarketplaceTab.WATCH_MOVIE_VIEW.route) { MovieCinemaScreen(viewModel) }
        composable(MarketplaceTab.PLAY_MUSIC_VIEW.route) { MusicPlayerScreen(viewModel) }
    }
}
