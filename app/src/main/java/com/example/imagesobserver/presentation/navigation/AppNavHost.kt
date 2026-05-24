package com.example.imagesobserver.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.imagesobserver.presentation.images.ImageDetailScreen
import com.example.imagesobserver.presentation.images.ImagesScreen
import com.example.imagesobserver.presentation.images.ImagesViewModel

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Images,
        modifier = modifier,
    ) {
        composable<Images> {
            val imagesViewModel: ImagesViewModel = hiltViewModel()
            ImagesScreen(
                onOpenImageDetail = { startIndex ->
                    navController.navigate(ImageDetail(startIndex = startIndex))
                },
                viewModel = imagesViewModel,
            )
        }
        composable<ImageDetail> { backStackEntry ->
            val detail = backStackEntry.toRoute<ImageDetail>()
            ImageDetailScreen(
                startIndex = detail.startIndex,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
