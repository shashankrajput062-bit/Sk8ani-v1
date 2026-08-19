package com.example.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scene.ObjectType
import com.example.scene.SceneObject
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.*
import com.example.viewmodel.AnimForgeViewModel
import com.example.viewmodel.StudioUiState

@Composable
fun OutlinerPane(
    viewModel: AnimForgeViewModel,
    uiState: StudioUiState,
    modifier: Modifier = Modifier
) {
    var renamingObjectId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    NeoCard(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
        cornerRadius = 12.dp,
        elevation = 3.dp,
        backgroundColor = NeoSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = StudioCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Outliner",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeoTextPrimary,
                            fontSize = 13.sp
                        )
                    )
                }

                // Add Object shortcut
                NeoIconButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Add Object",
                    onClick = { viewModel.setShowAddMenu(true) },
                    size = 26.dp,
                    iconSize = 14.dp,
                    testTag = "outliner_add_btn"
                )
            }

            Divider(
                color = NeoBorder.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Object List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.objectList, key = { it.id }) { obj ->
                    val isSelected = (obj.id == uiState.selectedObjectId)

                    OutlinerItemRow(
                        obj = obj,
                        isSelected = isSelected,
                        onSelect = { viewModel.selectObject(obj.id) },
                        onToggleVisibility = { viewModel.toggleVisibility(obj.id) },
                        onToggleLock = { viewModel.toggleLock(obj.id) },
                        onStartRename = {
                            renamingObjectId = obj.id
                            renameText = obj.name
                        }
                    )
                }
            }

            // Bottom Actions (Duplicate, Delete)
            if (uiState.selectedObjectId != null) {
                Divider(
                    color = NeoBorder.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duplicate
                    NeoIconButton(
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Duplicate Selected",
                        onClick = { viewModel.duplicateSelected() },
                        size = 30.dp,
                        iconSize = 15.dp,
                        testTag = "outliner_duplicate_btn"
                    )

                    // Delete
                    NeoIconButton(
                        icon = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Selected",
                        onClick = { viewModel.deleteSelected() },
                        tint = StudioCrimson,
                        accentColor = StudioCrimson,
                        size = 30.dp,
                        iconSize = 15.dp,
                        testTag = "outliner_delete_btn"
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (renamingObjectId != null) {
        AlertDialog(
            onDismissRequest = { renamingObjectId = null },
            title = { Text("Rename Object", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameObject(renamingObjectId!!, renameText)
                        }
                        renamingObjectId = null
                    }
                ) {
                    Text("Save", color = StudioCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingObjectId = null }) {
                    Text("Cancel", color = NeoTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun OutlinerItemRow(
    obj: SceneObject,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    onStartRename: () -> Unit
) {
    val bg = if (isSelected) StudioCyan.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (isSelected) StudioCyan else NeoTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onSelect() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Icon + Name
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when (obj.type) {
                    ObjectType.MESH -> Icons.Default.Category
                    ObjectType.LIGHT -> Icons.Default.Lightbulb
                    ObjectType.CAMERA -> Icons.Default.Videocam
                    ObjectType.GROUP -> Icons.Default.Folder
                },
                contentDescription = null,
                tint = if (isSelected) StudioCyan else NeoTextSecondary,
                modifier = Modifier.size(15.dp)
            )

            Text(
                text = obj.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
        }

        // Toggles: Eye (Visibility) & Lock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Visibility
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (obj.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    tint = if (obj.isVisible) NeoTextSecondary else NeoTextTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Lock
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = if (obj.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    tint = if (obj.isLocked) StudioAmber else NeoTextTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
