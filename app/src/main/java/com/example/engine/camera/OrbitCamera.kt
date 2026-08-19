package com.example.engine.camera

import android.opengl.Matrix
import com.example.engine.math.Ray
import com.example.engine.math.Vector3
import kotlin.math.*

class OrbitCamera {
    var target: Vector3 = Vector3(0f, 0.5f, 0f)
    var distance: Float = 6.0f
    var yaw: Float = 45.0f       // Degrees
    var pitch: Float = 25.0f     // Degrees

    var fov: Float = 45.0f
    var near: Float = 0.1f
    var far: Float = 100.0f
    var aspect: Float = 1.0f

    val eye: Vector3 = Vector3()
    val up: Vector3 = Vector3(0f, 1f, 0f)

    val viewMatrix = FloatArray(16)
    val projMatrix = FloatArray(16)
    val viewProjMatrix = FloatArray(16)
    val invViewProjMatrix = FloatArray(16)

    init {
        updateMatrices()
    }

    fun setViewportSize(width: Int, height: Int) {
        if (height > 0) {
            aspect = width.toFloat() / height.toFloat()
            updateMatrices()
        }
    }

    fun orbit(deltaYaw: Float, deltaPitch: Float) {
        yaw = (yaw + deltaYaw) % 360f
        pitch = (pitch + deltaPitch).coerceIn(-89f, 89f)
        updateMatrices()
    }

    fun pan(deltaX: Float, deltaY: Float) {
        // Calculate camera right and up in world space
        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()
        val forwardX = -sin(radYaw)
        val forwardZ = -cos(radYaw)
        val rightX = cos(radYaw)
        val rightZ = -sin(radYaw)

        val factor = distance * 0.0015f
        target.x += (-rightX * deltaX) * factor
        target.z += (-rightZ * deltaX) * factor
        target.y += (deltaY) * factor
        updateMatrices()
    }

    fun zoom(factor: Float) {
        distance = (distance * factor).coerceIn(0.5f, 50.0f)
        updateMatrices()
    }

    fun focusOn(point: Vector3, newDistance: Float = 4.0f) {
        target = point.copy()
        distance = newDistance.coerceIn(1.0f, 30.0f)
        updateMatrices()
    }

    fun setPresetView(preset: CameraPreset) {
        when (preset) {
            CameraPreset.PERSPECTIVE -> {
                yaw = 45f
                pitch = 25f
                distance = 6f
                target.set(0f, 0.5f, 0f)
            }
            CameraPreset.TOP -> {
                yaw = 0f
                pitch = 89f
                distance = 8f
                target.set(0f, 0f, 0f)
            }
            CameraPreset.FRONT -> {
                yaw = 0f
                pitch = 0f
                distance = 6f
                target.set(0f, 0.5f, 0f)
            }
            CameraPreset.RIGHT -> {
                yaw = 90f
                pitch = 0f
                distance = 6f
                target.set(0f, 0.5f, 0f)
            }
        }
        updateMatrices()
    }

    fun updateMatrices() {
        val radYaw = Math.toRadians(yaw.toDouble()).toFloat()
        val radPitch = Math.toRadians(pitch.toDouble()).toFloat()

        eye.x = target.x + distance * cos(radPitch) * sin(radYaw)
        eye.y = target.y + distance * sin(radPitch)
        eye.z = target.z + distance * cos(radPitch) * cos(radYaw)

        Matrix.setLookAtM(
            viewMatrix, 0,
            eye.x, eye.y, eye.z,
            target.x, target.y, target.z,
            up.x, up.y, up.z
        )

        Matrix.perspectiveM(projMatrix, 0, fov, aspect, near, far)
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)
        Matrix.invertM(invViewProjMatrix, 0, viewProjMatrix, 0)
    }

    /**
     * Convert screen touch coordinate (0..viewWidth, 0..viewHeight) into a 3D Ray
     */
    fun screenToRay(touchX: Float, touchY: Float, viewWidth: Int, viewHeight: Int): Ray {
        // Normalized Device Coordinates (-1 to 1)
        val ndcX = (2f * touchX / viewWidth) - 1f
        val ndcY = 1f - (2f * touchY / viewHeight)

        val nearPoint = floatArrayOf(ndcX, ndcY, -1f, 1f)
        val farPoint = floatArrayOf(ndcX, ndcY, 1f, 1f)

        val nearWorld = FloatArray(4)
        val farWorld = FloatArray(4)

        Matrix.multiplyMV(nearWorld, 0, invViewProjMatrix, 0, nearPoint, 0)
        Matrix.multiplyMV(farWorld, 0, invViewProjMatrix, 0, farPoint, 0)

        val nearPos = Vector3(
            nearWorld[0] / nearWorld[3],
            nearWorld[1] / nearWorld[3],
            nearWorld[2] / nearWorld[3]
        )

        val farPos = Vector3(
            farWorld[0] / farWorld[3],
            farWorld[1] / farWorld[3],
            farWorld[2] / farWorld[3]
        )

        val dir = (farPos - nearPos).normalize()
        return Ray(nearPos, dir)
    }
}

enum class CameraPreset {
    PERSPECTIVE,
    TOP,
    FRONT,
    RIGHT
}
