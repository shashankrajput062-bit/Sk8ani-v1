package com.example.engine.mesh

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.*

enum class PrimitiveType {
    CUBE,
    SPHERE,
    CYLINDER,
    CONE,
    TORUS,
    PLANE,
    CAPSULE,
    CUSTOM
}

class MeshData(
    val vertices: FloatArray, // x, y, z, nx, ny, nz, u, v (stride = 8 floats = 32 bytes)
    val indices: ShortArray,
    val primitiveType: PrimitiveType = PrimitiveType.CUSTOM
) {
    val vertexCount: Int = vertices.size / 8
    val indexCount: Int = indices.size

    val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    val indexBuffer: ShortBuffer = ByteBuffer.allocateDirect(indices.size * 2)
        .order(ByteOrder.nativeOrder())
        .asShortBuffer()
        .apply {
            put(indices)
            position(0)
        }

    companion object {
        const val STRIDE = 8 * 4 // 8 floats * 4 bytes = 32 bytes
        const val POS_OFFSET = 0
        const val NORMAL_OFFSET = 3 * 4
        const val UV_OFFSET = 6 * 4

        fun createCube(size: Float = 1f): MeshData {
            val h = size / 2f
            // 24 vertices for 6 distinct faces (flat normals)
            val v = floatArrayOf(
                // Front face (+Z)
                -h, -h,  h,   0f, 0f, 1f,   0f, 0f,
                 h, -h,  h,   0f, 0f, 1f,   1f, 0f,
                 h,  h,  h,   0f, 0f, 1f,   1f, 1f,
                -h,  h,  h,   0f, 0f, 1f,   0f, 1f,
                // Back face (-Z)
                 h, -h, -h,   0f, 0f, -1f,  0f, 0f,
                -h, -h, -h,   0f, 0f, -1f,  1f, 0f,
                -h,  h, -h,   0f, 0f, -1f,  1f, 1f,
                 h,  h, -h,   0f, 0f, -1f,  0f, 1f,
                // Top face (+Y)
                -h,  h,  h,   0f, 1f, 0f,   0f, 0f,
                 h,  h,  h,   0f, 1f, 0f,   1f, 0f,
                 h,  h, -h,   0f, 1f, 0f,   1f, 1f,
                -h,  h, -h,   0f, 1f, 0f,   0f, 1f,
                // Bottom face (-Y)
                -h, -h, -h,   0f, -1f, 0f,  0f, 0f,
                 h, -h, -h,   0f, -1f, 0f,  1f, 0f,
                 h, -h,  h,   0f, -1f, 0f,  1f, 1f,
                -h, -h,  h,   0f, -1f, 0f,  0f, 1f,
                // Right face (+X)
                 h, -h,  h,   1f, 0f, 0f,   0f, 0f,
                 h, -h, -h,   1f, 0f, 0f,   1f, 0f,
                 h,  h, -h,   1f, 0f, 0f,   1f, 1f,
                 h,  h,  h,   1f, 0f, 0f,   0f, 1f,
                // Left face (-X)
                -h, -h, -h,  -1f, 0f, 0f,   0f, 0f,
                -h, -h,  h,  -1f, 0f, 0f,   1f, 0f,
                -h,  h,  h,  -1f, 0f, 0f,   1f, 1f,
                -h,  h, -h,  -1f, 0f, 0f,   0f, 1f
            )

            val indices = ShortArray(36)
            var idx = 0
            for (f in 0 until 6) {
                val base = (f * 4).toShort()
                indices[idx++] = base
                indices[idx++] = (base + 1).toShort()
                indices[idx++] = (base + 2).toShort()
                indices[idx++] = base
                indices[idx++] = (base + 2).toShort()
                indices[idx++] = (base + 3).toShort()
            }

            return MeshData(v, indices, PrimitiveType.CUBE)
        }

        fun createPlane(width: Float = 2f, depth: Float = 2f): MeshData {
            val hw = width / 2f
            val hd = depth / 2f
            val v = floatArrayOf(
                -hw, 0f,  hd,   0f, 1f, 0f,   0f, 0f,
                 hw, 0f,  hd,   0f, 1f, 0f,   1f, 0f,
                 hw, 0f, -hd,   0f, 1f, 0f,   1f, 1f,
                -hw, 0f, -hd,   0f, 1f, 0f,   0f, 1f
            )
            val indices = shortArrayOf(0, 1, 2, 0, 2, 3)
            return MeshData(v, indices, PrimitiveType.PLANE)
        }

        fun createSphere(radius: Float = 0.6f, rings: Int = 18, sectors: Int = 24): MeshData {
            val vertexList = mutableListOf<Float>()
            val indexList = mutableListOf<Short>()

            for (r in 0..rings) {
                val phi = (r.toFloat() / rings) * Math.PI.toFloat()
                val y = cos(phi) * radius
                val ringR = sin(phi) * radius

                for (s in 0..sectors) {
                    val theta = (s.toFloat() / sectors) * 2f * Math.PI.toFloat()
                    val x = ringR * cos(theta)
                    val z = ringR * sin(theta)

                    val nx = x / radius
                    val ny = y / radius
                    val nz = z / radius
                    val u = s.toFloat() / sectors
                    val v = r.toFloat() / rings

                    vertexList.addAll(listOf(x, y, z, nx, ny, nz, u, v))
                }
            }

            for (r in 0 until rings) {
                for (s in 0 until sectors) {
                    val first = ((r * (sectors + 1)) + s).toShort()
                    val second = (first + sectors + 1).toShort()

                    indexList.add(first)
                    indexList.add(second)
                    indexList.add((first + 1).toShort())

                    indexList.add(second)
                    indexList.add((second + 1).toShort())
                    indexList.add((first + 1).toShort())
                }
            }

            return MeshData(vertexList.toFloatArray(), indexList.toShortArray(), PrimitiveType.SPHERE)
        }

        fun createCylinder(radius: Float = 0.5f, height: Float = 1.2f, segments: Int = 20): MeshData {
            val vertexList = mutableListOf<Float>()
            val indexList = mutableListOf<Short>()
            val hh = height / 2f

            // Side vertices
            for (i in 0..segments) {
                val theta = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val x = cos(theta) * radius
                val z = sin(theta) * radius
                val nx = cos(theta)
                val nz = sin(theta)
                val u = i.toFloat() / segments

                // Top vertex
                vertexList.addAll(listOf(x, hh, z, nx, 0f, nz, u, 1f))
                // Bottom vertex
                vertexList.addAll(listOf(x, -hh, z, nx, 0f, nz, u, 0f))
            }

            for (i in 0 until segments) {
                val base = (i * 2).toShort()
                indexList.add(base)
                indexList.add((base + 1).toShort())
                indexList.add((base + 2).toShort())

                indexList.add((base + 1).toShort())
                indexList.add((base + 3).toShort())
                indexList.add((base + 2).toShort())
            }

            // Top Cap center
            val topCenterIdx = (vertexList.size / 8).toShort()
            vertexList.addAll(listOf(0f, hh, 0f, 0f, 1f, 0f, 0.5f, 0.5f))
            val topRingStart = (vertexList.size / 8).toShort()
            for (i in 0..segments) {
                val theta = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val x = cos(theta) * radius
                val z = sin(theta) * radius
                vertexList.addAll(listOf(x, hh, z, 0f, 1f, 0f, (cos(theta) + 1) * 0.5f, (sin(theta) + 1) * 0.5f))
            }
            for (i in 0 until segments) {
                indexList.add(topCenterIdx)
                indexList.add((topRingStart + i).toShort())
                indexList.add((topRingStart + i + 1).toShort())
            }

            // Bottom Cap center
            val botCenterIdx = (vertexList.size / 8).toShort()
            vertexList.addAll(listOf(0f, -hh, 0f, 0f, -1f, 0f, 0.5f, 0.5f))
            val botRingStart = (vertexList.size / 8).toShort()
            for (i in 0..segments) {
                val theta = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val x = cos(theta) * radius
                val z = sin(theta) * radius
                vertexList.addAll(listOf(x, -hh, z, 0f, -1f, 0f, (cos(theta) + 1) * 0.5f, (sin(theta) + 1) * 0.5f))
            }
            for (i in 0 until segments) {
                indexList.add(botCenterIdx)
                indexList.add((botRingStart + i + 1).toShort())
                indexList.add((botRingStart + i).toShort())
            }

            return MeshData(vertexList.toFloatArray(), indexList.toShortArray(), PrimitiveType.CYLINDER)
        }

        fun createCone(radius: Float = 0.6f, height: Float = 1.2f, segments: Int = 20): MeshData {
            val vertexList = mutableListOf<Float>()
            val indexList = mutableListOf<Short>()
            val hh = height / 2f

            val apexIdx = 0.toShort()
            vertexList.addAll(listOf(0f, hh, 0f, 0f, 1f, 0f, 0.5f, 1f))

            val sideStart = 1.toShort()
            for (i in 0..segments) {
                val theta = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val x = cos(theta) * radius
                val z = sin(theta) * radius
                val nx = cos(theta)
                val nz = sin(theta)
                vertexList.addAll(listOf(x, -hh, z, nx, 0.3f, nz, i.toFloat() / segments, 0f))
            }

            for (i in 0 until segments) {
                indexList.add(apexIdx)
                indexList.add((sideStart + i).toShort())
                indexList.add((sideStart + i + 1).toShort())
            }

            // Bottom cap
            val botCenterIdx = (vertexList.size / 8).toShort()
            vertexList.addAll(listOf(0f, -hh, 0f, 0f, -1f, 0f, 0.5f, 0.5f))
            val botRingStart = (vertexList.size / 8).toShort()
            for (i in 0..segments) {
                val theta = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
                val x = cos(theta) * radius
                val z = sin(theta) * radius
                vertexList.addAll(listOf(x, -hh, z, 0f, -1f, 0f, (cos(theta) + 1) * 0.5f, (sin(theta) + 1) * 0.5f))
            }
            for (i in 0 until segments) {
                indexList.add(botCenterIdx)
                indexList.add((botRingStart + i + 1).toShort())
                indexList.add((botRingStart + i).toShort())
            }

            return MeshData(vertexList.toFloatArray(), indexList.toShortArray(), PrimitiveType.CONE)
        }

        fun createTorus(mainRadius: Float = 0.6f, tubeRadius: Float = 0.2f, radialSegments: Int = 20, tubularSegments: Int = 16): MeshData {
            val vertexList = mutableListOf<Float>()
            val indexList = mutableListOf<Short>()

            for (j in 0..radialSegments) {
                for (i in 0..tubularSegments) {
                    val u = (i.toFloat() / tubularSegments) * 2f * Math.PI.toFloat()
                    val v = (j.toFloat() / radialSegments) * 2f * Math.PI.toFloat()

                    val x = (mainRadius + tubeRadius * cos(v)) * cos(u)
                    val y = tubeRadius * sin(v)
                    val z = (mainRadius + tubeRadius * cos(v)) * sin(u)

                    val nx = cos(v) * cos(u)
                    val ny = sin(v)
                    val nz = cos(v) * sin(u)

                    vertexList.addAll(listOf(x, y, z, nx, ny, nz, i.toFloat() / tubularSegments, j.toFloat() / radialSegments))
                }
            }

            for (j in 0 until radialSegments) {
                for (i in 0 until tubularSegments) {
                    val a = (j * (tubularSegments + 1) + i).toShort()
                    val b = ((j + 1) * (tubularSegments + 1) + i).toShort()
                    val c = ((j + 1) * (tubularSegments + 1) + i + 1).toShort()
                    val d = (j * (tubularSegments + 1) + i + 1).toShort()

                    indexList.add(a)
                    indexList.add(b)
                    indexList.add(d)

                    indexList.add(b)
                    indexList.add(c)
                    indexList.add(d)
                }
            }

            return MeshData(vertexList.toFloatArray(), indexList.toShortArray(), PrimitiveType.TORUS)
        }

        fun createCapsule(radius: Float = 0.4f, cylinderHeight: Float = 0.8f, rings: Int = 10, sectors: Int = 16): MeshData {
            val vertexList = mutableListOf<Float>()
            val indexList = mutableListOf<Short>()
            val halfH = cylinderHeight / 2f

            // Top Dome
            for (r in 0..rings) {
                val phi = (r.toFloat() / rings) * (Math.PI.toFloat() / 2f)
                val y = cos(phi) * radius + halfH
                val ringR = sin(phi) * radius

                for (s in 0..sectors) {
                    val theta = (s.toFloat() / sectors) * 2f * Math.PI.toFloat()
                    val x = ringR * cos(theta)
                    val z = ringR * sin(theta)

                    val nx = x / radius
                    val ny = (y - halfH) / radius
                    val nz = z / radius

                    vertexList.addAll(listOf(x, y, z, nx, ny, nz, s.toFloat() / sectors, (y + halfH + radius) / (cylinderHeight + 2 * radius)))
                }
            }

            // Bottom Dome
            for (r in 0..rings) {
                val phi = (Math.PI.toFloat() / 2f) + (r.toFloat() / rings) * (Math.PI.toFloat() / 2f)
                val y = cos(phi) * radius - halfH
                val ringR = sin(phi) * radius

                for (s in 0..sectors) {
                    val theta = (s.toFloat() / sectors) * 2f * Math.PI.toFloat()
                    val x = ringR * cos(theta)
                    val z = ringR * sin(theta)

                    val nx = x / radius
                    val ny = (y + halfH) / radius
                    val nz = z / radius

                    vertexList.addAll(listOf(x, y, z, nx, ny, nz, s.toFloat() / sectors, (y + halfH + radius) / (cylinderHeight + 2 * radius)))
                }
            }

            val totalRings = rings * 2 + 1
            for (r in 0 until totalRings) {
                for (s in 0 until sectors) {
                    val first = ((r * (sectors + 1)) + s).toShort()
                    val second = (first + sectors + 1).toShort()

                    indexList.add(first)
                    indexList.add(second)
                    indexList.add((first + 1).toShort())

                    indexList.add(second)
                    indexList.add((second + 1).toShort())
                    indexList.add((first + 1).toShort())
                }
            }

            return MeshData(vertexList.toFloatArray(), indexList.toShortArray(), PrimitiveType.CAPSULE)
        }
    }
}
