package com.example.scene

import com.example.engine.math.Vector3
import com.example.engine.mesh.PrimitiveType
import java.util.UUID

data class Transform(
    var position: Vector3 = Vector3(0f, 0f, 0f),
    var rotation: Vector3 = Vector3(0f, 0f, 0f), // Euler angles in degrees
    var scale: Vector3 = Vector3(1f, 1f, 1f)
) {
    fun copy(): Transform = Transform(position.copy(), rotation.copy(), scale.copy())

    fun reset() {
        position.set(0f, 0f, 0f)
        rotation.set(0f, 0f, 0f)
        scale.set(1f, 1f, 1f)
    }
}

data class MaterialData(
    var baseColor: FloatArray = floatArrayOf(0.9f, 0.92f, 0.95f, 1.0f), // RGBA
    var metallic: Float = 0.1f,           // 0.0 to 1.0
    var roughness: Float = 0.4f,          // 0.0 to 1.0
    var specular: Float = 0.5f,           // 0.0 to 1.0 (Dielectric F0)
    var emissionColor: FloatArray = floatArrayOf(0f, 0f, 0f, 1f),
    var emissionIntensity: Float = 0.0f,
    var transmission: Float = 0.0f,       // 0.0 to 1.0 (Glass / Water refraction)
    var ior: Float = 1.45f,               // 1.0 to 3.0 (Index of Refraction)
    var clearCoat: Float = 0.0f,          // 0.0 to 1.0 (Car paint / Lacquer)
    var clearCoatRoughness: Float = 0.05f,// 0.0 to 1.0
    var sheen: Float = 0.0f,              // 0.0 to 1.0 (Fabric / Velvet)
    var anisotropy: Float = 0.0f,         // 0.0 to 1.0 (Brushed metal)
    var ao: Float = 1.0f,                 // 0.0 to 1.0 (Ambient Occlusion)
    var opacity: Float = 1.0f,
    var isWireframe: Boolean = false,
    var isFlatShaded: Boolean = false
) {
    fun copy(): MaterialData = MaterialData(
        baseColor = baseColor.clone(),
        metallic = metallic,
        roughness = roughness,
        specular = specular,
        emissionColor = emissionColor.clone(),
        emissionIntensity = emissionIntensity,
        transmission = transmission,
        ior = ior,
        clearCoat = clearCoat,
        clearCoatRoughness = clearCoatRoughness,
        sheen = sheen,
        anisotropy = anisotropy,
        ao = ao,
        opacity = opacity,
        isWireframe = isWireframe,
        isFlatShaded = isFlatShaded
    )

    companion object {
        fun defaultMaterial() = MaterialData()

        fun glass() = MaterialData(
            baseColor = floatArrayOf(0.95f, 0.98f, 1.0f, 0.2f),
            metallic = 0.0f,
            roughness = 0.02f,
            specular = 0.9f,
            transmission = 0.95f,
            ior = 1.52f,
            clearCoat = 0.5f
        )

        fun water() = MaterialData(
            baseColor = floatArrayOf(0.75f, 0.92f, 0.98f, 0.35f),
            metallic = 0.0f,
            roughness = 0.04f,
            specular = 0.7f,
            transmission = 0.90f,
            ior = 1.33f
        )

        fun carPaint() = MaterialData(
            baseColor = floatArrayOf(0.85f, 0.08f, 0.12f, 1.0f), // Candy Red
            metallic = 0.65f,
            roughness = 0.3f,
            specular = 0.8f,
            clearCoat = 1.0f,
            clearCoatRoughness = 0.03f
        )

        fun chrome() = MaterialData(
            baseColor = floatArrayOf(0.96f, 0.97f, 0.99f, 1.0f),
            metallic = 1.0f,
            roughness = 0.05f,
            specular = 1.0f
        )

        fun gold() = MaterialData(
            baseColor = floatArrayOf(1.0f, 0.78f, 0.28f, 1.0f),
            metallic = 0.95f,
            roughness = 0.18f,
            specular = 0.9f
        )

        fun copper() = MaterialData(
            baseColor = floatArrayOf(0.98f, 0.55f, 0.42f, 1.0f),
            metallic = 0.92f,
            roughness = 0.22f,
            specular = 0.85f
        )

        fun emerald() = MaterialData(
            baseColor = floatArrayOf(0.1f, 0.85f, 0.45f, 0.7f),
            metallic = 0.0f,
            roughness = 0.05f,
            specular = 0.95f,
            transmission = 0.8f,
            ior = 1.57f
        )

        fun ruby() = MaterialData(
            baseColor = floatArrayOf(0.92f, 0.08f, 0.28f, 0.7f),
            metallic = 0.0f,
            roughness = 0.05f,
            specular = 0.95f,
            transmission = 0.8f,
            ior = 1.76f
        )

        fun velvet() = MaterialData(
            baseColor = floatArrayOf(0.45f, 0.08f, 0.25f, 1.0f), // Deep Burgundy
            metallic = 0.0f,
            roughness = 0.75f,
            sheen = 0.9f
        )

        fun plastic() = MaterialData(
            baseColor = floatArrayOf(0.12f, 0.53f, 0.9f, 1.0f),
            metallic = 0.0f,
            roughness = 0.35f,
            specular = 0.5f
        )

        fun neonCyan() = MaterialData(
            baseColor = floatArrayOf(0.0f, 0.9f, 1.0f, 1.0f),
            metallic = 0.0f,
            roughness = 0.1f,
            specular = 0.5f,
            emissionColor = floatArrayOf(0.0f, 0.9f, 1.0f, 1.0f),
            emissionIntensity = 2.5f
        )

        fun neonOrange() = MaterialData(
            baseColor = floatArrayOf(1.0f, 0.45f, 0.0f, 1.0f),
            metallic = 0.0f,
            roughness = 0.1f,
            specular = 0.5f,
            emissionColor = floatArrayOf(1.0f, 0.45f, 0.0f, 1.0f),
            emissionIntensity = 2.5f
        )

        fun matteClay() = MaterialData(
            baseColor = floatArrayOf(0.85f, 0.72f, 0.65f, 1.0f),
            metallic = 0.0f,
            roughness = 0.9f,
            specular = 0.1f
        )

        fun darkObsidian() = MaterialData(
            baseColor = floatArrayOf(0.12f, 0.14f, 0.18f, 1.0f),
            metallic = 0.8f,
            roughness = 0.15f,
            specular = 0.8f,
            clearCoat = 0.8f
        )
    }
}

enum class LightType {
    DIRECTIONAL,
    POINT,
    SPOT
}

data class LightData(
    var type: LightType = LightType.POINT,
    var color: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),
    var intensity: Float = 1.2f,
    var range: Float = 15.0f,
    var spotAngleDegrees: Float = 45.0f
) {
    fun copy(): LightData = LightData(
        type = type,
        color = color.clone(),
        intensity = intensity,
        range = range,
        spotAngleDegrees = spotAngleDegrees
    )
}

data class PhysicsData(
    var enabled: Boolean = false,
    var isDynamic: Boolean = true, // true = falls and collides, false = static obstacle / floor
    var mass: Float = 1.0f,
    var restitution: Float = 0.65f, // Bounciness 0 to 1
    var friction: Float = 0.4f,
    var velocity: Vector3 = Vector3(0f, 0f, 0f),
    var angularVelocity: Vector3 = Vector3(0f, 0f, 0f)
) {
    fun copy(): PhysicsData = PhysicsData(
        enabled = enabled,
        isDynamic = isDynamic,
        mass = mass,
        restitution = restitution,
        friction = friction,
        velocity = velocity.copy(),
        angularVelocity = angularVelocity.copy()
    )
}

enum class ObjectType {
    MESH,
    LIGHT,
    CAMERA,
    GROUP
}

data class SceneObject(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Object",
    val type: ObjectType = ObjectType.MESH,
    val primitiveType: PrimitiveType = PrimitiveType.CUBE,
    var transform: Transform = Transform(),
    var material: MaterialData = MaterialData.defaultMaterial(),
    var lightData: LightData? = null,
    var physicsData: PhysicsData = PhysicsData(),
    var isVisible: Boolean = true,
    var isLocked: Boolean = false,
    var parentId: String? = null
) {
    fun copy(
        id: String = this.id,
        name: String = this.name,
        transform: Transform = this.transform.copy(),
        material: MaterialData = this.material.copy(),
        lightData: LightData? = this.lightData?.copy(),
        physicsData: PhysicsData = this.physicsData.copy(),
        isVisible: Boolean = this.isVisible,
        isLocked: Boolean = this.isLocked,
        parentId: String? = this.parentId
    ): SceneObject {
        return SceneObject(
            id = id,
            name = name,
            type = this.type,
            primitiveType = this.primitiveType,
            transform = transform,
            material = material,
            lightData = lightData,
            physicsData = physicsData,
            isVisible = isVisible,
            isLocked = isLocked,
            parentId = parentId
        )
    }
}
