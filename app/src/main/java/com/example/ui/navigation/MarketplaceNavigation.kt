package com.example.ui.navigation

import com.example.data.database.MarketplaceItem

/**
 * Phase 1.6: Typed navigation — replaces raw String tabs.
 * Keeps route strings for backwards compatibility with persisted state / deep links.
 */
enum class MarketplaceTab(val route: String, val itemType: String? = null) {
    APPS("APPS", "APP"),
    GAMES("GAMES", "GAME"),
    BOOKS("BOOKS", "BOOK"),
    MOVIES("MOVIES", "MOVIE"),
    MUSIC("MUSIC", "MUSIC"),
    LIBRARY("LIBRARY"),
    ASSISTANT("ASSISTANT"),
    READ_BOOK_VIEW("READ_BOOK_VIEW"),
    WATCH_MOVIE_VIEW("WATCH_MOVIE_VIEW"),
    PLAY_MUSIC_VIEW("PLAY_MUSIC_VIEW");

    companion object {
        fun fromRoute(route: String): MarketplaceTab =
            entries.find { it.route == route } ?: APPS
    }

    val isContentTab: Boolean get() = itemType != null
    val isLibrary: Boolean get() = this == LIBRARY
    val isInternalView: Boolean get() = this in setOf(ASSISTANT, READ_BOOK_VIEW, WATCH_MOVIE_VIEW, PLAY_MUSIC_VIEW)
}

sealed interface MarketplaceScreen {
    data class Dashboard(val tab: MarketplaceTab) : MarketplaceScreen {
        val route: String get() = tab.route
    }
    data object Library : MarketplaceScreen {
        const val route = "LIBRARY"
    }
    data object Assistant : MarketplaceScreen {
        const val route = "ASSISTANT"
    }
    data class BookReader(val itemId: String) : MarketplaceScreen {
        companion object { const val route = "READ_BOOK_VIEW" }
    }
    data class MoviePlayer(val itemId: String) : MarketplaceScreen {
        companion object { const val route = "WATCH_MOVIE_VIEW" }
    }
    data class MusicPlayer(val itemId: String) : MarketplaceScreen {
        companion object { const val route = "PLAY_MUSIC_VIEW" }
    }
    data class Details(val item: MarketplaceItem) : MarketplaceScreen
}
