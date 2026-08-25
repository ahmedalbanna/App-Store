package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.MarketplaceItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MarketplaceDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and getItemCount`() = runTest {
        val dao = db.marketplaceDao()
        assertEquals(0, dao.getItemCount())
        val item = MarketplaceItem(
            id = "test_app",
            titleAr = "اختبار",
            titleEn = "Test",
            type = "APP",
            category = "أدوات",
            sizeMb = 10.0,
            rating = 4.5,
            developerOrCreatorAr = "dev",
            descriptionAr = "desc"
        )
        dao.insertItem(item)
        assertEquals(1, dao.getItemCount())
        val all = dao.getAllItems().first()
        assertEquals(1, all.size)
        assertEquals("test_app", all[0].id)
    }

    @Test
    fun `insertAll with REPLACE overrides existing`() = runTest {
        val dao = db.marketplaceDao()
        val item1 = MarketplaceItem(id = "dup", titleAr = "اول", titleEn = "First", type = "APP", category = "أدوات", sizeMb = 1.0, rating = 4.0, developerOrCreatorAr = "dev", descriptionAr = "d")
        val item2 = MarketplaceItem(id = "dup", titleAr = "ثاني", titleEn = "Second", type = "APP", category = "أدوات", sizeMb = 1.0, rating = 5.0, developerOrCreatorAr = "dev", descriptionAr = "d2")
        dao.insertAll(listOf(item1))
        assertEquals(1, dao.getItemCount())
        dao.insertAll(listOf(item2))
        assertEquals(1, dao.getItemCount())
        val fetched = dao.getItemById("dup")
        assertEquals("ثاني", fetched?.titleAr)
        assertEquals(5.0, fetched?.rating ?: 0.0, 0.01)
    }

    @Test
    fun `getItemsByType filters correctly`() = runTest {
        val dao = db.marketplaceDao()
        dao.insertAll(
            listOf(
                MarketplaceItem(id = "a1", titleAr = "تطبيق", titleEn = "App", type = "APP", category = "أدوات", sizeMb = 1.0, rating = 4.0, developerOrCreatorAr = "dev", descriptionAr = "d"),
                MarketplaceItem(id = "g1", titleAr = "لعبة", titleEn = "Game", type = "GAME", category = "ألعاب", sizeMb = 1.0, rating = 4.0, developerOrCreatorAr = "dev", descriptionAr = "d")
            )
        )
        val apps = dao.getItemsByType("APP").first()
        assertEquals(1, apps.size)
        assertEquals("APP", apps[0].type)
    }

    @Test
    fun `updateDownloadState and bookmark work`() = runTest {
        val dao = db.marketplaceDao()
        val item = MarketplaceItem(id = "b1", titleAr = "كتاب", titleEn = "Book", type = "BOOK", category = "كتب", sizeMb = 5.0, rating = 4.9, developerOrCreatorAr = "dev", descriptionAr = "d")
        dao.insertItem(item)
        dao.updateDownloadState("b1", "DOWNLOADING", 50)
        var fetched = dao.getItemById("b1")
        assertEquals("DOWNLOADING", fetched?.downloadState)
        assertEquals(50, fetched?.downloadProgress)

        dao.updateIsBookmarked("b1", true)
        fetched = dao.getItemById("b1")
        assertTrue(fetched?.isBookmarked == true)

        dao.updateBookProgress("b1", 3)
        fetched = dao.getItemById("b1")
        assertEquals(3, fetched?.lastReadPage)
    }

    @Test
    fun `indices on type and downloadState do not break queries`() = runTest {
        val dao = db.marketplaceDao()
        dao.insertAll(
            (1..20).map {
                MarketplaceItem(
                    id = "item_$it",
                    titleAr = "عنصر $it",
                    titleEn = "Item $it",
                    type = if (it % 2 == 0) "APP" else "GAME",
                    category = "عام",
                    sizeMb = 1.0,
                    rating = 4.0,
                    developerOrCreatorAr = "dev",
                    descriptionAr = "desc",
                    downloadState = if (it % 3 == 0) "DOWNLOADING" else "NONE"
                )
            }
        )
        assertEquals(20, dao.getItemCount())
        val downloading = dao.getAllItems().first().filter { it.downloadState == "DOWNLOADING" }
        assertEquals(6, downloading.size)
    }
}
