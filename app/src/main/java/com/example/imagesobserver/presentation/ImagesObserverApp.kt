package com.example.imagesobserver.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.imagesobserver.presentation.navigation.AppNavHost
import com.example.imagesobserver.presentation.theme.ImagesObserverTheme

@Composable
fun ImagesObserverApp(modifier: Modifier = Modifier) {
    ImagesObserverTheme {
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            AppNavHost(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
