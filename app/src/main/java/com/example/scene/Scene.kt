package com.example.scene

import com.example.engine.math.Vector3
import com.example.engine.mesh.PrimitiveType
import java.util.UUID

enum class ViewportTheme(
    val displayName: String,
    val r: Float,
    val g: Float,
    val b: Float,
    val gridR: Float,
    val gridG: Float,
    val gridB: Float,
    val gridAlpha: Float
) {
    DARK_STUDIO("Dark Studio", 0.17f, 0.19f, 0.22f, 0.32f, 0.36f, 0.42f, 0.50f),
    BLENDER_GREY("Blender 3D", 0.23f, 0.25f, 0.28f, 0.38f, 0.42f, 0.48f, 0.45f),
    MAYA_SLATE("Maya Slate", 0.14f, 0.18f, 0.22f, 0.28f, 0.34f, 0.40f, 0.45f),
    CINEMA_CHARCOAL("Cinema Charcoal", 0.11f, 0.12f, 0.14f, 0.25f, 0.27f, 0.30f, 0.55f),
    LIGHT_STUDIO("Soft Light", 0.94f, 0.955f, 0.97f, 0.78f, 0.82f, 0.88f, 0.45f)
}

data class SceneEnvironment(
    var viewportTheme: ViewportTheme = ViewportTheme.DARK_STUDIO,
    var ambientColor: FloatArray = floatArrayOf(0.18f, 0.20f, 0.24f, 1.0f),
    var ambientIntensity: Float = 0.6f,
    var showGrid: Boolean = true,
    var showAxes: Boolean = true,
    var showOrigin: Boolean = true,
    var renderPreset: RenderPreset = RenderPreset.HIGH,
    var bloomEnabled: Boolean = true,
    var shadowEnabled: Boolean = true,
    var shadowBias: Float = 0.0035f,
    var contactShadows: Boolean = true,
    var fogEnabled: Boolean = false,
    var fogColor: FloatArray = floatArrayOf(0.94f, 0.96f, 0.98f, 1.0f),
    var exposure: Float = 0.0f,
    var contrast: Float = 1.08f,
    var saturation: Float = 1.05f,
    var vignetteStrength: Float = 0.35f,
    var fxaaEnabled: Boolean = true,
    var ssrEnabled: Boolean = true
) {
    fun copy(): SceneEnvironment = SceneEnvironment(
        viewportTheme = viewportTheme,
        ambientColor = ambientColor.clone(),
        ambientIntensity = ambientIntensity,
        showGrid = showGrid,
        showAxes = showAxes,
        showOrigin = showOrigin,
        renderPreset = renderPreset,
        bloomEnabled = bloomEnabled,
        shadowEnabled = shadowEnabled,
        shadowBias = shadowBias,
        contactShadows = contactShadows,
        fogEnabled = fogEnabled,
        fogColor = fogColor.clone(),
        exposure = exposure,
        contrast = contrast,
        saturation = saturation,
        vignetteStrength = vignetteStrength,
        fxaaEnabled = fxaaEnabled,
        ssrEnabled = ssrEnabled
    )
}

enum class RenderPreset {
    LOW,
    MEDIUM,
    HIGH,
    ULTRA
}

class Scene(
    var name: String = "Untitled Project",
    val objects: MutableList<SceneObject> = mutableListOf(),
    var environment: SceneEnvironment = SceneEnvironment()
) {
    fun findObjectById(id: String): SceneObject? = objects.find { it.id == id }

    fun addObject(obj: SceneObject) {
        objects.add(obj)
    }

    fun removeObject(id: String): Boolean {
        return objects.removeIf { it.id == id }
    }

    fun duplicateObject(id: String): SceneObject? {
        val original = findObjectById(id) ?: return null
        val newObj = original.copy(
            id = UUID.randomUUID().toString(),
            name = "${original.name}_Copy",
            transform = original.transform.copy().apply {
                position.x += 0.5f
                position.z += 0.5f
            }
        )
        objects.add(newObj)
        return newObj
    }

    companion object {
        fun createDefaultScene(): Scene {
            val scene = Scene("Sk8ani Studio Scene")

            // Default Sun Light (Directional)
            val sunLight = SceneObject(
                id = "light_sun_1",
                name = "Sun Light",
                type = ObjectType.LIGHT,
                transform = Transform(
                    position = Vector3(4.0f, 6.0f, 4.0f),
                    rotation = Vector3(45f, -30f, 0f)
                ),
                lightData = LightData(
                    type = LightType.DIRECTIONAL,
                    color = floatArrayOf(1.0f, 0.98f, 0.94f, 1.0f),
                    intensity = 1.4f
                )
            )

            // Fill Point Light
            val fillLight = SceneObject(
                id = "light_point_1",
                name = "Fill Light",
                type = ObjectType.LIGHT,
                transform = Transform(
                    position = Vector3(-3.0f, 3.0f, -3.0f)
                ),
                lightData = LightData(
                    type = LightType.POINT,
                    color = floatArrayOf(0.6f, 0.8f, 1.0f, 1.0f),
                    intensity = 0.8f,
                    range = 10f
                )
            )

            // Starter Hero Cube
            val starterCube = SceneObject(
                id = "mesh_cube_1",
                name = "Cube",
                type = ObjectType.MESH,
                primitiveType = PrimitiveType.CUBE,
                transform = Transform(
                    position = Vector3(0f, 0.5f, 0f),
                    scale = Vector3(1f, 1f, 1f)
                ),
                material = MaterialData.plastic().apply {
                    baseColor = floatArrayOf(0.0f, 0.57f, 0.92f, 1.0f) // Studio Cyan
                    roughness = 0.25f
                    metallic = 0.2f
                }
            )

            scene.addObject(sunLight)
            scene.addObject(fillLight)
            scene.addObject(starterCube)

            return scene
        }

        fun createSkateScene(): Scene {
            val scene = Scene("Sk8ani Skateboard Ramp")

            // Sun
            scene.addObject(
                SceneObject(
                    id = "sun_1",
                    name = "Sun",
                    type = ObjectType.LIGHT,
                    transform = Transform(position = Vector3(5f, 8f, 5f)),
                    lightData = LightData(type = LightType.DIRECTIONAL, intensity = 1.5f)
                )
            )

            // Ground plane
            scene.addObject(
                SceneObject(
                    id = "ground_1",
                    name = "Park Ground",
                    type = ObjectType.MESH,
                    primitiveType = PrimitiveType.PLANE,
                    transform = Transform(
                        position = Vector3(0f, 0f, 0f),
                        scale = Vector3(5f, 1f, 5f)
                    ),
                    material = MaterialData.matteClay().apply {
                        baseColor = floatArrayOf(0.88f, 0.90f, 0.94f, 1f)
                    },
                    physicsData = PhysicsData(enabled = true, isDynamic = false)
                )
            )

            // Skateboard Deck (scaled cube)
            scene.addObject(
                SceneObject(
                    id = "skate_deck",
                    name = "Skateboard Deck",
                    type = ObjectType.MESH,
                    primitiveType = PrimitiveType.CUBE,
                    transform = Transform(
                        position = Vector3(0f, 0.4f, 0f),
                        scale = Vector3(0.6f, 0.06f, 1.8f)
                    ),
                    material = MaterialData.neonOrange(),
                    physicsData = PhysicsData(enabled = true, isDynamic = true, mass = 1.5f, restitution = 0.7f)
                )
            )

            // Skate Wheels (Torus or Cylinders)
            scene.addObject(
                SceneObject(
                    id = "wheel_fl",
                    name = "Wheel Front-Left",
                    type = ObjectType.MESH,
                    primitiveType = PrimitiveType.CYLINDER,
                    transform = Transform(
                        position = Vector3(0.35f, 0.2f, 0.6f),
                        rotation = Vector3(0f, 0f, 90f),
                        scale = Vector3(0.2f, 0.1f, 0.2f)
                    ),
                    material = MaterialData.chrome()
                )
            )

            scene.addObject(
                SceneObject(
                    id = "wheel_fr",
                    name = "Wheel Front-Right",
                    type = ObjectType.MESH,
                    primitiveType = PrimitiveType.CYLINDER,
                    transform = Transform(
                        position = Vector3(-0.35f, 0.2f, 0.6f),
                        rotation = Vector3(0f, 0f, 90f),
                        scale = Vector3(0.2f, 0.1f, 0.2f)
                    ),
                    material = MaterialData.chrome()
                )
            )

            return scene
        }
    }
}
