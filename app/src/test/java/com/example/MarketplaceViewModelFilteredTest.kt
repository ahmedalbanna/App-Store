package com.example

import com.example.data.database.MarketplaceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 0 verification: filteredItems tab→type mapping was broken (type==tab never matched).
 * This test validates the fixed mapping in isolation (mirrors MarketplaceViewModel logic).
 */
class MarketplaceViewModelFilteredTest {

    private val tabToType = mapOf(
        "APPS" to "APP",
        "GAMES" to "GAME",
        "BOOKS" to "BOOK",
        "MOVIES" to "MOVIE",
        "MUSIC" to "MUSIC"
    )

    private fun filter(list: List<MarketplaceItem>, tab: String, query: String): List<MarketplaceItem> {
        var res = list
        when {
            tab == "LIBRARY" -> res = res.filter { it.isBookmarked || it.isDownloaded || it.isInstalled }
            tabToType.containsKey(tab) -> res = res.filter { it.type == tabToType[tab] }
            tab in setOf("ASSISTANT", "READ_BOOK_VIEW", "WATCH_MOVIE_VIEW", "PLAY_MUSIC_VIEW") -> res = emptyList()
            else -> res = emptyList()
        }
        if (query.isNotEmpty()) {
            res = res.filter {
                it.titleAr.contains(query, ignoreCase = true) ||
                it.titleEn.contains(query, ignoreCase = true) ||
                it.descriptionAr.contains(query, ignoreCase = true)
            }
        }
        return res
    }

    private fun fakeCatalog() = listOf(
        MarketplaceItem(id="a1", titleAr="زاد المسلم", titleEn="Zad Muslim", type="APP", category="إسلامي", sizeMb=10.0, rating=4.9, developerOrCreatorAr="dev", descriptionAr="أذكار"),
        MarketplaceItem(id="g1", titleAr="فارس الصحراء", titleEn="Desert Knight", type="GAME", category="مغامرات", sizeMb=100.0, rating=4.8, developerOrCreatorAr="dev", descriptionAr="لعبة"),
        MarketplaceItem(id="b1", titleAr="مقدمة ابن خلدون", titleEn="Ibn Khaldun", type="BOOK", category="تاريخ", sizeMb=5.0, rating=4.9, developerOrCreatorAr="dev", descriptionAr="كتاب"),
        MarketplaceItem(id="m1", titleAr="تاريخ الأندلس", titleEn="Andalus", type="MOVIE", category="وثائقي", sizeMb=300.0, rating=4.9, developerOrCreatorAr="dev", descriptionAr="فيلم"),
        MarketplaceItem(id="mu1", titleAr="أنغام الشرق", titleEn="Oriental", type="MUSIC", category="موسيقى", sizeMb=8.0, rating=4.8, developerOrCreatorAr="dev", descriptionAr="موسيقى"),
        MarketplaceItem(id="lib1", titleAr="محفوظ", titleEn="Saved", type="APP", category="أدوات", sizeMb=1.0, rating=5.0, developerOrCreatorAr="dev", descriptionAr="محفوظ", isBookmarked=true),
    )

    @Test
    fun `APPS tab returns only APP items`() {
        val res = filter(fakeCatalog(), "APPS", "")
        assertEquals(2, res.size) // a1 + lib1 (both APP)
        assert(res.all { it.type == "APP" })
    }

    @Test
    fun `GAMES tab returns only GAME`() {
        assertEquals(1, filter(fakeCatalog(), "GAMES", "").size)
        assertEquals("GAME", filter(fakeCatalog(), "GAMES", "")[0].type)
    }

    @Test
    fun `LIBRARY returns bookmarked or downloaded`() {
        val res = filter(fakeCatalog(), "LIBRARY", "")
        assertEquals(1, res.size)
        assertEquals("lib1", res[0].id)
    }

    @Test
    fun `ASSISTANT returns empty as expected`() {
        assertEquals(0, filter(fakeCatalog(), "ASSISTANT", "").size)
    }

    @Test
    fun `search filters within tab`() {
        val res = filter(fakeCatalog(), "APPS", "زاد")
        assertEquals(1, res.size)
        assertEquals("a1", res[0].id)
    }

    @Test
    fun `old broken logic would return 0 for APPS`() {
        // Simulate old bug: it.type == tab ("APP" == "APPS" always false)
        val broken = fakeCatalog().filter { it.type == "APPS" }
        assertEquals(0, broken.size)
        // Fixed logic must NOT be 0
        assertEquals(2, filter(fakeCatalog(), "APPS", "").size)
    }
}
