package com.example.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.renderer.GizmoMode
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.*
import com.example.viewmodel.AnimForgeViewModel
import com.example.viewmodel.StudioUiState

@Composable
fun ViewportOverlayControls(
    viewModel: AnimForgeViewModel,
    uiState: StudioUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Left Floating Gizmo Tools Column
        NeoCard(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(44.dp),
            shape = RoundedCornerShape(10.dp),
            cornerRadius = 10.dp,
            elevation = 3.dp,
            backgroundColor = ViewportOverlayBg
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Select tool
                NeoIconButton(
                    icon = Icons.Default.NearMe,
                    contentDescription = "Select Object",
                    onClick = { viewModel.setGizmoMode(GizmoMode.NONE) },
                    isSelected = uiState.gizmoMode == GizmoMode.NONE,
                    size = 36.dp,
                    iconSize = 18.dp,
                    testTag = "btn_tool_select"
                )

                // Translate tool (Move)
                NeoIconButton(
                    icon = Icons.Default.OpenWith,
                    contentDescription = "Move Tool",
                    onClick = { viewModel.setGizmoMode(GizmoMode.TRANSLATE) },
                    isSelected = uiState.gizmoMode == GizmoMode.TRANSLATE,
                    size = 36.dp,
                    iconSize = 18.dp,
                    testTag = "btn_tool_move"
                )

                // Rotate tool
                NeoIconButton(
                    icon = Icons.Default.Sync,
                    contentDescription = "Rotate Tool",
                    onClick = { viewModel.setGizmoMode(GizmoMode.ROTATE) },
                    isSelected = uiState.gizmoMode == GizmoMode.ROTATE,
                    size = 36.dp,
                    iconSize = 18.dp,
                    testTag = "btn_tool_rotate"
                )

                // Scale tool
                NeoIconButton(
                    icon = Icons.Default.AspectRatio,
                    contentDescription = "Scale Tool",
                    onClick = { viewModel.setGizmoMode(GizmoMode.SCALE) },
                    isSelected = uiState.gizmoMode == GizmoMode.SCALE,
                    size = 36.dp,
                    iconSize = 18.dp,
                    testTag = "btn_tool_scale"
                )

                Divider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = NeoBorder.copy(alpha = 0.5f)
                )

                // Focus on Selection
                NeoIconButton(
                    icon = Icons.Default.FilterCenterFocus,
                    contentDescription = "Frame Selection",
                    onClick = { viewModel.focusSelection() },
                    size = 36.dp,
                    iconSize = 18.dp,
                    testTag = "btn_tool_focus"
                )
            }
        }

        // Top Center Status Chip
        uiState.statusMessage?.let { msg ->
            NeoCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .height(30.dp),
                shape = RoundedCornerShape(15.dp),
                cornerRadius = 15.dp,
                elevation = 2.dp,
                backgroundColor = NeoSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StudioCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NeoTextPrimary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        // Bottom Left Selection Name Tag
        if (uiState.selectedObject != null) {
            NeoCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .height(32.dp),
                shape = RoundedCornerShape(8.dp),
                cornerRadius = 8.dp,
                elevation = 2.dp,
                backgroundColor = ViewportOverlayBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (uiState.selectedObject.type) {
                            com.example.scene.ObjectType.MESH -> Icons.Default.Category
                            com.example.scene.ObjectType.LIGHT -> Icons.Default.Lightbulb
                            else -> Icons.Default.Videocam
                        },
                        contentDescription = null,
                        tint = StudioCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.selectedObject.name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeoTextPrimary,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}
