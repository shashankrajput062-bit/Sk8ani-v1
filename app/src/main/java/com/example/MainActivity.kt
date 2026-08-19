package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.studio.StudioScreen
import com.example.ui.theme.AnimForgeTheme
import com.example.ui.theme.NeoBackground
import com.example.viewmodel.AnimForgeViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AnimForgeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NeoBackground
                ) {
                    StudioScreen(viewModel = viewModel)
                }
            }
        }
    }
}

