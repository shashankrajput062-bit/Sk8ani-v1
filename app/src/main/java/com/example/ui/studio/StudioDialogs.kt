package com.example.ui.studio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.engine.mesh.PrimitiveType
import com.example.scene.LightType
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.*
import com.example.viewmodel.AnimForgeViewModel
import com.example.viewmodel.StudioUiState

@Composable
fun StudioDialogs(
    viewModel: AnimForgeViewModel,
    uiState: StudioUiState
) {
    if (uiState.showAddMenu) {
        AddObjectDialog(
            onDismiss = { viewModel.setShowAddMenu(false) },
            onAddPrimitive = { viewModel.addPrimitive(it) },
            onAddLight = { viewModel.addLight(it) }
        )
    }

    if (uiState.showTemplateDialog) {
        TemplatePickerDialog(
            onDismiss = { viewModel.setShowTemplateDialog(false) },
            onSelectTemplate = { viewModel.loadTemplate(it) }
        )
    }

    if (uiState.showExportDialog) {
        ExportImportDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowExportDialog(false) }
        )
    }
}

@Composable
fun AddObjectDialog(
    onDismiss: () -> Unit,
    onAddPrimitive: (PrimitiveType) -> Unit,
    onAddLight: (LightType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier
                .width(380.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            cornerRadius = 16.dp,
            elevation = 6.dp,
            backgroundColor = NeoSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add to 3D Scene",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeoTextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeoTextSecondary)
                    }
                }

                Text("3D MESH PRIMITIVES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StudioCyan))
                
                val primitives = listOf(
                    PrimitiveType.CUBE to "Cube",
                    PrimitiveType.SPHERE to "Sphere",
                    PrimitiveType.CYLINDER to "Cylinder",
                    PrimitiveType.CONE to "Cone",
                    PrimitiveType.TORUS to "Torus",
                    PrimitiveType.PLANE to "Plane",
                    PrimitiveType.CAPSULE to "Capsule"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    primitives.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { (prim, name) ->
                                NeoButton(
                                    onClick = { onAddPrimitive(prim) },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    testTag = "btn_add_${name.lowercase()}"
                                ) {
                                    Text(name, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("LIGHTS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StudioAmber))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeoButton(
                        onClick = { onAddLight(LightType.DIRECTIONAL) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        accentColor = StudioAmber,
                        shape = RoundedCornerShape(8.dp),
                        testTag = "btn_add_sun"
                    ) {
                        Text("Sun Light", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = StudioAmber, fontSize = 11.sp))
                    }
                    NeoButton(
                        onClick = { onAddLight(LightType.POINT) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        accentColor = StudioAmber,
                        shape = RoundedCornerShape(8.dp),
                        testTag = "btn_add_point_light"
                    ) {
                        Text("Point Light", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = StudioAmber, fontSize = 11.sp))
                    }
                }
            }
        }
    }
}

@Composable
fun TemplatePickerDialog(
    onDismiss: () -> Unit,
    onSelectTemplate: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            cornerRadius = 16.dp,
            elevation = 6.dp,
            backgroundColor = NeoSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preset Scenes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NeoTextPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeoTextSecondary)
                    }
                }

                val templates = listOf(
                    "Kinetic Orbit" to "Animated glowing rings and orbiting spheres with multi-track keyframe curves.",
                    "Skater Scene" to "Skateboard deck, trucks, 4 wheels with metallic materials and physics floor.",
                    "Default Cube" to "Clean studio lighting, floor grid, and single editable cube."
                )

                templates.forEach { (title, desc) ->
                    NeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTemplate(title) },
                        shape = RoundedCornerShape(10.dp),
                        cornerRadius = 10.dp,
                        elevation = 2.dp,
                        backgroundColor = NeoSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = StudioCyan))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall.copy(color = NeoTextSecondary, fontSize = 11.sp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportImportDialog(
    viewModel: AnimForgeViewModel,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    var exportedText by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(activeTab) {
        if (activeTab == 0) {
            exportedText = viewModel.exportObj()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NeoCard(
            modifier = Modifier
                .width(420.dp)
                .height(340.dp),
            shape = RoundedCornerShape(16.dp),
            cornerRadius = 16.dp,
            elevation = 6.dp,
            backgroundColor = NeoSurfaceElevated
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3D Asset Manager", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeoTextSecondary)
                    }
                }

                // Tab Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeoButton(
                        onClick = { activeTab = 0 },
                        isSelected = activeTab == 0,
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Export Wavefront (.OBJ)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    NeoButton(
                        onClick = { activeTab = 1 },
                        isSelected = activeTab == 1,
                        modifier = Modifier.weight(1f).height(32.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Import (.OBJ)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                if (activeTab == 0) {
                    // Export preview
                    NeoCard(
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        shape = RoundedCornerShape(8.dp),
                        cornerRadius = 8.dp,
                        isInset = true,
                        backgroundColor = NeoSurfaceInset
                    ) {
                        Text(
                            text = exportedText.take(1500),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = NeoTextSecondary),
                            modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        NeoButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(exportedText))
                                Toast.makeText(context, "OBJ copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            accentColor = StudioCyan,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy to Clipboard", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = StudioCyan))
                        }
                    }
                } else {
                    // Import textfield
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Paste Wavefront OBJ syntax") },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        NeoButton(
                            onClick = {
                                if (importText.isNotBlank()) {
                                    viewModel.importObjString(importText)
                                    onDismiss()
                                }
                            },
                            accentColor = StudioEmerald,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import OBJ Mesh", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = StudioEmerald))
                        }
                    }
                }
            }
        }
    }
}
