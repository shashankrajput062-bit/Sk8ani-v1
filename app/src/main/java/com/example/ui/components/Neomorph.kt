package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Custom Neomorphic Box with dual soft shadows (white top-left highlight + dark slate bottom-right shadow)
 * Supports pressed/inset state, rounded corners, and elevated styling.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    cornerRadius: Dp = 12.dp,
    elevation: Dp = 4.dp,
    isInset: Boolean = false,
    backgroundColor: Color = NeoSurface,
    content: @Composable BoxScope.() -> Unit
) {
    val darkShadowColor = NeoShadowDark.copy(alpha = 0.65f)
    val lightShadowColor = NeoShadowLight.copy(alpha = 0.95f)

    Box(
        modifier = modifier
            .drawBehind {
                val radiusPx = cornerRadius.toPx()
                val offsetDist = if (isInset) 2.dp.toPx() else elevation.toPx()

                if (!isInset) {
                    // Elevated dual shadow:
                    // Bottom-right dark shadow
                    drawRoundRect(
                        color = darkShadowColor,
                        topLeft = Offset(offsetDist, offsetDist),
                        size = size,
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                    // Top-left light shadow
                    drawRoundRect(
                        color = lightShadowColor,
                        topLeft = Offset(-offsetDist, -offsetDist),
                        size = size,
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                } else {
                    // Inset appearance
                    drawRoundRect(
                        color = darkShadowColor.copy(alpha = 0.4f),
                        topLeft = Offset(0f, 0f),
                        size = size,
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                }
            }
            .clip(shape)
            .background(if (isInset) NeoSurfaceInset else backgroundColor)
            .border(
                width = 1.dp,
                color = if (isInset) NeoBorder.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun NeoButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    accentColor: Color = StudioCyan,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(10.dp),
    cornerRadius: Dp = 10.dp,
    elevation: Dp = 3.dp,
    testTag: String = "neo_button",
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isStateInset = isPressed || isSelected

    val currentBg = if (isSelected) {
        accentColor.copy(alpha = 0.15f)
    } else if (isStateInset) {
        NeoSurfaceInset
    } else {
        NeoSurface
    }

    val borderColor = if (isSelected) accentColor.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)

    NeoCard(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        cornerRadius = cornerRadius,
        elevation = if (isStateInset) 0.dp else elevation,
        isInset = isStateInset,
        backgroundColor = currentBg
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
fun NeoIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    tint: Color = if (isSelected) StudioCyan else NeoTextPrimary,
    accentColor: Color = StudioCyan,
    size: Dp = 38.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    cornerRadius: Dp = 8.dp,
    testTag: String = "neo_icon_btn"
) {
    NeoButton(
        onClick = onClick,
        modifier = modifier.size(size),
        isSelected = isSelected,
        enabled = enabled,
        accentColor = accentColor,
        shape = shape,
        cornerRadius = cornerRadius,
        testTag = testTag
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = StudioCyan
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
fun NeoSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = StudioCyan
) {
    NeoCard(
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        cornerRadius = 8.dp,
        isInset = true,
        backgroundColor = NeoSurfaceInset
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) NeoSurface else Color.Transparent
                        )
                        .clickable { onItemSelected(index) }
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) accentColor else NeoTextSecondary,
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PrecisionNumberScrubber(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 0.1f,
    axisColor: Color = StudioCyan,
    min: Float = -1000f,
    max: Float = 1000f
) {
    NeoCard(
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(6.dp),
        cornerRadius = 6.dp,
        elevation = 2.dp,
        backgroundColor = NeoSurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Axis identifier pill
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(axisColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = axisColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }

            // Decrement
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onValueChange((value - step).coerceIn(min, max)) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeoTextSecondary
                    )
                )
            }

            // Value text
            Text(
                text = String.format(java.util.Locale.US, "%.2f", value),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = NeoTextPrimary,
                    fontSize = 12.sp
                )
            )

            // Increment
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onValueChange((value + step).coerceIn(min, max)) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NeoTextSecondary
                    )
                )
            }
        }
    }
}
