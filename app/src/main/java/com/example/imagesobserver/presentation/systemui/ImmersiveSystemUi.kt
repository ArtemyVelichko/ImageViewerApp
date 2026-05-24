package com.example.imagesobserver.presentation.systemui

import android.app.Activity
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides or shows status bar, navigation bar, and fits content edge-to-edge when immersive.
 */
@Composable
fun ImmersiveSystemUi(systemBarsVisible: Boolean) {
    val view = LocalView.current

    SideEffect {
        if (view.isInEditMode) return@SideEffect
        applySystemBarsVisibility(view, systemBarsVisible)
    }

    DisposableEffect(Unit) {
        onDispose {
            if (view.isInEditMode) return@onDispose
            applySystemBarsVisibility(view, visible = true)
        }
    }
}

private fun applySystemBarsVisibility(view: android.view.View, visible: Boolean) {
    val window = (view.context as Activity).window
    val controller = WindowCompat.getInsetsController(window, view)

    if (visible) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        controller.show(WindowInsetsCompat.Type.systemBars())
    } else {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
