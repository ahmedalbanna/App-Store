package com.example.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "marketplace_items",
    indices = [
        Index(value = ["type"]),
        Index(value = ["downloadState"])
    ]
)
data class MarketplaceItem(
    @PrimaryKey val id: String,
    val titleAr: String,
    val titleEn: String,
    val type: String, // "APP", "GAME", "BOOK", "MOVIE", "MUSIC"
    val category: String, // e.g. "أدوات", "ديني", "تسلية", "تعليم", "مغامرات"
    val sizeMb: Double,
    val rating: Double,
    val developerOrCreatorAr: String,
    val descriptionAr: String,
    val isDownloaded: Boolean = false,
    val isInstalled: Boolean = false,
    val downloadProgress: Int = 0, // 0 to 100
    val downloadState: String = "NONE", // "NONE", "DOWNLOADING", "PAUSED", "DOWNLOADED", "INSTALLING", "INSTALLED", "UPDATE_AVAILABLE"
    val lastReadPage: Int = 0, // For Books: remembers page index
    val isBookmarked: Boolean = false,
    val simulatedVersionCode: Int = 1, // Current version code in store
    val installedVersionCode: Int = 0  // 0 means not installed, or can be changed to check updates
)
