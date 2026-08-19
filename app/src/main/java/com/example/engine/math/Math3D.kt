package com.example.engine.math

import android.opengl.Matrix
import kotlin.math.*

data class Vector3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float) {
        x = nx
        y = ny
        z = nz
    }

    fun copy(): Vector3 = Vector3(x, y, z)

    operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = if (scalar != 0f) Vector3(x / scalar, y / scalar, z / scalar) else Vector3()

    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun lengthSquared(): Float = x * x + y * y + z * z

    fun normalize(): Vector3 {
        val len = length()
        return if (len > 0.00001f) Vector3(x / len, y / len, z / len) else Vector3(0f, 0f, 0f)
    }

    fun dot(other: Vector3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vector3): Vector3 = Vector3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun distanceTo(other: Vector3): Float = (this - other).length()

    fun toFloatArray(): FloatArray = floatArrayOf(x, y, z)

    companion object {
        val Zero get() = Vector3(0f, 0f, 0f)
        val One get() = Vector3(1f, 1f, 1f)
        val Up get() = Vector3(0f, 1f, 0f)
        val Forward get() = Vector3(0f, 0f, -1f)
        val Right get() = Vector3(1f, 0f, 0f)

        fun lerp(a: Vector3, b: Vector3, t: Float): Vector3 {
            val clamped = t.coerceIn(0f, 1f)
            return Vector3(
                a.x + (b.x - a.x) * clamped,
                a.y + (b.y - a.y) * clamped,
                a.z + (b.z - a.z) * clamped
            )
        }
    }
}

data class Ray(val origin: Vector3, val direction: Vector3) {
    fun getPoint(distance: Float): Vector3 = origin + direction * distance

    /**
     * Test intersection with Axis-Aligned Bounding Box (AABB)
     */
    fun intersectAABB(min: Vector3, max: Vector3): Float? {
        var tmin = (min.x - origin.x) / if (direction.x != 0f) direction.x else 0.000001f
        var tmax = (max.x - origin.x) / if (direction.x != 0f) direction.x else 0.000001f
        if (tmin > tmax) {
            val tmp = tmin; tmin = tmax; tmax = tmp
        }

        var tymin = (min.y - origin.y) / if (direction.y != 0f) direction.y else 0.000001f
        var tymax = (max.y - origin.y) / if (direction.y != 0f) direction.y else 0.000001f
        if (tymin > tymax) {
            val tmp = tymin; tymin = tymax; tymax = tmp
        }

        if (tmin > tymax || tymin > tmax) return null
        if (tymin > tmin) tmin = tymin
        if (tymax < tmax) tmax = tymax

        var tzmin = (min.z - origin.z) / if (direction.z != 0f) direction.z else 0.000001f
        var tzmax = (max.z - origin.z) / if (direction.z != 0f) direction.z else 0.000001f
        if (tzmin > tzmax) {
            val tmp = tzmin; tzmin = tzmax; tzmax = tmp
        }

        if (tmin > tzmax || tzmin > tmax) return null
        if (tzmin > tmin) tmin = tzmin
        if (tzmax < tmax) tmax = tzmax

        return if (tmax < 0f) null else max(0f, tmin)
    }

    /**
     * Test intersection with Bounding Sphere
     */
    fun intersectSphere(center: Vector3, radius: Float): Float? {
        val oc = origin - center
        val a = direction.dot(direction)
        val b = 2f * oc.dot(direction)
        val c = oc.dot(oc) - radius * radius
        val discriminant = b * b - 4 * a * c
        if (discriminant < 0) return null
        val t0 = (-b - sqrt(discriminant)) / (2f * a)
        if (t0 >= 0) return t0
        val t1 = (-b + sqrt(discriminant)) / (2f * a)
        return if (t1 >= 0) t1 else null
    }

    /**
     * Intersect with an infinite plane (e.g. ground plane Y = 0 or custom normal and point)
     */
    fun intersectPlane(planeNormal: Vector3, planePoint: Vector3): Float? {
        val denom = planeNormal.dot(direction)
        if (abs(denom) > 0.0001f) {
            val t = (planePoint - origin).dot(planeNormal) / denom
            if (t >= 0) return t
        }
        return null
    }
}

data class BoundingBox(val min: Vector3, val max: Vector3) {
    val center: Vector3 get() = (min + max) * 0.5f
    val size: Vector3 get() = max - min
    val radius: Float get() = size.length() * 0.5f
}
