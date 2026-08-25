package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.database.MarketplaceItem
import com.example.ui.DashboardGridScreen
import com.example.ui.navigation.MarketplaceTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MarketplaceViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 1.7: Roborazzi screenshot for DashboardGridScreen (previously only Greeting was screenshotted).
 * Uses a lightweight fake ViewModel state via a real ViewModel with a fake repo could also be used;
 * here we directly render DashboardGridScreen with a fake ViewModel backed by in-memory data
 * using a manual CompositionLocal approach: we create a fake VM that emits a small catalog.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class DashboardScreenshotTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun dashboard_screenshot_apps_tab() {
        // Use the real MarketplaceViewModelHiltTest.FakeMarketplaceRepository to seed data
        val fakeRepo = MarketplaceViewModelHiltTest.FakeMarketplaceRepository().apply {
            emit(
                listOf(
                    MarketplaceItem(id = "app_zad", titleAr = "زاد المسلم", titleEn = "Zad Muslim", type = "APP", category = "إسلامي", sizeMb = 24.5, rating = 4.9, developerOrCreatorAr = "مطور", descriptionAr = "حقيبة المسلم"),
                    MarketplaceItem(id = "game_1", titleAr = "فارس الصحراء", titleEn = "Desert Knight", type = "GAME", category = "ألعاب", sizeMb = 120.0, rating = 4.8, developerOrCreatorAr = "ستوديو", descriptionAr = "مغامرة")
                )
            )
        }
        val viewModel = MarketplaceViewModel(fakeRepo)

        composeRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardGridScreen(viewModel = viewModel)
                }
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard.png")
    }
}
