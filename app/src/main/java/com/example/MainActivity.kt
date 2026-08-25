package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MarketplaceMainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val app = application as AlWahaApplication
                val viewModel: com.example.ui.viewmodel.MarketplaceViewModel = viewModel(
                    factory = com.example.ui.viewmodel.MarketplaceViewModel.provideFactory(app.container.marketplaceRepository)
                )
                MarketplaceMainScreen(viewModel = viewModel)
            }
        }
    }
}
