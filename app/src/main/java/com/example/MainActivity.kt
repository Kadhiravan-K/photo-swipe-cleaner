package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.PhotoFlowMain
import com.example.ui.PhotoFlowViewModel
import com.example.ui.PhotoFlowViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PhotoFlowViewModel by viewModels {
        PhotoFlowViewModelFactory((application as PhotoFlowApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PhotoFlowMain(viewModel = viewModel)
            }
        }
    }
}
