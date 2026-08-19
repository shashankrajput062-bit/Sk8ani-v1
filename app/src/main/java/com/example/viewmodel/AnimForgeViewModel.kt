package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.animation.AnimatedProperty
import com.example.animation.AnimationTimeline
import com.example.animation.InterpolationType
import com.example.engine.camera.CameraPreset
import com.example.engine.mesh.PrimitiveType
import com.example.engine.renderer.AnimForgeRenderer
import com.example.engine.renderer.GizmoMode
import com.example.engine.renderer.TransformSpace
import com.example.engine.renderer.ViewportShading
import com.example.history.*
import com.example.io.ObjImporterExporter
import com.example.physics.PhysicsEngine
import com.example.project.ProjectManager
import com.example.scene.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class InspectorTab {
    TRANSFORM,
    MATERIAL,
    LIGHT,
    PHYSICS,
    ANIMATION,
    RENDER
}

data class StudioUiState(
    val projectName: String = "Sk8ani Studio",
    val selectedObjectId: String? = null,
    val selectedObject: SceneObject? = null,
    val objectList: List<SceneObject> = emptyList(),
    val currentFrame: Int = 0,
    val startFrame: Int = 0,
    val endFrame: Int = 120,
    val fps: Int = 30,
    val isPlaying: Boolean = false,
    val isAutoKey: Boolean = false,
    val keyframedFrames: Set<Int> = emptySet(),
    val gizmoMode: GizmoMode = GizmoMode.TRANSLATE,
    val transformSpace: TransformSpace = TransformSpace.WORLD,
    val shadingMode: ViewportShading = ViewportShading.RENDERED_PBR,
    val activeInspectorTab: InspectorTab = InspectorTab.TRANSFORM,
    val isOutlinerOpen: Boolean = true,
    val isInspectorOpen: Boolean = true,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isPhysicsSimulating: Boolean = false,
    val showAddMenu: Boolean = false,
    val showExportDialog: Boolean = false,
    val showTemplateDialog: Boolean = false,
    val statusMessage: String? = null
)

class AnimForgeViewModel(application: Application) : AndroidViewModel(application) {

    val renderer = AnimForgeRenderer()
    val timeline = AnimationTimeline()
    val physicsEngine = PhysicsEngine()
    val history = CommandHistory()

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var physicsJob: Job? = null

    init {
        // Initial setup
        val defaultScene = Scene.createDefaultScene()
        renderer.scene = defaultScene
        timeline.startFrame = 0
        timeline.endFrame = 120
        timeline.fps = 30

        // Select the default cube
        val cube = defaultScene.objects.find { it.type == ObjectType.MESH }
        renderer.selectedObjectId = cube?.id
        
        syncUiState()

        // Check if autosave exists
        ProjectManager.loadAutoSave(getApplication())?.let { autoProject ->
            // Ready if user wants to restore
        }
    }

    fun syncUiState() {
        val selId = renderer.selectedObjectId
        val selObj = renderer.scene.findObjectById(selId ?: "")
        val keyframed = if (selId != null) timeline.getKeyframedFramesForObject(selId) else emptySet()

        _uiState.update {
            it.copy(
                projectName = renderer.scene.name,
                selectedObjectId = selId,
                selectedObject = selObj?.copy(),
                objectList = renderer.scene.objects.map { obj -> obj.copy() },
                currentFrame = timeline.currentFrame,
                startFrame = timeline.startFrame,
                endFrame = timeline.endFrame,
                fps = timeline.fps,
                isPlaying = timeline.isPlaying,
                isAutoKey = timeline.isAutoKeyEnabled,
                keyframedFrames = keyframed,
                gizmoMode = renderer.gizmoMode,
                transformSpace = renderer.transformSpace,
                shadingMode = renderer.shadingMode,
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                isPhysicsSimulating = physicsEngine.isSimulating
            )
        }
    }

    fun selectObject(id: String?) {
        renderer.selectedObjectId = id
        syncUiState()
    }

    fun setGizmoMode(mode: GizmoMode) {
        renderer.gizmoMode = mode
        syncUiState()
    }

    fun setTransformSpace(space: TransformSpace) {
        renderer.transformSpace = space
        syncUiState()
    }

    fun setShadingMode(shading: ViewportShading) {
        renderer.shadingMode = shading
        syncUiState()
    }

    fun setInspectorTab(tab: InspectorTab) {
        _uiState.update { it.copy(activeInspectorTab = tab) }
    }

    fun toggleOutliner() {
        _uiState.update { it.copy(isOutlinerOpen = !it.isOutlinerOpen) }
    }

    fun toggleInspector() {
        _uiState.update { it.copy(isInspectorOpen = !it.isInspectorOpen) }
    }

    // --- Transform Controls ---

    fun updatePosition(x: Float, y: Float, z: Float) {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        val oldTransform = obj.transform.copy()
        obj.transform.position.set(x, y, z)
        val newTransform = obj.transform.copy()

        if (timeline.isAutoKeyEnabled) {
            timeline.insertTransformKeyframe(obj, timeline.currentFrame)
        }

        history.executeCommand(TransformCommand(obj.id, oldTransform, newTransform), renderer.scene)
        syncUiState()
    }

    fun updateRotation(rx: Float, ry: Float, rz: Float) {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        val oldTransform = obj.transform.copy()
        obj.transform.rotation.set(rx, ry, rz)
        val newTransform = obj.transform.copy()

        if (timeline.isAutoKeyEnabled) {
            timeline.insertTransformKeyframe(obj, timeline.currentFrame)
        }

        history.executeCommand(TransformCommand(obj.id, oldTransform, newTransform), renderer.scene)
        syncUiState()
    }

    fun updateScale(sx: Float, sy: Float, sz: Float) {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        val oldTransform = obj.transform.copy()
        obj.transform.scale.set(sx.coerceAtLeast(0.01f), sy.coerceAtLeast(0.01f), sz.coerceAtLeast(0.01f))
        val newTransform = obj.transform.copy()

        if (timeline.isAutoKeyEnabled) {
            timeline.insertTransformKeyframe(obj, timeline.currentFrame)
        }

        history.executeCommand(TransformCommand(obj.id, oldTransform, newTransform), renderer.scene)
        syncUiState()
    }

    fun resetTransform() {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        val old = obj.transform.copy()
        obj.transform.reset()
        history.executeCommand(TransformCommand(obj.id, old, obj.transform.copy()), renderer.scene)
        syncUiState()
    }

    fun focusSelection() {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "")
        if (obj != null) {
            renderer.camera.focusOn(obj.transform.position)
        } else {
            renderer.camera.focusOn(com.example.engine.math.Vector3(0f, 0.5f, 0f))
        }
    }

    fun setCameraPreset(preset: CameraPreset) {
        renderer.camera.setPresetView(preset)
    }

    // --- Object Management ---

    fun addPrimitive(type: PrimitiveType) {
        val name = when (type) {
            PrimitiveType.CUBE -> "Cube"
            PrimitiveType.SPHERE -> "Sphere"
            PrimitiveType.CYLINDER -> "Cylinder"
            PrimitiveType.CONE -> "Cone"
            PrimitiveType.TORUS -> "Torus"
            PrimitiveType.PLANE -> "Plane"
            PrimitiveType.CAPSULE -> "Capsule"
            PrimitiveType.CUSTOM -> "Mesh"
        }

        val count = renderer.scene.objects.count { it.name.startsWith(name) } + 1
        val newObj = SceneObject(
            id = UUID.randomUUID().toString(),
            name = "$name $count",
            type = ObjectType.MESH,
            primitiveType = type,
            transform = Transform(
                position = com.example.engine.math.Vector3(0f, 0.5f, 0f)
            ),
            material = MaterialData.plastic()
        )

        history.executeCommand(AddObjectCommand(newObj), renderer.scene)
        renderer.selectedObjectId = newObj.id
        _uiState.update { it.copy(showAddMenu = false) }
        syncUiState()
        showStatus("Added $name")
    }

    fun addLight(type: LightType) {
        val name = when (type) {
            LightType.DIRECTIONAL -> "Sun Light"
            LightType.POINT -> "Point Light"
            LightType.SPOT -> "Spot Light"
        }
        val count = renderer.scene.objects.count { it.name.startsWith(name) } + 1
        val lightObj = SceneObject(
            id = UUID.randomUUID().toString(),
            name = "$name $count",
            type = ObjectType.LIGHT,
            transform = Transform(
                position = com.example.engine.math.Vector3(2f, 4f, 2f)
            ),
            lightData = LightData(type = type, intensity = 1.2f)
        )

        history.executeCommand(AddObjectCommand(lightObj), renderer.scene)
        renderer.selectedObjectId = lightObj.id
        _uiState.update { it.copy(showAddMenu = false) }
        syncUiState()
        showStatus("Added $name")
    }

    fun duplicateSelected() {
        val selId = renderer.selectedObjectId ?: return
        val dup = renderer.scene.duplicateObject(selId)
        if (dup != null) {
            renderer.selectedObjectId = dup.id
            syncUiState()
            showStatus("Duplicated ${dup.name}")
        }
    }

    fun deleteSelected() {
        val selId = renderer.selectedObjectId ?: return
        val obj = renderer.scene.findObjectById(selId) ?: return
        history.executeCommand(DeleteObjectCommand(obj), renderer.scene)
        renderer.selectedObjectId = null
        syncUiState()
        showStatus("Deleted ${obj.name}")
    }

    fun renameObject(id: String, newName: String) {
        renderer.scene.findObjectById(id)?.name = newName.trim()
        syncUiState()
    }

    fun toggleVisibility(id: String) {
        renderer.scene.findObjectById(id)?.let {
            it.isVisible = !it.isVisible
            syncUiState()
        }
    }

    fun toggleLock(id: String) {
        renderer.scene.findObjectById(id)?.let {
            it.isLocked = !it.isLocked
            syncUiState()
        }
    }

    // --- Material System ---

    fun updateMaterialColor(r: Float, g: Float, b: Float, a: Float = 1.0f) {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        val old = obj.material.copy()
        obj.material.baseColor = floatArrayOf(r, g, b, a)
        history.executeCommand(MaterialCommand(obj.id, old, obj.material.copy()), renderer.scene)
        syncUiState()
    }

    fun updateMaterialProps(
        metallic: Float? = null,
        roughness: Float? = null,
        specular: Float? = null,
        emission: Float? = null,
        transmission: Float? = null,
        ior: Float? = null,
        clearCoat: Float? = null,
        clearCoatRoughness: Float? = null,
        sheen: Float? = null,
        anisotropy: Float? = null,
        ao: Float? = null,
        opacity: Float? = null,
        wireframe: Boolean? = null
    ) {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        val old = obj.material.copy()
        metallic?.let { obj.material.metallic = it }
        roughness?.let { obj.material.roughness = it }
        specular?.let { obj.material.specular = it }
        emission?.let { obj.material.emissionIntensity = it }
        transmission?.let { obj.material.transmission = it }
        ior?.let { obj.material.ior = it }
        clearCoat?.let { obj.material.clearCoat = it }
        clearCoatRoughness?.let { obj.material.clearCoatRoughness = it }
        sheen?.let { obj.material.sheen = it }
        anisotropy?.let { obj.material.anisotropy = it }
        ao?.let { obj.material.ao = it }
        opacity?.let { obj.material.opacity = it }
        wireframe?.let { obj.material.isWireframe = it }
        history.executeCommand(MaterialCommand(obj.id, old, obj.material.copy()), renderer.scene)
        syncUiState()
    }

    fun applyMaterialPreset(presetName: String) {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        val old = obj.material.copy()
        obj.material = when (presetName.lowercase()) {
            "glass" -> MaterialData.glass()
            "water" -> MaterialData.water()
            "car paint" -> MaterialData.carPaint()
            "chrome" -> MaterialData.chrome()
            "gold" -> MaterialData.gold()
            "copper" -> MaterialData.copper()
            "emerald" -> MaterialData.emerald()
            "ruby" -> MaterialData.ruby()
            "velvet" -> MaterialData.velvet()
            "plastic" -> MaterialData.plastic()
            "neon cyan" -> MaterialData.neonCyan()
            "neon orange" -> MaterialData.neonOrange()
            "clay" -> MaterialData.matteClay()
            "obsidian" -> MaterialData.darkObsidian()
            else -> MaterialData.defaultMaterial()
        }
        history.executeCommand(MaterialCommand(obj.id, old, obj.material.copy()), renderer.scene)
        syncUiState()
        showStatus("Applied $presetName PBR material")
    }

    fun updateEnvironmentSettings(
        theme: ViewportTheme? = null,
        showGrid: Boolean? = null,
        showAxes: Boolean? = null,
        bloomEnabled: Boolean? = null,
        shadowEnabled: Boolean? = null,
        shadowBias: Float? = null,
        contactShadows: Boolean? = null,
        renderPreset: RenderPreset? = null
    ) {
        theme?.let { renderer.scene.environment.viewportTheme = it }
        showGrid?.let { renderer.scene.environment.showGrid = it }
        showAxes?.let { renderer.scene.environment.showAxes = it }
        bloomEnabled?.let { renderer.scene.environment.bloomEnabled = it }
        shadowEnabled?.let { renderer.scene.environment.shadowEnabled = it }
        shadowBias?.let { renderer.scene.environment.shadowBias = it }
        contactShadows?.let { renderer.scene.environment.contactShadows = it }
        renderPreset?.let { renderer.scene.environment.renderPreset = it }
        syncUiState()
    }

    // --- Animation Timeline System ---

    fun setTimelineFrame(frame: Int) {
        val clamped = frame.coerceIn(timeline.startFrame, timeline.endFrame)
        timeline.currentFrame = clamped
        timeline.evaluateScene(renderer.scene, clamped.toFloat())
        syncUiState()
    }

    fun togglePlayback() {
        if (timeline.isPlaying) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        timeline.isPlaying = true
        syncUiState()

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val intervalMs = (1000L / timeline.fps.toLong()).coerceAtLeast(16L)
            while (isActive && timeline.isPlaying) {
                var nextFrame = timeline.currentFrame + 1
                if (nextFrame > timeline.endFrame) {
                    nextFrame = if (timeline.isLooping) timeline.startFrame else timeline.endFrame
                    if (!timeline.isLooping) {
                        timeline.isPlaying = false
                        syncUiState()
                        break
                    }
                }
                timeline.currentFrame = nextFrame
                timeline.evaluateScene(renderer.scene, nextFrame.toFloat())
                syncUiState()
                delay(intervalMs)
            }
        }
    }

    fun pausePlayback() {
        timeline.isPlaying = false
        playbackJob?.cancel()
        syncUiState()
    }

    fun stopPlayback() {
        pausePlayback()
        setTimelineFrame(timeline.startFrame)
    }

    fun insertKeyframeForSelected(interpolation: InterpolationType = InterpolationType.EASE_IN_OUT) {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        timeline.insertTransformKeyframe(obj, timeline.currentFrame, interpolation)
        syncUiState()
        showStatus("Keyframe added at frame ${timeline.currentFrame}")
    }

    fun deleteKeyframeAtCurrent() {
        val obj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "") ?: return
        timeline.deleteKeyframeAtCurrent(obj, timeline.currentFrame)
        syncUiState()
        showStatus("Keyframe removed at frame ${timeline.currentFrame}")
    }

    fun toggleAutoKey() {
        timeline.isAutoKeyEnabled = !timeline.isAutoKeyEnabled
        syncUiState()
        showStatus(if (timeline.isAutoKeyEnabled) "Auto-Key ON" else "Auto-Key OFF")
    }

    // --- Physics Simulation ---

    fun togglePhysicsSimulation() {
        if (physicsEngine.isSimulating) {
            physicsEngine.stopSimulation(renderer.scene)
            physicsJob?.cancel()
            syncUiState()
            showStatus("Physics Stopped")
        } else {
            physicsEngine.startSimulation(renderer.scene)
            syncUiState()
            showStatus("Physics Simulating...")

            physicsJob?.cancel()
            physicsJob = viewModelScope.launch {
                while (isActive && physicsEngine.isSimulating) {
                    physicsEngine.step(renderer.scene, 1f / 60f)
                    syncUiState()
                    delay(16L)
                }
            }
        }
    }

    fun bakePhysicsToTimeline() {
        physicsEngine.bakePhysicsToKeyframes(renderer.scene, timeline)
        syncUiState()
        showStatus("Physics baked to keyframes!")
    }

    // --- History / Project / Templates ---

    fun undo() {
        val cmd = history.undo(renderer.scene)
        syncUiState()
        if (cmd != null) showStatus("Undo: ${cmd.description}")
    }

    fun redo() {
        val cmd = history.redo(renderer.scene)
        syncUiState()
        if (cmd != null) showStatus("Redo: ${cmd.description}")
    }

    fun loadTemplate(templateName: String) {
        when (templateName) {
            "Skater Scene" -> {
                renderer.scene = Scene.createSkateScene()
                timeline.tracks.clear()
                renderer.selectedObjectId = renderer.scene.objects.firstOrNull { it.type == ObjectType.MESH }?.id
            }
            "Kinetic Orbit" -> {
                val proj = ProjectManager.createKineticBounceProject()
                renderer.scene = proj.scene
                timeline.tracks.clear()
                timeline.tracks.addAll(proj.timeline.tracks)
                timeline.fps = proj.timeline.fps
                renderer.selectedObjectId = "torus_center"
            }
            else -> {
                renderer.scene = Scene.createDefaultScene()
                timeline.tracks.clear()
                renderer.selectedObjectId = renderer.scene.objects.firstOrNull { it.type == ObjectType.MESH }?.id
            }
        }
        _uiState.update { it.copy(showTemplateDialog = false) }
        syncUiState()
        showStatus("Loaded template: $templateName")
    }

    fun exportObj(): String {
        val objContent = ObjImporterExporter.exportSceneToObj(renderer.scene)
        showStatus("OBJ scene exported (${objContent.length} bytes)")
        return objContent
    }

    fun importObjString(objContent: String) {
        val meshData = ObjImporterExporter.parseObj(objContent)
        if (meshData != null) {
            val newObj = SceneObject(
                id = UUID.randomUUID().toString(),
                name = "Imported_Mesh",
                type = ObjectType.MESH,
                primitiveType = PrimitiveType.CUSTOM,
                transform = Transform(position = com.example.engine.math.Vector3(0f, 0.5f, 0f)),
                material = MaterialData.plastic()
            )
            renderer.scene.addObject(newObj)
            renderer.selectedObjectId = newObj.id
            syncUiState()
            showStatus("Model successfully imported!")
        } else {
            showStatus("Failed to parse OBJ data")
        }
    }

    fun saveProject() {
        ProjectManager.saveAutoSave(getApplication(), renderer.scene, timeline)
        showStatus("Project saved successfully")
    }

    fun setShowAddMenu(show: Boolean) {
        _uiState.update { it.copy(showAddMenu = show) }
    }

    fun setShowTemplateDialog(show: Boolean) {
        _uiState.update { it.copy(showTemplateDialog = show) }
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    private fun showStatus(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { if (it.statusMessage == message) it.copy(statusMessage = null) else it }
        }
    }
}
