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
import com.example.engine.camera.CameraPreset
import com.example.engine.renderer.TransformSpace
import com.example.engine.renderer.ViewportShading
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.*
import com.example.viewmodel.AnimForgeViewModel
import com.example.viewmodel.StudioUiState

@Composable
fun StudioTopBar(
    viewModel: AnimForgeViewModel,
    uiState: StudioUiState,
    isLandscape: Boolean = false,
    onToggleOrientation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    NeoCard(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(0.dp),
        cornerRadius = 0.dp,
        elevation = 3.dp,
        backgroundColor = NeoSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: App Logo, Project Name, Template Loader, Add Object, Landscape Mode Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Logo & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(StudioCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3D",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Text(
                        text = "Sk8ani",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeoTextPrimary,
                            fontSize = 15.sp
                        )
                    )

                    NeoBadge(
                        text = "STUDIO",
                        color = StudioCyan
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Landscape / Portrait Mode Button
                NeoButton(
                    onClick = onToggleOrientation,
                    accentColor = if (isLandscape) StudioCyan else StudioAmber,
                    isSelected = isLandscape,
                    modifier = Modifier.height(34.dp),
                    testTag = "btn_landscape_mode"
                ) {
                    Icon(
                        imageVector = if (isLandscape) Icons.Default.ScreenRotation else Icons.Default.ScreenLockLandscape,
                        contentDescription = "Toggle Landscape Mode",
                        tint = if (isLandscape) StudioCyan else StudioAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLandscape) "Landscape" else "Landscape",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isLandscape) StudioCyan else StudioAmber,
                            fontSize = 11.sp
                        )
                    )
                }

                // Outliner Toggle
                NeoIconButton(
                    icon = if (uiState.isOutlinerOpen) Icons.Default.ViewSidebar else Icons.Outlined.ViewSidebar,
                    contentDescription = "Toggle Outliner",
                    onClick = { viewModel.toggleOutliner() },
                    isSelected = uiState.isOutlinerOpen,
                    size = 34.dp,
                    iconSize = 18.dp,
                    testTag = "btn_toggle_outliner"
                )

                // Add Object Button
                NeoButton(
                    onClick = { viewModel.setShowAddMenu(true) },
                    accentColor = StudioCyan,
                    modifier = Modifier.height(34.dp),
                    testTag = "btn_add_object"
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add 3D Object",
                        tint = StudioCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = StudioCyan,
                            fontSize = 12.sp
                        )
                    )
                }

                // Templates Button
                NeoButton(
                    onClick = { viewModel.setShowTemplateDialog(true) },
                    modifier = Modifier.height(34.dp),
                    testTag = "btn_templates"
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Templates",
                        tint = StudioAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Scenes",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = NeoTextPrimary,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            // Center: Shading Modes & Camera Presets
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Shading Mode Pills
                NeoIconButton(
                    icon = Icons.Default.Circle,
                    contentDescription = "PBR Rendered",
                    onClick = { viewModel.setShadingMode(ViewportShading.RENDERED_PBR) },
                    isSelected = uiState.shadingMode == ViewportShading.RENDERED_PBR,
                    tint = if (uiState.shadingMode == ViewportShading.RENDERED_PBR) StudioCyan else NeoTextSecondary,
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_shade_pbr"
                )
                NeoIconButton(
                    icon = Icons.Outlined.Circle,
                    contentDescription = "Solid Shading",
                    onClick = { viewModel.setShadingMode(ViewportShading.SOLID) },
                    isSelected = uiState.shadingMode == ViewportShading.SOLID,
                    tint = if (uiState.shadingMode == ViewportShading.SOLID) StudioCyan else NeoTextSecondary,
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_shade_solid"
                )
                NeoIconButton(
                    icon = Icons.Default.GridOn,
                    contentDescription = "Wireframe",
                    onClick = { viewModel.setShadingMode(ViewportShading.WIREFRAME) },
                    isSelected = uiState.shadingMode == ViewportShading.WIREFRAME,
                    tint = if (uiState.shadingMode == ViewportShading.WIREFRAME) StudioCyan else NeoTextSecondary,
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_shade_wire"
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Transform Space Toggle (World / Local)
                NeoButton(
                    onClick = {
                        val next = if (uiState.transformSpace == TransformSpace.WORLD) TransformSpace.LOCAL else TransformSpace.WORLD
                        viewModel.setTransformSpace(next)
                    },
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(6.dp),
                    testTag = "btn_transform_space"
                ) {
                    Text(
                        text = if (uiState.transformSpace == TransformSpace.WORLD) "World" else "Local",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = StudioCyan,
                            fontSize = 11.sp
                        )
                    )
                }

                // Camera Presets
                NeoIconButton(
                    icon = Icons.Default.CenterFocusStrong,
                    contentDescription = "Perspective View",
                    onClick = { viewModel.setCameraPreset(CameraPreset.PERSPECTIVE) },
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_cam_persp"
                )
                NeoIconButton(
                    icon = Icons.Default.VerticalAlignTop,
                    contentDescription = "Top View",
                    onClick = { viewModel.setCameraPreset(CameraPreset.TOP) },
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_cam_top"
                )
                NeoIconButton(
                    icon = Icons.Default.CropPortrait,
                    contentDescription = "Front View",
                    onClick = { viewModel.setCameraPreset(CameraPreset.FRONT) },
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_cam_front"
                )
            }

            // Right: Undo/Redo, Physics, Save, Export, Inspector Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Undo / Redo
                NeoIconButton(
                    icon = Icons.Default.Undo,
                    contentDescription = "Undo",
                    onClick = { viewModel.undo() },
                    tint = if (uiState.canUndo) NeoTextPrimary else NeoTextTertiary,
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_undo"
                )
                NeoIconButton(
                    icon = Icons.Default.Redo,
                    contentDescription = "Redo",
                    onClick = { viewModel.redo() },
                    tint = if (uiState.canRedo) NeoTextPrimary else NeoTextTertiary,
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_redo"
                )

                // Physics Simulation Toggle
                NeoIconButton(
                    icon = if (uiState.isPhysicsSimulating) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                    contentDescription = "Physics Simulation",
                    onClick = { viewModel.togglePhysicsSimulation() },
                    isSelected = uiState.isPhysicsSimulating,
                    tint = if (uiState.isPhysicsSimulating) StudioCrimson else StudioEmerald,
                    accentColor = if (uiState.isPhysicsSimulating) StudioCrimson else StudioEmerald,
                    size = 34.dp,
                    iconSize = 18.dp,
                    testTag = "btn_physics_sim"
                )

                // Save
                NeoIconButton(
                    icon = Icons.Default.Save,
                    contentDescription = "Save Project",
                    onClick = { viewModel.saveProject() },
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_save"
                )

                // Export
                NeoIconButton(
                    icon = Icons.Default.FileDownload,
                    contentDescription = "Export Scene",
                    onClick = { viewModel.setShowExportDialog(true) },
                    size = 32.dp,
                    iconSize = 16.dp,
                    testTag = "btn_export"
                )

                // Inspector Toggle
                NeoIconButton(
                    icon = if (uiState.isInspectorOpen) Icons.Default.Tune else Icons.Outlined.Tune,
                    contentDescription = "Toggle Inspector",
                    onClick = { viewModel.toggleInspector() },
                    isSelected = uiState.isInspectorOpen,
                    size = 34.dp,
                    iconSize = 18.dp,
                    testTag = "btn_toggle_inspector"
                )
            }
        }
    }
}
