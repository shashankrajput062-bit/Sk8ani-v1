package com.example.ui.studio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.InterpolationType
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.*
import com.example.viewmodel.AnimForgeViewModel
import com.example.viewmodel.StudioUiState

@Composable
fun TimelineDock(
    viewModel: AnimForgeViewModel,
    uiState: StudioUiState,
    modifier: Modifier = Modifier
) {
    NeoCard(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        cornerRadius = 14.dp,
        elevation = 4.dp,
        backgroundColor = NeoSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Transport Buttons & Frame Info & Keyframe Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Transport Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // First frame
                    NeoIconButton(
                        icon = Icons.Default.SkipPrevious,
                        contentDescription = "First Frame",
                        onClick = { viewModel.setTimelineFrame(uiState.startFrame) },
                        size = 30.dp,
                        iconSize = 16.dp,
                        testTag = "btn_first_frame"
                    )

                    // Step back
                    NeoIconButton(
                        icon = Icons.Default.ArrowLeft,
                        contentDescription = "Step Back",
                        onClick = { viewModel.setTimelineFrame(uiState.currentFrame - 1) },
                        size = 30.dp,
                        iconSize = 18.dp,
                        testTag = "btn_step_back"
                    )

                    // Play / Pause
                    NeoIconButton(
                        icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        onClick = { viewModel.togglePlayback() },
                        isSelected = uiState.isPlaying,
                        accentColor = StudioCyan,
                        tint = if (uiState.isPlaying) StudioCyan else NeoTextPrimary,
                        size = 34.dp,
                        iconSize = 20.dp,
                        testTag = "btn_play_pause"
                    )

                    // Step forward
                    NeoIconButton(
                        icon = Icons.Default.ArrowRight,
                        contentDescription = "Step Forward",
                        onClick = { viewModel.setTimelineFrame(uiState.currentFrame + 1) },
                        size = 30.dp,
                        iconSize = 18.dp,
                        testTag = "btn_step_forward"
                    )

                    // Last frame
                    NeoIconButton(
                        icon = Icons.Default.SkipNext,
                        contentDescription = "Last Frame",
                        onClick = { viewModel.setTimelineFrame(uiState.endFrame) },
                        size = 30.dp,
                        iconSize = 16.dp,
                        testTag = "btn_last_frame"
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Frame & Time Readout Pill
                    val seconds = uiState.currentFrame.toFloat() / uiState.fps.toFloat()
                    NeoCard(
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(6.dp),
                        cornerRadius = 6.dp,
                        isInset = true,
                        backgroundColor = NeoSurfaceInset
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "F: ${uiState.currentFrame}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StudioCyan,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(java.util.Locale.US, "%.2fs", seconds),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = NeoTextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // Right: Auto-Key, Insert Keyframe, Remove Keyframe, FPS Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Auto-Key recording toggle
                    NeoIconButton(
                        icon = Icons.Default.FiberManualRecord,
                        contentDescription = "Auto-Key",
                        onClick = { viewModel.toggleAutoKey() },
                        isSelected = uiState.isAutoKey,
                        accentColor = StudioCrimson,
                        tint = if (uiState.isAutoKey) StudioCrimson else NeoTextSecondary,
                        size = 30.dp,
                        iconSize = 16.dp,
                        testTag = "btn_auto_key"
                    )

                    // Add Keyframe
                    NeoButton(
                        onClick = { viewModel.insertKeyframeForSelected(InterpolationType.EASE_IN_OUT) },
                        enabled = uiState.selectedObjectId != null,
                        accentColor = StudioAmber,
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(6.dp),
                        testTag = "btn_add_keyframe"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "+Key",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = StudioAmber,
                                fontSize = 11.sp
                            )
                        )
                    }

                    // Delete Keyframe
                    val hasKeyAtCurrent = uiState.keyframedFrames.contains(uiState.currentFrame)
                    NeoIconButton(
                        icon = Icons.Default.LayersClear,
                        contentDescription = "Remove Keyframe",
                        onClick = { viewModel.deleteKeyframeAtCurrent() },
                        enabled = hasKeyAtCurrent,
                        tint = if (hasKeyAtCurrent) StudioCrimson else NeoTextTertiary,
                        size = 30.dp,
                        iconSize = 15.dp,
                        testTag = "btn_del_keyframe"
                    )

                    // FPS indicator
                    NeoBadge(
                        text = "${uiState.fps} FPS",
                        color = NeoTextSecondary
                    )
                }
            }

            // Bottom: Interactive Timeline Track with Scrubber & Keyframe Diamonds
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                shape = RoundedCornerShape(6.dp),
                cornerRadius = 6.dp,
                isInset = true,
                backgroundColor = NeoSurfaceInset
            ) {
                val totalFrames = (uiState.endFrame - uiState.startFrame).coerceAtLeast(1)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalFrames) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                val targetFrame = (uiState.startFrame + fraction * totalFrames).toInt()
                                viewModel.setTimelineFrame(targetFrame)
                            }
                        }
                        .pointerInput(totalFrames) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                val targetFrame = (uiState.startFrame + fraction * totalFrames).toInt()
                                viewModel.setTimelineFrame(targetFrame)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Frame Tick Marks & numbers
                    val majorStep = 10
                    for (f in uiState.startFrame..uiState.endFrame) {
                        val fraction = (f - uiState.startFrame).toFloat() / totalFrames.toFloat()
                        val x = fraction * w

                        if (f % majorStep == 0) {
                            // Major tick
                            drawLine(
                                color = Color(0xFF94A3B8),
                                start = Offset(x, 0f),
                                end = Offset(x, 10f),
                                strokeWidth = 1.5f
                            )
                        } else if (f % 5 == 0) {
                            // Minor tick
                            drawLine(
                                color = Color(0xFFCBD5E1),
                                start = Offset(x, 0f),
                                end = Offset(x, 6f),
                                strokeWidth = 1f
                            )
                        }
                    }

                    // 2. Draw Keyframe Diamonds for selected object
                    for (kfFrame in uiState.keyframedFrames) {
                        if (kfFrame in uiState.startFrame..uiState.endFrame) {
                            val kfFraction = (kfFrame - uiState.startFrame).toFloat() / totalFrames.toFloat()
                            val kfX = kfFraction * w
                            val kfY = h * 0.5f

                            val diamondPath = Path().apply {
                                moveTo(kfX, kfY - 6f)
                                lineTo(kfX + 5f, kfY)
                                lineTo(kfX, kfY + 6f)
                                lineTo(kfX - 5f, kfY)
                                close()
                            }
                            drawPath(diamondPath, color = Color(0xFFFF9100))
                        }
                    }

                    // 3. Draw Playhead Scrubber Line & Marker
                    val playheadFraction = (uiState.currentFrame - uiState.startFrame).toFloat() / totalFrames.toFloat()
                    val playheadX = (playheadFraction * w).coerceIn(0f, w)

                    // Scrubber line
                    drawLine(
                        color = Color(0xFF0091EA),
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2.5f
                    )

                    // Scrubber Head
                    val headPath = Path().apply {
                        moveTo(playheadX - 6f, 0f)
                        lineTo(playheadX + 6f, 0f)
                        lineTo(playheadX + 6f, 7f)
                        lineTo(playheadX, 13f)
                        lineTo(playheadX - 6f, 7f)
                        close()
                    }
                    drawPath(headPath, color = Color(0xFF0091EA))
                }
            }
        }
    }
}
