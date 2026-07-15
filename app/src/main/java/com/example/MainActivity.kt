package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    private var gameViewModel: GameViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val vm: GameViewModel = viewModel()
                gameViewModel = vm

                LaunchedEffect(intent) {
                    handleIntent(intent)
                }

                MainDashboard(viewModel = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("TRIGGER_LOCK_APP")?.let { appName ->
            gameViewModel?.simulateOpeningApp(appName)
            intent.removeExtra("TRIGGER_LOCK_APP")
        }
    }
}
