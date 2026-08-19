package com.example.animation

import com.example.engine.math.Vector3
import com.example.scene.Scene
import com.example.scene.SceneObject
import kotlin.math.*

enum class InterpolationType {
    LINEAR,
    EASE_IN_OUT,
    BEZIER,
    STEP
}

enum class AnimatedProperty {
    POSITION_X,
    POSITION_Y,
    POSITION_Z,
    ROTATION_X,
    ROTATION_Y,
    ROTATION_Z,
    SCALE_X,
    SCALE_Y,
    SCALE_Z,
    MATERIAL_COLOR_R,
    MATERIAL_COLOR_G,
    MATERIAL_COLOR_B,
    EMISSION_INTENSITY,
    LIGHT_INTENSITY
}

data class Keyframe(
    val frame: Int,
    var value: Float,
    var interpolation: InterpolationType = InterpolationType.EASE_IN_OUT,
    var handleInX: Float = -0.3f,
    var handleInY: Float = 0f,
    var handleOutX: Float = 0.3f,
    var handleOutY: Float = 0f
) {
    fun copy(): Keyframe = Keyframe(
        frame = frame,
        value = value,
        interpolation = interpolation,
        handleInX = handleInX,
        handleInY = handleInY,
        handleOutX = handleOutX,
        handleOutY = handleOutY
    )
}

data class AnimationTrack(
    val objectId: String,
    val property: AnimatedProperty,
    val keyframes: MutableList<Keyframe> = mutableListOf()
) {
    fun addOrUpdateKeyframe(frame: Int, value: Float, interpolation: InterpolationType = InterpolationType.EASE_IN_OUT) {
        val existing = keyframes.find { it.frame == frame }
        if (existing != null) {
            existing.value = value
            existing.interpolation = interpolation
        } else {
            keyframes.add(Keyframe(frame, value, interpolation))
            keyframes.sortBy { it.frame }
        }
    }

    fun removeKeyframe(frame: Int): Boolean {
        return keyframes.removeIf { it.frame == frame }
    }

    fun evaluate(frame: Float): Float? {
        if (keyframes.isEmpty()) return null
        if (keyframes.size == 1 || frame <= keyframes.first().frame) {
            return keyframes.first().value
        }
        if (frame >= keyframes.last().frame) {
            return keyframes.last().value
        }

        // Find surrounding keyframes
        for (i in 0 until keyframes.size - 1) {
            val k0 = keyframes[i]
            val k1 = keyframes[i + 1]

            if (frame >= k0.frame && frame <= k1.frame) {
                val span = (k1.frame - k0.frame).toFloat()
                if (span <= 0f) return k0.value

                val t = (frame - k0.frame) / span

                return when (k0.interpolation) {
                    InterpolationType.STEP -> k0.value
                    InterpolationType.LINEAR -> k0.value + (k1.value - k0.value) * t
                    InterpolationType.EASE_IN_OUT -> {
                        // Smooth cubic hermite S-curve
                        val smoothT = t * t * (3f - 2f * t)
                        k0.value + (k1.value - k0.value) * smoothT
                    }
                    InterpolationType.BEZIER -> {
                        // Approximate cubic bezier
                        val t2 = t * t
                        val t3 = t2 * t
                        val mt = 1f - t
                        val mt2 = mt * mt
                        val mt3 = mt2 * mt
                        val p0 = k0.value
                        val p1 = k0.value + k0.handleOutY
                        val p2 = k1.value + k1.handleInY
                        val p3 = k1.value
                        mt3 * p0 + 3f * mt2 * t * p1 + 3f * mt * t2 * p2 + t3 * p3
                    }
                }
            }
        }

        return keyframes.last().value
    }
}

class AnimationTimeline(
    var currentFrame: Int = 0,
    var startFrame: Int = 0,
    var endFrame: Int = 120,
    var fps: Int = 30,
    var isPlaying: Boolean = false,
    var isLooping: Boolean = true,
    var isAutoKeyEnabled: Boolean = false,
    val tracks: MutableList<AnimationTrack> = mutableListOf()
) {
    fun findTrack(objectId: String, property: AnimatedProperty): AnimationTrack? {
        return tracks.find { it.objectId == objectId && it.property == property }
    }

    fun getOrCreateTrack(objectId: String, property: AnimatedProperty): AnimationTrack {
        var track = findTrack(objectId, property)
        if (track == null) {
            track = AnimationTrack(objectId, property)
            tracks.add(track)
        }
        return track
    }

    fun getTracksForObject(objectId: String): List<AnimationTrack> {
        return tracks.filter { it.objectId == objectId }
    }

    fun getKeyframedFramesForObject(objectId: String): Set<Int> {
        return tracks.filter { it.objectId == objectId }.flatMap { it.keyframes.map { k -> k.frame } }.toSet()
    }

    fun insertTransformKeyframe(obj: SceneObject, frame: Int = currentFrame, interpolation: InterpolationType = InterpolationType.EASE_IN_OUT) {
        getOrCreateTrack(obj.id, AnimatedProperty.POSITION_X).addOrUpdateKeyframe(frame, obj.transform.position.x, interpolation)
        getOrCreateTrack(obj.id, AnimatedProperty.POSITION_Y).addOrUpdateKeyframe(frame, obj.transform.position.y, interpolation)
        getOrCreateTrack(obj.id, AnimatedProperty.POSITION_Z).addOrUpdateKeyframe(frame, obj.transform.position.z, interpolation)

        getOrCreateTrack(obj.id, AnimatedProperty.ROTATION_X).addOrUpdateKeyframe(frame, obj.transform.rotation.x, interpolation)
        getOrCreateTrack(obj.id, AnimatedProperty.ROTATION_Y).addOrUpdateKeyframe(frame, obj.transform.rotation.y, interpolation)
        getOrCreateTrack(obj.id, AnimatedProperty.ROTATION_Z).addOrUpdateKeyframe(frame, obj.transform.rotation.z, interpolation)

        getOrCreateTrack(obj.id, AnimatedProperty.SCALE_X).addOrUpdateKeyframe(frame, obj.transform.scale.x, interpolation)
        getOrCreateTrack(obj.id, AnimatedProperty.SCALE_Y).addOrUpdateKeyframe(frame, obj.transform.scale.y, interpolation)
        getOrCreateTrack(obj.id, AnimatedProperty.SCALE_Z).addOrUpdateKeyframe(frame, obj.transform.scale.z, interpolation)
    }

    fun deleteKeyframeAtCurrent(obj: SceneObject, frame: Int = currentFrame) {
        getTracksForObject(obj.id).forEach { track ->
            track.removeKeyframe(frame)
        }
    }

    /**
     * Apply sampled animation properties to the entire scene for the given frame
     */
    fun evaluateScene(scene: Scene, frame: Float = currentFrame.toFloat()) {
        for (obj in scene.objects) {
            val posX = findTrack(obj.id, AnimatedProperty.POSITION_X)?.evaluate(frame)
            val posY = findTrack(obj.id, AnimatedProperty.POSITION_Y)?.evaluate(frame)
            val posZ = findTrack(obj.id, AnimatedProperty.POSITION_Z)?.evaluate(frame)
            if (posX != null) obj.transform.position.x = posX
            if (posY != null) obj.transform.position.y = posY
            if (posZ != null) obj.transform.position.z = posZ

            val rotX = findTrack(obj.id, AnimatedProperty.ROTATION_X)?.evaluate(frame)
            val rotY = findTrack(obj.id, AnimatedProperty.ROTATION_Y)?.evaluate(frame)
            val rotZ = findTrack(obj.id, AnimatedProperty.ROTATION_Z)?.evaluate(frame)
            if (rotX != null) obj.transform.rotation.x = rotX
            if (rotY != null) obj.transform.rotation.y = rotY
            if (rotZ != null) obj.transform.rotation.z = rotZ

            val scaX = findTrack(obj.id, AnimatedProperty.SCALE_X)?.evaluate(frame)
            val scaY = findTrack(obj.id, AnimatedProperty.SCALE_Y)?.evaluate(frame)
            val scaZ = findTrack(obj.id, AnimatedProperty.SCALE_Z)?.evaluate(frame)
            if (scaX != null) obj.transform.scale.x = scaX
            if (scaY != null) obj.transform.scale.y = scaY
            if (scaZ != null) obj.transform.scale.z = scaZ

            val colR = findTrack(obj.id, AnimatedProperty.MATERIAL_COLOR_R)?.evaluate(frame)
            val colG = findTrack(obj.id, AnimatedProperty.MATERIAL_COLOR_G)?.evaluate(frame)
            val colB = findTrack(obj.id, AnimatedProperty.MATERIAL_COLOR_B)?.evaluate(frame)
            if (colR != null) obj.material.baseColor[0] = colR
            if (colG != null) obj.material.baseColor[1] = colG
            if (colB != null) obj.material.baseColor[2] = colB

            val emiss = findTrack(obj.id, AnimatedProperty.EMISSION_INTENSITY)?.evaluate(frame)
            if (emiss != null) obj.material.emissionIntensity = emiss

            val lightIntens = findTrack(obj.id, AnimatedProperty.LIGHT_INTENSITY)?.evaluate(frame)
            if (lightIntens != null && obj.lightData != null) obj.lightData!!.intensity = lightIntens
        }
    }
}
