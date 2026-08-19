package com.example.physics

import com.example.animation.AnimationTimeline
import com.example.animation.InterpolationType
import com.example.engine.math.Vector3
import com.example.scene.ObjectType
import com.example.scene.Scene
import com.example.scene.SceneObject
import kotlin.math.max

class PhysicsEngine {
    var gravity: Vector3 = Vector3(0f, -9.81f, 0f)
    var isSimulating: Boolean = false

    private val initialTransforms = mutableMapOf<String, com.example.scene.Transform>()

    fun startSimulation(scene: Scene) {
        initialTransforms.clear()
        for (obj in scene.objects) {
            if (obj.physicsData.enabled) {
                initialTransforms[obj.id] = obj.transform.copy()
            }
        }
        isSimulating = true
    }

    fun stopSimulation(scene: Scene, resetPositions: Boolean = true) {
        isSimulating = false
        if (resetPositions) {
            for (obj in scene.objects) {
                initialTransforms[obj.id]?.let { original ->
                    obj.transform = original.copy()
                    obj.physicsData.velocity.set(0f, 0f, 0f)
                    obj.physicsData.angularVelocity.set(0f, 0f, 0f)
                }
            }
        }
    }

    fun step(scene: Scene, deltaTime: Float = 1f / 60f) {
        if (!isSimulating) return

        val dynamicObjects = scene.objects.filter { 
            it.type == ObjectType.MESH && it.physicsData.enabled && it.physicsData.isDynamic 
        }
        val staticObjects = scene.objects.filter { 
            it.type == ObjectType.MESH && it.physicsData.enabled && !it.physicsData.isDynamic 
        }

        for (obj in dynamicObjects) {
            val pData = obj.physicsData

            // Apply gravity
            pData.velocity = pData.velocity + (gravity * deltaTime)

            // Integrate position
            obj.transform.position = obj.transform.position + (pData.velocity * deltaTime)

            // Floor Plane Collision (Y = 0)
            val halfHeight = max(0.1f, obj.transform.scale.y * 0.5f)
            val floorY = 0f + halfHeight

            if (obj.transform.position.y <= floorY) {
                obj.transform.position.y = floorY
                if (pData.velocity.y < 0f) {
                    pData.velocity.y = -pData.velocity.y * pData.restitution
                    // Apply floor friction
                    pData.velocity.x *= (1f - pData.friction * 0.1f)
                    pData.velocity.z *= (1f - pData.friction * 0.1f)

                    // Angular wobble damp
                    obj.transform.rotation.x *= 0.95f
                    obj.transform.rotation.z *= 0.95f
                }
            }

            // Static Obstacle Collision (simplified box test)
            for (staticObj in staticObjects) {
                val dx = obj.transform.position.x - staticObj.transform.position.x
                val dy = obj.transform.position.y - staticObj.transform.position.y
                val dz = obj.transform.position.z - staticObj.transform.position.z

                val boundX = (obj.transform.scale.x + staticObj.transform.scale.x) * 0.5f
                val boundY = (obj.transform.scale.y + staticObj.transform.scale.y) * 0.5f
                val boundZ = (obj.transform.scale.z + staticObj.transform.scale.z) * 0.5f

                if (kotlin.math.abs(dx) < boundX && kotlin.math.abs(dy) < boundY && kotlin.math.abs(dz) < boundZ) {
                    // Push out along collision normal
                    if (dy > 0 && pData.velocity.y < 0) {
                        obj.transform.position.y = staticObj.transform.position.y + boundY
                        pData.velocity.y = -pData.velocity.y * pData.restitution
                    }
                }
            }
        }

        // Object-to-Object Collisions (Dynamic vs Dynamic)
        for (i in 0 until dynamicObjects.size) {
            for (j in i + 1 until dynamicObjects.size) {
                val a = dynamicObjects[i]
                val b = dynamicObjects[j]

                val diff = a.transform.position - b.transform.position
                val dist = diff.length()
                val minRadius = (a.transform.scale.length() + b.transform.scale.length()) * 0.25f

                if (dist < minRadius && dist > 0.0001f) {
                    val normal = diff.normalize()
                    val overlap = minRadius - dist

                    // Separate bodies
                    a.transform.position = a.transform.position + (normal * (overlap * 0.5f))
                    b.transform.position = b.transform.position - (normal * (overlap * 0.5f))

                    // Elastic velocity exchange
                    val vRel = a.physicsData.velocity - b.physicsData.velocity
                    val impulse = vRel.dot(normal) * (1f + (a.physicsData.restitution + b.physicsData.restitution) * 0.5f)

                    if (impulse < 0) {
                        a.physicsData.velocity = a.physicsData.velocity - (normal * (impulse * 0.5f))
                        b.physicsData.velocity = b.physicsData.velocity + (normal * (impulse * 0.5f))
                    }
                }
            }
        }
    }

    /**
     * Bakes the physics simulation over the timeline range into Keyframes on the AnimationTimeline.
     */
    fun bakePhysicsToKeyframes(scene: Scene, timeline: AnimationTimeline) {
        startSimulation(scene)
        val originalFrame = timeline.currentFrame

        for (frame in timeline.startFrame..timeline.endFrame) {
            step(scene, 1f / timeline.fps.toFloat())

            for (obj in scene.objects) {
                if (obj.physicsData.enabled && obj.physicsData.isDynamic) {
                    timeline.insertTransformKeyframe(obj, frame, InterpolationType.LINEAR)
                }
            }
        }

        stopSimulation(scene, resetPositions = false)
        timeline.currentFrame = originalFrame
    }
}
