package com.example.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.animation.InterpolationType
import com.example.scene.LightType
import com.example.scene.ObjectType
import com.example.scene.RenderPreset
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AnimForgeViewModel
import com.example.viewmodel.InspectorTab
import com.example.viewmodel.StudioUiState

@Composable
fun InspectorPane(
    viewModel: AnimForgeViewModel,
    uiState: StudioUiState,
    modifier: Modifier = Modifier
) {
    val selectedObj = uiState.selectedObject

    NeoCard(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
        cornerRadius = 12.dp,
        elevation = 3.dp,
        backgroundColor = NeoSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header Tabs Selector
            val tabs = listOf(
                InspectorTab.TRANSFORM to "Trans",
                InspectorTab.MATERIAL to "Mat",
                InspectorTab.LIGHT to "Light",
                InspectorTab.PHYSICS to "Phys",
                InspectorTab.ANIMATION to "Anim",
                InspectorTab.RENDER to "Rndr"
            )

            val tabNames = tabs.map { it.second }
            val currentIdx = tabs.indexOfFirst { it.first == uiState.activeInspectorTab }.coerceAtLeast(0)

            NeoSegmentedControl(
                items = tabNames,
                selectedIndex = currentIdx,
                onItemSelected = { idx ->
                    viewModel.setInspectorTab(tabs[idx].first)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (uiState.activeInspectorTab) {
                    InspectorTab.TRANSFORM -> TransformTabContent(viewModel, selectedObj)
                    InspectorTab.MATERIAL -> MaterialTabContent(viewModel, selectedObj)
                    InspectorTab.LIGHT -> LightTabContent(viewModel, selectedObj)
                    InspectorTab.PHYSICS -> PhysicsTabContent(viewModel, selectedObj)
                    InspectorTab.ANIMATION -> AnimationTabContent(viewModel, selectedObj, uiState)
                    InspectorTab.RENDER -> RenderTabContent(viewModel)
                }
            }
        }
    }
}

@Composable
private fun TransformTabContent(viewModel: AnimForgeViewModel, obj: com.example.scene.SceneObject?) {
    if (obj == null) {
        EmptyInspectorPlaceholder("No object selected")
        return
    }

    // Section Header
    SectionTitle("Location")
    PrecisionNumberScrubber(
        label = "X",
        value = obj.transform.position.x,
        onValueChange = { viewModel.updatePosition(it, obj.transform.position.y, obj.transform.position.z) },
        axisColor = AxisRed
    )
    PrecisionNumberScrubber(
        label = "Y",
        value = obj.transform.position.y,
        onValueChange = { viewModel.updatePosition(obj.transform.position.x, it, obj.transform.position.z) },
        axisColor = AxisGreen
    )
    PrecisionNumberScrubber(
        label = "Z",
        value = obj.transform.position.z,
        onValueChange = { viewModel.updatePosition(obj.transform.position.x, obj.transform.position.y, it) },
        axisColor = AxisBlue
    )

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle("Rotation (deg)")
    PrecisionNumberScrubber(
        label = "X",
        value = obj.transform.rotation.x,
        onValueChange = { viewModel.updateRotation(it, obj.transform.rotation.y, obj.transform.rotation.z) },
        step = 5f,
        axisColor = AxisRed
    )
    PrecisionNumberScrubber(
        label = "Y",
        value = obj.transform.rotation.y,
        onValueChange = { viewModel.updateRotation(obj.transform.rotation.x, it, obj.transform.rotation.z) },
        step = 5f,
        axisColor = AxisGreen
    )
    PrecisionNumberScrubber(
        label = "Z",
        value = obj.transform.rotation.z,
        onValueChange = { viewModel.updateRotation(obj.transform.rotation.x, obj.transform.rotation.y, it) },
        step = 5f,
        axisColor = AxisBlue
    )

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle("Scale")
    PrecisionNumberScrubber(
        label = "X",
        value = obj.transform.scale.x,
        onValueChange = { viewModel.updateScale(it, obj.transform.scale.y, obj.transform.scale.z) },
        min = 0.01f,
        axisColor = AxisRed
    )
    PrecisionNumberScrubber(
        label = "Y",
        value = obj.transform.scale.y,
        onValueChange = { viewModel.updateScale(obj.transform.scale.x, it, obj.transform.scale.z) },
        min = 0.01f,
        axisColor = AxisGreen
    )
    PrecisionNumberScrubber(
        label = "Z",
        value = obj.transform.scale.z,
        onValueChange = { viewModel.updateScale(obj.transform.scale.x, obj.transform.scale.y, it) },
        min = 0.01f,
        axisColor = AxisBlue
    )

    Spacer(modifier = Modifier.height(4.dp))
    NeoButton(
        onClick = { viewModel.resetTransform() },
        modifier = Modifier.fillMaxWidth().height(32.dp),
        shape = RoundedCornerShape(6.dp),
        testTag = "btn_reset_transform"
    ) {
        Text(
            text = "Reset Transforms",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = NeoTextSecondary)
        )
    }
}

@Composable
private fun MaterialTabContent(viewModel: AnimForgeViewModel, obj: com.example.scene.SceneObject?) {
    if (obj == null || obj.type != ObjectType.MESH) {
        EmptyInspectorPlaceholder("Select a 3D Mesh to edit Material")
        return
    }

    SectionTitle("Base Color")
    val colorPalettes = listOf(
        Color(0xFF0091EA) to "Cyan",
        Color(0xFFFF6D00) to "Amber",
        Color(0xFF00C853) to "Emerald",
        Color(0xFFD50000) to "Crimson",
        Color(0xFF651FFF) to "Purple",
        Color(0xFFF0F4F8) to "White",
        Color(0xFF1E293B) to "Slate",
        Color(0xFFFFD600) to "Gold"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        colorPalettes.forEach { (color, _) ->
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable {
                        viewModel.updateMaterialColor(color.red, color.green, color.blue, 1.0f)
                    }
            )
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle("PBR Surface Properties")

    // Metallic
    PropertySlider(
        label = "Metallic",
        value = obj.material.metallic,
        onValueChange = { viewModel.updateMaterialProps(metallic = it) }
    )

    // Roughness
    PropertySlider(
        label = "Roughness",
        value = obj.material.roughness,
        onValueChange = { viewModel.updateMaterialProps(roughness = it) }
    )

    // Specular (Dielectric F0)
    PropertySlider(
        label = "Specular",
        value = obj.material.specular,
        onValueChange = { viewModel.updateMaterialProps(specular = it) }
    )

    // Transmission (Physical Glass / Water)
    PropertySlider(
        label = "Transmission (Glass)",
        value = obj.material.transmission,
        onValueChange = { viewModel.updateMaterialProps(transmission = it) }
    )

    // IOR (Index of Refraction)
    PropertySlider(
        label = "IOR (1.0 - 2.5)",
        value = ((obj.material.ior - 1.0f) / 1.5f).coerceIn(0f, 1f),
        onValueChange = { viewModel.updateMaterialProps(ior = 1.0f + it * 1.5f) }
    )

    // Clear Coat (Car Paint / Lacquer)
    PropertySlider(
        label = "Clear Coat",
        value = obj.material.clearCoat,
        onValueChange = { viewModel.updateMaterialProps(clearCoat = it) }
    )

    // Sheen (Fabric / Velvet Microfiber)
    PropertySlider(
        label = "Sheen (Fabric)",
        value = obj.material.sheen,
        onValueChange = { viewModel.updateMaterialProps(sheen = it) }
    )

    // Emission Intensity
    PropertySlider(
        label = "Emission",
        value = obj.material.emissionIntensity / 5f,
        onValueChange = { viewModel.updateMaterialProps(emission = it * 5f) }
    )

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle("PBR Material Presets")
    val presets = listOf(
        "Glass", "Water",
        "Car Paint", "Chrome",
        "Gold", "Copper",
        "Emerald", "Ruby",
        "Velvet", "Plastic",
        "Neon Cyan", "Neon Orange",
        "Clay", "Obsidian"
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        presets.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { name ->
                    NeoButton(
                        onClick = { viewModel.applyMaterialPreset(name) },
                        modifier = Modifier.weight(1f).height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = NeoTextPrimary,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LightTabContent(viewModel: AnimForgeViewModel, obj: com.example.scene.SceneObject?) {
    val light = obj?.lightData
    if (obj == null || light == null) {
        EmptyInspectorPlaceholder("Select a Light source or add one from the top bar")
        return
    }

    SectionTitle("Light Type: ${light.type.name}")

    PropertySlider(
        label = "Intensity",
        value = (light.intensity / 4f).coerceIn(0f, 1f),
        onValueChange = {
            light.intensity = it * 4f
            viewModel.syncUiState()
        }
    )

    PropertySlider(
        label = "Range",
        value = (light.range / 30f).coerceIn(0f, 1f),
        onValueChange = {
            light.range = it * 30f
            viewModel.syncUiState()
        }
    )

    SectionTitle("Light Color")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(
            Color(1f, 1f, 1f) to "White",
            Color(1f, 0.9f, 0.7f) to "Warm",
            Color(0.7f, 0.85f, 1f) to "Cool",
            Color(1f, 0.4f, 0.1f) to "Orange",
            Color(0.2f, 0.8f, 1f) to "Cyan"
        ).forEach { (color, _) ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable {
                        light.color = floatArrayOf(color.red, color.green, color.blue, 1f)
                        viewModel.syncUiState()
                    }
            )
        }
    }
}

@Composable
private fun PhysicsTabContent(viewModel: AnimForgeViewModel, obj: com.example.scene.SceneObject?) {
    if (obj == null || obj.type != ObjectType.MESH) {
        EmptyInspectorPlaceholder("Select a 3D Mesh to configure Rigid Body Physics")
        return
    }

    val p = obj.physicsData

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Enable Physics", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Switch(
            checked = p.enabled,
            onCheckedChange = {
                p.enabled = it
                viewModel.syncUiState()
            }
        )
    }

    if (p.enabled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (p.isDynamic) "Dynamic Body" else "Static Collider", style = MaterialTheme.typography.labelMedium)
            Switch(
                checked = p.isDynamic,
                onCheckedChange = {
                    p.isDynamic = it
                    viewModel.syncUiState()
                }
            )
        }

        PropertySlider(
            label = "Bounciness",
            value = p.restitution,
            onValueChange = {
                p.restitution = it
                viewModel.syncUiState()
            }
        )

        PropertySlider(
            label = "Friction",
            value = p.friction,
            onValueChange = {
                p.friction = it
                viewModel.syncUiState()
            }
        )

        PropertySlider(
            label = "Mass (kg)",
            value = (p.mass / 10f).coerceIn(0f, 1f),
            onValueChange = {
                p.mass = (it * 10f).coerceAtLeast(0.1f)
                viewModel.syncUiState()
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        NeoButton(
            onClick = { viewModel.bakePhysicsToTimeline() },
            modifier = Modifier.fillMaxWidth().height(32.dp),
            accentColor = StudioAmber,
            shape = RoundedCornerShape(6.dp)
        ) {
            Icon(Icons.Default.Animation, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bake to Keyframes", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = StudioAmber))
        }
    }
}

@Composable
private fun AnimationTabContent(viewModel: AnimForgeViewModel, obj: com.example.scene.SceneObject?, uiState: StudioUiState) {
    if (obj == null) {
        EmptyInspectorPlaceholder("Select an object to inspect keyframes")
        return
    }

    SectionTitle("Interpolation Curve")
    val interps = listOf(
        InterpolationType.EASE_IN_OUT to "Smooth",
        InterpolationType.LINEAR to "Linear",
        InterpolationType.BEZIER to "Bezier",
        InterpolationType.STEP to "Step"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        interps.forEach { (type, label) ->
            NeoButton(
                onClick = { viewModel.insertKeyframeForSelected(type) },
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle("Animated Tracks")
    val tracks = viewModel.timeline.getTracksForObject(obj.id)
    if (tracks.isEmpty()) {
        Text("No keyframes recorded for this object yet.", style = MaterialTheme.typography.bodySmall.copy(color = NeoTextTertiary))
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            tracks.forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeoSurfaceInset)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(track.property.name, style = MaterialTheme.typography.labelSmall.copy(color = StudioCyan, fontSize = 10.sp))
                    Text("${track.keyframes.size} keys", style = MaterialTheme.typography.labelSmall.copy(color = NeoTextSecondary, fontSize = 10.sp))
                }
            }
        }
    }
}

@Composable
private fun RenderTabContent(viewModel: AnimForgeViewModel) {
    SectionTitle("Workspace Theme")
    val viewportThemes = com.example.scene.ViewportTheme.values()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        viewportThemes.toList().chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { theme ->
                    val isSel = viewModel.renderer.scene.environment.viewportTheme == theme
                    NeoButton(
                        onClick = {
                            viewModel.renderer.scene.environment.viewportTheme = theme
                            viewModel.syncUiState()
                        },
                        isSelected = isSel,
                        modifier = Modifier.weight(1f).height(28.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = theme.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle("Eevee PBR Quality")
    val presets = listOf(RenderPreset.LOW, RenderPreset.MEDIUM, RenderPreset.HIGH, RenderPreset.ULTRA)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        presets.forEach { preset ->
            val isSel = viewModel.renderer.scene.environment.renderPreset == preset
            NeoButton(
                onClick = {
                    viewModel.renderer.scene.environment.renderPreset = preset
                    viewModel.syncUiState()
                },
                isSelected = isSel,
                modifier = Modifier.weight(1f).height(28.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(preset.name.take(3), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal))
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))
    SectionTitle("Real-Time Lighting & Effects")
    PropertySlider(
        label = "Ambient Light",
        value = viewModel.renderer.scene.environment.ambientIntensity,
        onValueChange = {
            viewModel.renderer.scene.environment.ambientIntensity = it
            viewModel.syncUiState()
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Dynamic PCF Shadows", style = MaterialTheme.typography.labelMedium)
        Switch(
            checked = viewModel.renderer.scene.environment.shadowEnabled,
            onCheckedChange = {
                viewModel.renderer.scene.environment.shadowEnabled = it
                viewModel.syncUiState()
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("HDR Bloom", style = MaterialTheme.typography.labelMedium)
        Switch(
            checked = viewModel.renderer.scene.environment.bloomEnabled,
            onCheckedChange = {
                viewModel.renderer.scene.environment.bloomEnabled = it
                viewModel.syncUiState()
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Grid Floor", style = MaterialTheme.typography.labelMedium)
        Switch(
            checked = viewModel.renderer.scene.environment.showGrid,
            onCheckedChange = {
                viewModel.renderer.scene.environment.showGrid = it
                viewModel.syncUiState()
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("World Axes", style = MaterialTheme.typography.labelMedium)
        Switch(
            checked = viewModel.renderer.scene.environment.showAxes,
            onCheckedChange = {
                viewModel.renderer.scene.environment.showAxes = it
                viewModel.syncUiState()
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = NeoTextPrimary,
            fontSize = 11.sp
        )
    )
}

@Composable
private fun PropertySlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = NeoTextSecondary, fontSize = 11.sp))
            Text(String.format(java.util.Locale.US, "%.2f", value), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = NeoTextPrimary, fontSize = 11.sp))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = StudioCyan,
                activeTrackColor = StudioCyan,
                inactiveTrackColor = NeoBorder
            )
        )
    }
}

@Composable
private fun EmptyInspectorPlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = NeoTextTertiary),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
