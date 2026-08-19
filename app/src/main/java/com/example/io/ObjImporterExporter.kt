package com.example.io

import com.example.engine.mesh.MeshData
import com.example.engine.mesh.PrimitiveType
import com.example.scene.Scene
import com.example.scene.SceneObject
import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter

object ObjImporterExporter {

    /**
     * Parse standard Wavefront OBJ content into a MeshData object
     */
    fun parseObj(objContent: String): MeshData? {
        try {
            val positions = mutableListOf<Float>()
            val normals = mutableListOf<Float>()
            val uvs = mutableListOf<Float>()

            val outVertices = mutableListOf<Float>()
            val outIndices = mutableListOf<Short>()
            val vertexMap = mutableMapOf<String, Short>()

            val reader = BufferedReader(StringReader(objContent))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                val parts = trimmed.split("\\s+".toRegex())
                when (parts[0]) {
                    "v" -> {
                        if (parts.size >= 4) {
                            positions.add(parts[1].toFloat())
                            positions.add(parts[2].toFloat())
                            positions.add(parts[3].toFloat())
                        }
                    }
                    "vn" -> {
                        if (parts.size >= 4) {
                            normals.add(parts[1].toFloat())
                            normals.add(parts[2].toFloat())
                            normals.add(parts[3].toFloat())
                        }
                    }
                    "vt" -> {
                        if (parts.size >= 3) {
                            uvs.add(parts[1].toFloat())
                            uvs.add(parts[2].toFloat())
                        }
                    }
                    "f" -> {
                        if (parts.size >= 4) {
                            // Triangle or Polygon face
                            val faceIndices = mutableListOf<Short>()
                            for (i in 1 until parts.size) {
                                val vertexToken = parts[i]
                                var index = vertexMap[vertexToken]
                                if (index == null) {
                                    val subParts = vertexToken.split("/")
                                    val vIdx = (subParts[0].toInt() - 1).coerceAtLeast(0)
                                    val vtIdx = if (subParts.size > 1 && subParts[1].isNotEmpty()) (subParts[1].toInt() - 1).coerceAtLeast(0) else -1
                                    val vnIdx = if (subParts.size > 2 && subParts[2].isNotEmpty()) (subParts[2].toInt() - 1).coerceAtLeast(0) else -1

                                    val px = if (vIdx * 3 + 2 < positions.size) positions[vIdx * 3] else 0f
                                    val py = if (vIdx * 3 + 2 < positions.size) positions[vIdx * 3 + 1] else 0f
                                    val pz = if (vIdx * 3 + 2 < positions.size) positions[vIdx * 3 + 2] else 0f

                                    val nx = if (vnIdx >= 0 && vnIdx * 3 + 2 < normals.size) normals[vnIdx * 3] else 0f
                                    val ny = if (vnIdx >= 0 && vnIdx * 3 + 2 < normals.size) normals[vnIdx * 3 + 1] else 1f
                                    val nz = if (vnIdx >= 0 && vnIdx * 3 + 2 < normals.size) normals[vnIdx * 3 + 2] else 0f

                                    val u = if (vtIdx >= 0 && vtIdx * 2 + 1 < uvs.size) uvs[vtIdx * 2] else 0f
                                    val v = if (vtIdx >= 0 && vtIdx * 2 + 1 < uvs.size) uvs[vtIdx * 2 + 1] else 0f

                                    index = (outVertices.size / 8).toShort()
                                    outVertices.addAll(listOf(px, py, pz, nx, ny, nz, u, v))
                                    vertexMap[vertexToken] = index
                                }
                                faceIndices.add(index)
                            }

                            // Fan triangulation if quad or n-gon
                            for (t in 1 until faceIndices.size - 1) {
                                outIndices.add(faceIndices[0])
                                outIndices.add(faceIndices[t])
                                outIndices.add(faceIndices[t + 1])
                            }
                        }
                    }
                }
            }

            if (outVertices.isEmpty()) return null
            return MeshData(outVertices.toFloatArray(), outIndices.toShortArray(), PrimitiveType.CUSTOM)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Export all mesh objects in the scene to standard Wavefront OBJ
     */
    fun exportSceneToObj(scene: Scene): String {
        val writer = StringWriter()
        writer.appendLine("# Sk8ani 3D Animation Studio OBJ Export")
        writer.appendLine("# Objects: ${scene.objects.size}")

        var vertexOffset = 1

        for (obj in scene.objects) {
            if (obj.type != com.example.scene.ObjectType.MESH) continue

            writer.appendLine("o ${obj.name.replace(" ", "_")}")
            
            // Get mesh representation
            val mesh = when (obj.primitiveType) {
                PrimitiveType.CUBE -> MeshData.createCube()
                PrimitiveType.SPHERE -> MeshData.createSphere()
                PrimitiveType.CYLINDER -> MeshData.createCylinder()
                PrimitiveType.CONE -> MeshData.createCone()
                PrimitiveType.TORUS -> MeshData.createTorus()
                PrimitiveType.PLANE -> MeshData.createPlane()
                PrimitiveType.CAPSULE -> MeshData.createCapsule()
                else -> MeshData.createCube()
            }

            // Write vertices with applied transform
            val v = mesh.vertices
            for (i in 0 until mesh.vertexCount) {
                val idx = i * 8
                val lx = v[idx] * obj.transform.scale.x
                val ly = v[idx + 1] * obj.transform.scale.y
                val lz = v[idx + 2] * obj.transform.scale.z

                val wx = lx + obj.transform.position.x
                val wy = ly + obj.transform.position.y
                val wz = lz + obj.transform.position.z

                writer.appendLine(String.format(java.util.Locale.US, "v %.4f %.4f %.4f", wx, wy, wz))
            }

            // Write normals
            for (i in 0 until mesh.vertexCount) {
                val idx = i * 8
                val nx = v[idx + 3]
                val ny = v[idx + 4]
                val nz = v[idx + 5]
                writer.appendLine(String.format(java.util.Locale.US, "vn %.4f %.4f %.4f", nx, ny, nz))
            }

            // Write UVs
            for (i in 0 until mesh.vertexCount) {
                val idx = i * 8
                val u = v[idx + 6]
                val vCoord = v[idx + 7]
                writer.appendLine(String.format(java.util.Locale.US, "vt %.4f %.4f", u, vCoord))
            }

            // Write faces
            val ind = mesh.indices
            for (i in 0 until ind.size step 3) {
                val i0 = ind[i] + vertexOffset
                val i1 = ind[i + 1] + vertexOffset
                val i2 = ind[i + 2] + vertexOffset
                writer.appendLine("f $i0/$i0/$i0 $i1/$i1/$i1 $i2/$i2/$i2")
            }

            vertexOffset += mesh.vertexCount
        }

        return writer.toString()
    }
}
