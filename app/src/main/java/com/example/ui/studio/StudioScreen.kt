package com.example.ui.studio

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.engine.renderer.AnimForgeGLSurfaceView
import com.example.ui.theme.NeoBackground
import com.example.viewmodel.AnimForgeViewModel

@Composable
fun StudioScreen(
    viewModel: AnimForgeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Toggle between landscape and portrait orientations
    val toggleOrientation: () -> Unit = {
        val activity = context as? Activity
        if (activity != null) {
            val newOrientation = if (isLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            activity.requestedOrientation = newOrientation
        }
    }

    // Remember the GLSurfaceView instance
    val glSurfaceView = remember {
        AnimForgeGLSurfaceView(
            context = context,
            renderer = viewModel.renderer,
            onObjectSelected = { objId ->
                viewModel.selectObject(objId)
            },
            onTransformChanged = {
                viewModel.syncUiState()
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NeoBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. Fullscreen Real-time 3D Viewport
        AndroidView(
            factory = { glSurfaceView },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Viewport Floating HUD Controls
        ViewportOverlayControls(
            viewModel = viewModel,
            uiState = uiState,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Top App Bar
        StudioTopBar(
            viewModel = viewModel,
            uiState = uiState,
            isLandscape = isLandscape,
            onToggleOrientation = toggleOrientation,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 4. Left Collapsible Outliner Pane
        AnimatedVisibility(
            visible = uiState.isOutlinerOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    top = if (isLandscape) 54.dp else 56.dp,
                    bottom = if (isLandscape) 78.dp else 88.dp
                )
        ) {
            OutlinerPane(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        // 5. Right Collapsible Inspector Pane
        AnimatedVisibility(
            visible = uiState.isInspectorOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    top = if (isLandscape) 54.dp else 56.dp,
                    bottom = if (isLandscape) 78.dp else 88.dp
                )
        ) {
            InspectorPane(
                viewModel = viewModel,
                uiState = uiState
            )
        }

        // 6. Bottom Timeline Dock
        TimelineDock(
            viewModel = viewModel,
            uiState = uiState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 7. Modals & Dialogs
        StudioDialogs(
            viewModel = viewModel,
            uiState = uiState
        )
    }
}
