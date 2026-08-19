package com.example.project

import android.content.Context
import com.example.animation.*
import com.example.engine.math.Vector3
import com.example.engine.mesh.PrimitiveType
import com.example.scene.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ProjectData(
    val scene: Scene,
    val timeline: AnimationTimeline
)

object ProjectManager {

    private const val AUTOSAVE_FILENAME = "sk8ani_autosave.json"

    fun serializeProject(scene: Scene, timeline: AnimationTimeline): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("name", scene.name)

        // Environment
        val envJson = JSONObject()
        envJson.put("viewportTheme", scene.environment.viewportTheme.name)
        envJson.put("ambientIntensity", scene.environment.ambientIntensity.toDouble())
        envJson.put("renderPreset", scene.environment.renderPreset.name)
        envJson.put("bloomEnabled", scene.environment.bloomEnabled)
        envJson.put("shadowEnabled", scene.environment.shadowEnabled)
        root.put("environment", envJson)

        // Objects
        val objectsArray = JSONArray()
        for (obj in scene.objects) {
            val objJson = JSONObject()
            objJson.put("id", obj.id)
            objJson.put("name", obj.name)
            objJson.put("type", obj.type.name)
            objJson.put("primitiveType", obj.primitiveType.name)
            objJson.put("isVisible", obj.isVisible)
            objJson.put("isLocked", obj.isLocked)

            // Transform
            val tJson = JSONObject()
            tJson.put("px", obj.transform.position.x.toDouble())
            tJson.put("py", obj.transform.position.y.toDouble())
            tJson.put("pz", obj.transform.position.z.toDouble())
            tJson.put("rx", obj.transform.rotation.x.toDouble())
            tJson.put("ry", obj.transform.rotation.y.toDouble())
            tJson.put("rz", obj.transform.rotation.z.toDouble())
            tJson.put("sx", obj.transform.scale.x.toDouble())
            tJson.put("sy", obj.transform.scale.y.toDouble())
            tJson.put("sz", obj.transform.scale.z.toDouble())
            objJson.put("transform", tJson)

            // Material
            val matJson = JSONObject()
            val colArray = JSONArray()
            for (c in obj.material.baseColor) colArray.put(c.toDouble())
            matJson.put("baseColor", colArray)
            matJson.put("metallic", obj.material.metallic.toDouble())
            matJson.put("roughness", obj.material.roughness.toDouble())
            matJson.put("specular", obj.material.specular.toDouble())
            matJson.put("emissionIntensity", obj.material.emissionIntensity.toDouble())
            matJson.put("opacity", obj.material.opacity.toDouble())
            matJson.put("isWireframe", obj.material.isWireframe)
            objJson.put("material", matJson)

            // Light Data
            obj.lightData?.let { l ->
                val lJson = JSONObject()
                lJson.put("type", l.type.name)
                lJson.put("intensity", l.intensity.toDouble())
                lJson.put("range", l.range.toDouble())
                val lCol = JSONArray()
                for (c in l.color) lCol.put(c.toDouble())
                lJson.put("color", lCol)
                objJson.put("lightData", lJson)
            }

            // Physics Data
            val pJson = JSONObject()
            pJson.put("enabled", obj.physicsData.enabled)
            pJson.put("isDynamic", obj.physicsData.isDynamic)
            pJson.put("mass", obj.physicsData.mass.toDouble())
            pJson.put("restitution", obj.physicsData.restitution.toDouble())
            pJson.put("friction", obj.physicsData.friction.toDouble())
            objJson.put("physicsData", pJson)

            objectsArray.put(objJson)
        }
        root.put("objects", objectsArray)

        // Timeline
        val tlJson = JSONObject()
        tlJson.put("fps", timeline.fps)
        tlJson.put("startFrame", timeline.startFrame)
        tlJson.put("endFrame", timeline.endFrame)
        tlJson.put("currentFrame", timeline.currentFrame)

        val tracksArray = JSONArray()
        for (track in timeline.tracks) {
            val trackJson = JSONObject()
            trackJson.put("objectId", track.objectId)
            trackJson.put("property", track.property.name)

            val kfArray = JSONArray()
            for (kf in track.keyframes) {
                val kfJson = JSONObject()
                kfJson.put("frame", kf.frame)
                kfJson.put("value", kf.value.toDouble())
                kfJson.put("interpolation", kf.interpolation.name)
                kfArray.put(kfJson)
            }
            trackJson.put("keyframes", kfArray)
            tracksArray.put(trackJson)
        }
        tlJson.put("tracks", tracksArray)
        root.put("timeline", tlJson)

        return root.toString(2)
    }

    fun deserializeProject(jsonString: String): ProjectData? {
        try {
            val root = JSONObject(jsonString)
            val sceneName = root.optString("name", "Imported Scene")
            val scene = Scene(sceneName)

            val envJson = root.optJSONObject("environment")
            if (envJson != null) {
                val themeStr = envJson.optString("viewportTheme", "DARK_STUDIO")
                scene.environment.viewportTheme = try {
                    ViewportTheme.valueOf(themeStr)
                } catch (e: Exception) {
                    ViewportTheme.DARK_STUDIO
                }
            }

            // Objects
            val objectsArray = root.optJSONArray("objects")
            if (objectsArray != null) {
                for (i in 0 until objectsArray.length()) {
                    val objJson = objectsArray.getJSONObject(i)
                    val id = objJson.getString("id")
                    val name = objJson.optString("name", "Object")
                    val type = ObjectType.valueOf(objJson.optString("type", "MESH"))
                    val primType = try {
                        PrimitiveType.valueOf(objJson.optString("primitiveType", "CUBE"))
                    } catch (e: Exception) {
                        PrimitiveType.CUBE
                    }

                    // Transform
                    val tJson = objJson.getJSONObject("transform")
                    val transform = Transform(
                        position = Vector3(tJson.getDouble("px").toFloat(), tJson.getDouble("py").toFloat(), tJson.getDouble("pz").toFloat()),
                        rotation = Vector3(tJson.getDouble("rx").toFloat(), tJson.getDouble("ry").toFloat(), tJson.getDouble("rz").toFloat()),
                        scale = Vector3(tJson.getDouble("sx").toFloat(), tJson.getDouble("sy").toFloat(), tJson.getDouble("sz").toFloat())
                    )

                    // Material
                    val matJson = objJson.optJSONObject("material")
                    val mat = MaterialData.defaultMaterial()
                    if (matJson != null) {
                        val colArr = matJson.optJSONArray("baseColor")
                        if (colArr != null && colArr.length() >= 4) {
                            mat.baseColor = floatArrayOf(
                                colArr.getDouble(0).toFloat(),
                                colArr.getDouble(1).toFloat(),
                                colArr.getDouble(2).toFloat(),
                                colArr.getDouble(3).toFloat()
                            )
                        }
                        mat.metallic = matJson.optDouble("metallic", 0.1).toFloat()
                        mat.roughness = matJson.optDouble("roughness", 0.4).toFloat()
                        mat.specular = matJson.optDouble("specular", 0.5).toFloat()
                        mat.emissionIntensity = matJson.optDouble("emissionIntensity", 0.0).toFloat()
                        mat.opacity = matJson.optDouble("opacity", 1.0).toFloat()
                        mat.isWireframe = matJson.optBoolean("isWireframe", false)
                    }

                    // Light
                    var lightData: LightData? = null
                    val lJson = objJson.optJSONObject("lightData")
                    if (lJson != null) {
                        lightData = LightData(
                            type = LightType.valueOf(lJson.optString("type", "POINT")),
                            intensity = lJson.optDouble("intensity", 1.0).toFloat(),
                            range = lJson.optDouble("range", 10.0).toFloat()
                        )
                    }

                    // Physics
                    val pJson = objJson.optJSONObject("physicsData")
                    val pData = PhysicsData()
                    if (pJson != null) {
                        pData.enabled = pJson.optBoolean("enabled", false)
                        pData.isDynamic = pJson.optBoolean("isDynamic", true)
                        pData.mass = pJson.optDouble("mass", 1.0).toFloat()
                        pData.restitution = pJson.optDouble("restitution", 0.6).toFloat()
                        pData.friction = pJson.optDouble("friction", 0.4).toFloat()
                    }

                    val sceneObj = SceneObject(
                        id = id,
                        name = name,
                        type = type,
                        primitiveType = primType,
                        transform = transform,
                        material = mat,
                        lightData = lightData,
                        physicsData = pData,
                        isVisible = objJson.optBoolean("isVisible", true),
                        isLocked = objJson.optBoolean("isLocked", false)
                    )
                    scene.addObject(sceneObj)
                }
            }

            // Timeline
            val tl = AnimationTimeline()
            val tlJson = root.optJSONObject("timeline")
            if (tlJson != null) {
                tl.fps = tlJson.optInt("fps", 30)
                tl.startFrame = tlJson.optInt("startFrame", 0)
                tl.endFrame = tlJson.optInt("endFrame", 120)
                tl.currentFrame = tlJson.optInt("currentFrame", 0)

                val tracksArr = tlJson.optJSONArray("tracks")
                if (tracksArr != null) {
                    for (t in 0 until tracksArr.length()) {
                        val trackJson = tracksArr.getJSONObject(t)
                        val objId = trackJson.getString("objectId")
                        val prop = AnimatedProperty.valueOf(trackJson.getString("property"))
                        val track = tl.getOrCreateTrack(objId, prop)

                        val kfArr = trackJson.optJSONArray("keyframes")
                        if (kfArr != null) {
                            for (k in 0 until kfArr.length()) {
                                val kfObj = kfArr.getJSONObject(k)
                                val frame = kfObj.getInt("frame")
                                val v = kfObj.getDouble("value").toFloat()
                                val interp = InterpolationType.valueOf(kfObj.optString("interpolation", "EASE_IN_OUT"))
                                track.addOrUpdateKeyframe(frame, v, interp)
                            }
                        }
                    }
                }
            }

            return ProjectData(scene, tl)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun saveAutoSave(context: Context, scene: Scene, timeline: AnimationTimeline) {
        try {
            val json = serializeProject(scene, timeline)
            val file = File(context.filesDir, AUTOSAVE_FILENAME)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadAutoSave(context: Context): ProjectData? {
        return try {
            val file = File(context.filesDir, AUTOSAVE_FILENAME)
            if (file.exists()) {
                deserializeProject(file.readText())
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun createKineticBounceProject(): ProjectData {
        val scene = Scene("Kinetic Orbit Demo")
        
        // Sun Light
        scene.addObject(
            SceneObject(
                id = "sun_light",
                name = "Sun",
                type = ObjectType.LIGHT,
                transform = Transform(position = Vector3(4f, 7f, 4f)),
                lightData = LightData(type = LightType.DIRECTIONAL, intensity = 1.6f)
            )
        )

        // Ground Plane
        scene.addObject(
            SceneObject(
                id = "ground",
                name = "Mirror Floor",
                type = ObjectType.MESH,
                primitiveType = PrimitiveType.PLANE,
                transform = Transform(scale = Vector3(6f, 1f, 6f)),
                material = MaterialData.chrome()
            )
        )

        // Center Gold Torus
        val torus = SceneObject(
            id = "torus_center",
            name = "Gold Ring",
            type = ObjectType.MESH,
            primitiveType = PrimitiveType.TORUS,
            transform = Transform(position = Vector3(0f, 1.2f, 0f), scale = Vector3(1.2f, 1.2f, 1.2f)),
            material = MaterialData.gold()
        )
        scene.addObject(torus)

        // Orbiting Cyan Sphere
        val orbiter = SceneObject(
            id = "orbiter_sphere",
            name = "Cyan Orb",
            type = ObjectType.MESH,
            primitiveType = PrimitiveType.SPHERE,
            transform = Transform(position = Vector3(2.0f, 1.2f, 0f), scale = Vector3(0.5f, 0.5f, 0.5f)),
            material = MaterialData.neonCyan()
        )
        scene.addObject(orbiter)

        val timeline = AnimationTimeline()
        // Animate Torus Spin
        val rotYTrack = timeline.getOrCreateTrack(torus.id, AnimatedProperty.ROTATION_Y)
        rotYTrack.addOrUpdateKeyframe(0, 0f, InterpolationType.LINEAR)
        rotYTrack.addOrUpdateKeyframe(60, 180f, InterpolationType.LINEAR)
        rotYTrack.addOrUpdateKeyframe(120, 360f, InterpolationType.LINEAR)

        // Animate Orbiter Position (Circle around Torus)
        val posXTrack = timeline.getOrCreateTrack(orbiter.id, AnimatedProperty.POSITION_X)
        val posZTrack = timeline.getOrCreateTrack(orbiter.id, AnimatedProperty.POSITION_Z)
        val posYTrack = timeline.getOrCreateTrack(orbiter.id, AnimatedProperty.POSITION_Y)

        posXTrack.addOrUpdateKeyframe(0, 2.0f, InterpolationType.EASE_IN_OUT)
        posZTrack.addOrUpdateKeyframe(0, 0.0f, InterpolationType.EASE_IN_OUT)
        posYTrack.addOrUpdateKeyframe(0, 1.2f, InterpolationType.EASE_IN_OUT)

        posXTrack.addOrUpdateKeyframe(30, 0.0f, InterpolationType.EASE_IN_OUT)
        posZTrack.addOrUpdateKeyframe(30, 2.0f, InterpolationType.EASE_IN_OUT)
        posYTrack.addOrUpdateKeyframe(30, 2.0f, InterpolationType.EASE_IN_OUT)

        posXTrack.addOrUpdateKeyframe(60, -2.0f, InterpolationType.EASE_IN_OUT)
        posZTrack.addOrUpdateKeyframe(60, 0.0f, InterpolationType.EASE_IN_OUT)
        posYTrack.addOrUpdateKeyframe(60, 1.2f, InterpolationType.EASE_IN_OUT)

        posXTrack.addOrUpdateKeyframe(90, 0.0f, InterpolationType.EASE_IN_OUT)
        posZTrack.addOrUpdateKeyframe(90, -2.0f, InterpolationType.EASE_IN_OUT)
        posYTrack.addOrUpdateKeyframe(90, 0.6f, InterpolationType.EASE_IN_OUT)

        posXTrack.addOrUpdateKeyframe(120, 2.0f, InterpolationType.EASE_IN_OUT)
        posZTrack.addOrUpdateKeyframe(120, 0.0f, InterpolationType.EASE_IN_OUT)
        posYTrack.addOrUpdateKeyframe(120, 1.2f, InterpolationType.EASE_IN_OUT)

        return ProjectData(scene, timeline)
    }
}
