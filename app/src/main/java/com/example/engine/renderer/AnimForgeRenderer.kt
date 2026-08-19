package com.example.engine.renderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.engine.camera.OrbitCamera
import com.example.engine.math.Vector3
import com.example.engine.mesh.MeshData
import com.example.engine.mesh.PrimitiveType
import com.example.scene.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

enum class ViewportShading {
    RENDERED_PBR,
    SOLID,
    WIREFRAME,
    MATCAP
}

enum class GizmoMode {
    TRANSLATE,
    ROTATE,
    SCALE,
    NONE
}

enum class TransformSpace {
    WORLD,
    LOCAL
}

class AnimForgeRenderer : GLSurfaceView.Renderer {

    val camera = OrbitCamera()
    var scene: Scene = Scene.createDefaultScene()
    var selectedObjectId: String? = null
    var shadingMode: ViewportShading = ViewportShading.RENDERED_PBR
    var gizmoMode: GizmoMode = GizmoMode.TRANSLATE
    var transformSpace: TransformSpace = TransformSpace.WORLD

    // High-End PBR and Post-Processing Modules
    val shadowRenderer = ShadowMapRenderer(1024)
    val postPipeline = PostProcessingPipeline()

    private var pbrProgram = 0
    private var flatProgram = 0

    // Cached primitive meshes
    val primitiveMeshes = mutableMapOf<PrimitiveType, MeshData>()

    // Grid Buffer
    private lateinit var gridVertexBuffer: FloatBuffer
    private var gridLineCount = 0

    // Cached Static Buffers to avoid GC/ashmem allocation in render loop
    private lateinit var axesVertexBuffer: FloatBuffer
    private lateinit var translateGizmoBuffer: FloatBuffer
    private lateinit var rotateGizmoBufferX: FloatBuffer
    private lateinit var rotateGizmoBufferY: FloatBuffer
    private lateinit var rotateGizmoBufferZ: FloatBuffer
    private val rotateSegments = 32
    private lateinit var scaleGizmoBuffer: FloatBuffer

    // Matrices
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val mvMatrix = FloatArray(16)

    // Viewport dimensions
    private var viewportWidth = 1080
    private var viewportHeight = 1920

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val theme = scene.environment.viewportTheme
        GLES20.glClearColor(theme.r, theme.g, theme.b, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Compile Cook-Torrance GGX PBR Program and Flat Overlay Program
        pbrProgram = ShaderUtils.createProgram(PbrShaders.PBR_VERTEX_SHADER, PbrShaders.PBR_FRAGMENT_SHADER)
        flatProgram = ShaderUtils.createProgram(ShaderUtils.FLAT_COLOR_VERTEX_SHADER, ShaderUtils.FLAT_COLOR_FRAGMENT_SHADER)

        // Initialize Render Systems
        shadowRenderer.init()
        postPipeline.init()

        initPrimitiveMeshes()
        initGridBuffer()
        initStaticBuffers()
    }

    private fun initStaticBuffers() {
        // 1. Axes Buffer
        val axesVertices = floatArrayOf(
            // X Axis (-10 to 10)
            -10f, 0.001f, 0f, 10f, 0.001f, 0f,
            // Z Axis (-10 to 10)
            0f, 0.001f, -10f, 0f, 0.001f, 10f,
            // Y Axis (0 to 5)
            0f, 0f, 0f, 0f, 5f, 0f
        )
        axesVertexBuffer = ByteBuffer.allocateDirect(axesVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(axesVertices); position(0) }

        // 2. Translation Gizmo Buffer
        val gizmoLines = floatArrayOf(
            // X Axis Line & Arrow head
            0f, 0f, 0f,   1.2f, 0f, 0f,
            1.2f, 0f, 0f, 1.0f, 0.1f, 0f,
            1.2f, 0f, 0f, 1.0f, -0.1f, 0f,

            // Y Axis Line & Arrow head
            0f, 0f, 0f,   0f, 1.2f, 0f,
            0f, 1.2f, 0f, 0.1f, 1.0f, 0f,
            0f, 1.2f, 0f, -0.1f, 1.0f, 0f,

            // Z Axis Line & Arrow head
            0f, 0f, 0f,   0f, 0f, 1.2f,
            0f, 0f, 1.2f, 0f, 0.1f, 1.0f,
            0f, 0f, 1.2f, 0f, -0.1f, 1.0f
        )
        translateGizmoBuffer = ByteBuffer.allocateDirect(gizmoLines.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(gizmoLines); position(0) }

        // 3. Rotation Gizmo Buffers (Circles around X, Y, Z)
        val circleX = FloatArray(rotateSegments * 6)
        val circleY = FloatArray(rotateSegments * 6)
        val circleZ = FloatArray(rotateSegments * 6)

        for (i in 0 until rotateSegments) {
            val a0 = (i.toFloat() / rotateSegments) * 2f * Math.PI.toFloat()
            val a1 = ((i + 1).toFloat() / rotateSegments) * 2f * Math.PI.toFloat()

            // X Ring (YZ plane)
            circleX[i * 6 + 0] = 0f; circleX[i * 6 + 1] = kotlin.math.cos(a0); circleX[i * 6 + 2] = kotlin.math.sin(a0)
            circleX[i * 6 + 3] = 0f; circleX[i * 6 + 4] = kotlin.math.cos(a1); circleX[i * 6 + 5] = kotlin.math.sin(a1)

            // Y Ring (XZ plane)
            circleY[i * 6 + 0] = kotlin.math.cos(a0); circleY[i * 6 + 1] = 0f; circleY[i * 6 + 2] = kotlin.math.sin(a0)
            circleY[i * 6 + 3] = kotlin.math.cos(a1); circleY[i * 6 + 4] = 0f; circleY[i * 6 + 5] = kotlin.math.sin(a1)

            // Z Ring (XY plane)
            circleZ[i * 6 + 0] = kotlin.math.cos(a0); circleZ[i * 6 + 1] = kotlin.math.sin(a0); circleZ[i * 6 + 2] = 0f
            circleZ[i * 6 + 3] = kotlin.math.cos(a1); circleZ[i * 6 + 4] = kotlin.math.sin(a1); circleZ[i * 6 + 5] = 0f
        }

        rotateGizmoBufferX = ByteBuffer.allocateDirect(circleX.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(circleX); position(0) }
        rotateGizmoBufferY = ByteBuffer.allocateDirect(circleY.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(circleY); position(0) }
        rotateGizmoBufferZ = ByteBuffer.allocateDirect(circleZ.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(circleZ); position(0) }

        // 4. Scale Gizmo Buffer
        val scaleLines = floatArrayOf(
            // X Axis
            0f, 0f, 0f, 1.2f, 0f, 0f,
            1.2f, -0.08f, 0f, 1.2f, 0.08f, 0f,
            // Y Axis
            0f, 0f, 0f, 0f, 1.2f, 0f,
            -0.08f, 1.2f, 0f, 0.08f, 1.2f, 0f,
            // Z Axis
            0f, 0f, 0f, 0f, 0f, 1.2f,
            0f, -0.08f, 1.2f, 0f, 0.08f, 1.2f
        )
        scaleGizmoBuffer = ByteBuffer.allocateDirect(scaleLines.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(scaleLines); position(0) }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES20.glViewport(0, 0, width, height)
        camera.setViewportSize(width, height)
        postPipeline.resize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        camera.updateMatrices()

        // -------------------------------------------------------------
        // PASS 1: REAL-TIME SHADOW MAP DEPTH PASS
        // -------------------------------------------------------------
        if (scene.environment.shadowEnabled) {
            shadowRenderer.renderShadowPass(scene, primitiveMeshes)
        }

        // -------------------------------------------------------------
        // PASS 2: HDR SCENE PBR RENDERING PASS
        // -------------------------------------------------------------
        postPipeline.beginScenePass()

        val theme = scene.environment.viewportTheme
        GLES20.glClearColor(theme.r, theme.g, theme.b, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // 1. Draw Grid Floor & Axes
        if (scene.environment.showGrid) {
            drawGridAndAxes()
        }

        // 2. Draw Scene Objects with Full PBR Pipeline
        drawSceneObjectsPbr()

        // 3. Draw Transform Gizmo for selected object
        val selectedObj = scene.findObjectById(selectedObjectId ?: "")
        if (selectedObj != null && selectedObj.isVisible && gizmoMode != GizmoMode.NONE) {
            drawTransformGizmo(selectedObj)
        }

        // -------------------------------------------------------------
        // PASS 3: HDR BLOOM + ACES FILMIC TONE MAPPING + POST-PROCESSING
        // -------------------------------------------------------------
        postPipeline.endSceneAndRenderPostProcess(scene)
    }

    private fun initPrimitiveMeshes() {
        primitiveMeshes[PrimitiveType.CUBE] = MeshData.createCube()
        primitiveMeshes[PrimitiveType.SPHERE] = MeshData.createSphere()
        primitiveMeshes[PrimitiveType.PLANE] = MeshData.createPlane()
        primitiveMeshes[PrimitiveType.CYLINDER] = MeshData.createCylinder()
        primitiveMeshes[PrimitiveType.CONE] = MeshData.createCone()
        primitiveMeshes[PrimitiveType.TORUS] = MeshData.createTorus()
        primitiveMeshes[PrimitiveType.CAPSULE] = MeshData.createCapsule()
    }

    private fun initGridBuffer() {
        val lines = mutableListOf<Float>()
        val size = 10f
        val step = 1f

        var x = -size
        while (x <= size) {
            // Line parallel to Z axis
            lines.addAll(listOf(x, 0f, -size, x, 0f, size))
            x += step
        }

        var z = -size
        while (z <= size) {
            // Line parallel to X axis
            lines.addAll(listOf(-size, 0f, z, size, 0f, z))
            z += step
        }

        gridLineCount = lines.size / 3
        gridVertexBuffer = ByteBuffer.allocateDirect(lines.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(lines.toFloatArray())
                position(0)
            }
    }

    private fun drawGridAndAxes() {
        GLES20.glUseProgram(flatProgram)
        val mvpLoc = GLES20.glGetUniformLocation(flatProgram, "u_MVPMatrix")
        val colorLoc = GLES20.glGetUniformLocation(flatProgram, "u_Color")
        val posLoc = GLES20.glGetAttribLocation(flatProgram, "a_Position")

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, camera.viewProjMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)

        // Dynamic Grid lines according to workspace theme
        val theme = scene.environment.viewportTheme
        GLES20.glUniform4f(colorLoc, theme.gridR, theme.gridG, theme.gridB, theme.gridAlpha)
        GLES20.glEnableVertexAttribArray(posLoc)
        gridVertexBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, gridVertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, gridLineCount)

        if (scene.environment.showAxes) {
            GLES20.glLineWidth(2.5f)

            // Red X Axis
            GLES20.glUniform4f(colorLoc, 0.95f, 0.25f, 0.25f, 0.85f)
            axesVertexBuffer.position(0)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, axesVertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)

            // Blue Z Axis
            GLES20.glUniform4f(colorLoc, 0.25f, 0.55f, 0.95f, 0.85f)
            axesVertexBuffer.position(6)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, axesVertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)

            // Green Y Axis
            GLES20.glUniform4f(colorLoc, 0.15f, 0.85f, 0.45f, 0.85f)
            axesVertexBuffer.position(12)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, axesVertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)

            GLES20.glLineWidth(1f)
        }

        GLES20.glDisableVertexAttribArray(posLoc)
    }

    private fun drawSceneObjectsPbr() {
        GLES20.glUseProgram(pbrProgram)

        // Find primary Sun light and Point light
        val sunLight = scene.objects.find { it.type == ObjectType.LIGHT && it.lightData?.type == LightType.DIRECTIONAL }
        val sunDir = sunLight?.let {
            val radX = Math.toRadians(it.transform.rotation.x.toDouble()).toFloat()
            val radY = Math.toRadians(it.transform.rotation.y.toDouble()).toFloat()
            Vector3(
                kotlin.math.cos(radX) * kotlin.math.sin(radY),
                -kotlin.math.sin(radX),
                -kotlin.math.cos(radX) * kotlin.math.cos(radY)
            ).normalize()
        } ?: Vector3(0.5f, -1.0f, 0.5f).normalize()

        val sunIntensity = sunLight?.lightData?.intensity ?: 1.4f
        val sunColor = sunLight?.lightData?.color ?: floatArrayOf(1f, 0.98f, 0.94f, 1f)

        val pointLight = scene.objects.find { it.type == ObjectType.LIGHT && it.lightData?.type == LightType.POINT }
        val pointPos = pointLight?.transform?.position ?: Vector3(-3f, 3f, -3f)
        val pointColor = pointLight?.lightData?.color ?: floatArrayOf(0.6f, 0.8f, 1f, 1f)
        val pointIntensity = pointLight?.lightData?.intensity ?: 0.8f
        val pointRange = pointLight?.lightData?.range ?: 15f

        // Matrices Uniforms
        val mvpLoc = GLES20.glGetUniformLocation(pbrProgram, "u_MVPMatrix")
        val modelLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ModelMatrix")
        val viewLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ViewMatrix")
        val lightSpaceLoc = GLES20.glGetUniformLocation(pbrProgram, "u_LightSpaceMatrix")
        val camPosLoc = GLES20.glGetUniformLocation(pbrProgram, "u_CameraPos")

        // Material Uniforms
        val baseColorLoc = GLES20.glGetUniformLocation(pbrProgram, "u_BaseColor")
        val metallicLoc = GLES20.glGetUniformLocation(pbrProgram, "u_Metallic")
        val roughnessLoc = GLES20.glGetUniformLocation(pbrProgram, "u_Roughness")
        val specularLoc = GLES20.glGetUniformLocation(pbrProgram, "u_Specular")
        val emissColorLoc = GLES20.glGetUniformLocation(pbrProgram, "u_EmissionColor")
        val emissIntensLoc = GLES20.glGetUniformLocation(pbrProgram, "u_EmissionIntensity")
        val transLoc = GLES20.glGetUniformLocation(pbrProgram, "u_Transmission")
        val iorLoc = GLES20.glGetUniformLocation(pbrProgram, "u_IOR")
        val clearCoatLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ClearCoat")
        val clearCoatRoughLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ClearCoatRoughness")
        val sheenLoc = GLES20.glGetUniformLocation(pbrProgram, "u_Sheen")
        val anisoLoc = GLES20.glGetUniformLocation(pbrProgram, "u_Anisotropy")
        val aoLoc = GLES20.glGetUniformLocation(pbrProgram, "u_AO")
        val isSelectedLoc = GLES20.glGetUniformLocation(pbrProgram, "u_IsSelected")

        // Sun & Point Light Uniforms
        val sunDirLoc = GLES20.glGetUniformLocation(pbrProgram, "u_SunDir")
        val sunColLoc = GLES20.glGetUniformLocation(pbrProgram, "u_SunColor")
        val sunIntensLoc = GLES20.glGetUniformLocation(pbrProgram, "u_SunIntensity")

        val ptPosLoc = GLES20.glGetUniformLocation(pbrProgram, "u_PointLightPos")
        val ptColLoc = GLES20.glGetUniformLocation(pbrProgram, "u_PointLightColor")
        val ptIntensLoc = GLES20.glGetUniformLocation(pbrProgram, "u_PointLightIntensity")
        val ptRangeLoc = GLES20.glGetUniformLocation(pbrProgram, "u_PointLightRange")

        // Ambient & IBL
        val ambColLoc = GLES20.glGetUniformLocation(pbrProgram, "u_AmbientColor")
        val ambIntensLoc = GLES20.glGetUniformLocation(pbrProgram, "u_AmbientIntensity")
        val themeIdLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ViewportThemeId")

        // Shadow Map Uniforms
        val shadowMapLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ShadowMap")
        val shadowBiasLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ShadowBias")
        val shadowsEnLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ShadowsEnabled")
        val contactShadowsEnLoc = GLES20.glGetUniformLocation(pbrProgram, "u_ContactShadowsEnabled")

        // Attributes
        val posLoc = GLES20.glGetAttribLocation(pbrProgram, "a_Position")
        val normLoc = GLES20.glGetAttribLocation(pbrProgram, "a_Normal")
        val texLoc = GLES20.glGetAttribLocation(pbrProgram, "a_TexCoord")

        // Set Scene-wide Uniforms
        GLES20.glUniformMatrix4fv(viewLoc, 1, false, camera.viewMatrix, 0)
        GLES20.glUniformMatrix4fv(lightSpaceLoc, 1, false, shadowRenderer.lightSpaceMatrix, 0)
        GLES20.glUniform3f(camPosLoc, camera.eye.x, camera.eye.y, camera.eye.z)

        GLES20.glUniform3f(sunDirLoc, sunDir.x, sunDir.y, sunDir.z)
        GLES20.glUniform4fv(sunColLoc, 1, sunColor, 0)
        GLES20.glUniform1f(sunIntensLoc, sunIntensity)

        GLES20.glUniform3f(ptPosLoc, pointPos.x, pointPos.y, pointPos.z)
        GLES20.glUniform4fv(ptColLoc, 1, pointColor, 0)
        GLES20.glUniform1f(ptIntensLoc, pointIntensity)
        GLES20.glUniform1f(ptRangeLoc, pointRange)

        GLES20.glUniform4fv(ambColLoc, 1, scene.environment.ambientColor, 0)
        GLES20.glUniform1f(ambIntensLoc, scene.environment.ambientIntensity)
        GLES20.glUniform1i(themeIdLoc, scene.environment.viewportTheme.ordinal)

        // Bind Shadow Depth Texture to Texture Unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowRenderer.depthTextureId)
        GLES20.glUniform1i(shadowMapLoc, 0)
        GLES20.glUniform1f(shadowBiasLoc, scene.environment.shadowBias)
        GLES20.glUniform1i(shadowsEnLoc, if (scene.environment.shadowEnabled) 1 else 0)
        GLES20.glUniform1i(contactShadowsEnLoc, if (scene.environment.contactShadows) 1 else 0)

        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glEnableVertexAttribArray(normLoc)
        GLES20.glEnableVertexAttribArray(texLoc)

        for (obj in scene.objects) {
            if (!obj.isVisible) continue

            // Build Model Matrix
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, obj.transform.position.x, obj.transform.position.y, obj.transform.position.z)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.z, 0f, 0f, 1f)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.y, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.x, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, obj.transform.scale.x, obj.transform.scale.y, obj.transform.scale.z)

            Matrix.multiplyMM(mvpMatrix, 0, camera.viewProjMatrix, 0, modelMatrix, 0)

            GLES20.glUniformMatrix4fv(modelLoc, 1, false, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)

            val isSelected = (obj.id == selectedObjectId)
            GLES20.glUniform1f(isSelectedLoc, if (isSelected) 1f else 0f)

            if (obj.type == ObjectType.MESH) {
                // Set PBR Material properties
                when (shadingMode) {
                    ViewportShading.SOLID -> {
                        GLES20.glUniform4f(baseColorLoc, 0.85f, 0.88f, 0.92f, 1.0f)
                        GLES20.glUniform1f(metallicLoc, 0.0f)
                        GLES20.glUniform1f(roughnessLoc, 0.5f)
                        GLES20.glUniform1f(specularLoc, 0.3f)
                        GLES20.glUniform4f(emissColorLoc, 0f, 0f, 0f, 1f)
                        GLES20.glUniform1f(emissIntensLoc, 0f)
                        GLES20.glUniform1f(transLoc, 0f)
                        GLES20.glUniform1f(iorLoc, 1.45f)
                        GLES20.glUniform1f(clearCoatLoc, 0f)
                        GLES20.glUniform1f(clearCoatRoughLoc, 0.05f)
                        GLES20.glUniform1f(sheenLoc, 0f)
                        GLES20.glUniform1f(anisoLoc, 0f)
                        GLES20.glUniform1f(aoLoc, 1.0f)
                    }
                    ViewportShading.MATCAP -> {
                        GLES20.glUniform4f(baseColorLoc, 0.95f, 0.65f, 0.35f, 1.0f)
                        GLES20.glUniform1f(metallicLoc, 0.5f)
                        GLES20.glUniform1f(roughnessLoc, 0.2f)
                        GLES20.glUniform1f(specularLoc, 0.8f)
                        GLES20.glUniform4f(emissColorLoc, 0f, 0f, 0f, 1f)
                        GLES20.glUniform1f(emissIntensLoc, 0f)
                        GLES20.glUniform1f(transLoc, 0f)
                        GLES20.glUniform1f(iorLoc, 1.45f)
                        GLES20.glUniform1f(clearCoatLoc, 0.5f)
                        GLES20.glUniform1f(clearCoatRoughLoc, 0.05f)
                        GLES20.glUniform1f(sheenLoc, 0f)
                        GLES20.glUniform1f(anisoLoc, 0f)
                        GLES20.glUniform1f(aoLoc, 1.0f)
                    }
                    else -> {
                        val mat = obj.material
                        GLES20.glUniform4fv(baseColorLoc, 1, mat.baseColor, 0)
                        GLES20.glUniform1f(metallicLoc, mat.metallic)
                        GLES20.glUniform1f(roughnessLoc, mat.roughness)
                        GLES20.glUniform1f(specularLoc, mat.specular)
                        GLES20.glUniform4fv(emissColorLoc, 1, mat.emissionColor, 0)
                        GLES20.glUniform1f(emissIntensLoc, mat.emissionIntensity)
                        GLES20.glUniform1f(transLoc, mat.transmission)
                        GLES20.glUniform1f(iorLoc, mat.ior)
                        GLES20.glUniform1f(clearCoatLoc, mat.clearCoat)
                        GLES20.glUniform1f(clearCoatRoughLoc, mat.clearCoatRoughness)
                        GLES20.glUniform1f(sheenLoc, mat.sheen)
                        GLES20.glUniform1f(anisoLoc, mat.anisotropy)
                        GLES20.glUniform1f(aoLoc, mat.ao)
                    }
                }

                val mesh = primitiveMeshes[obj.primitiveType] ?: primitiveMeshes[PrimitiveType.CUBE]!!

                mesh.vertexBuffer.position(MeshData.POS_OFFSET / 4)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, MeshData.STRIDE, mesh.vertexBuffer)

                mesh.vertexBuffer.position(MeshData.NORMAL_OFFSET / 4)
                GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, MeshData.STRIDE, mesh.vertexBuffer)

                mesh.vertexBuffer.position(MeshData.UV_OFFSET / 4)
                GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, MeshData.STRIDE, mesh.vertexBuffer)

                if (shadingMode == ViewportShading.WIREFRAME || obj.material.isWireframe) {
                    GLES20.glDrawElements(GLES20.GL_LINES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
                } else {
                    GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
                }
            } else if (obj.type == ObjectType.LIGHT) {
                // Draw Light Source Visual Icon / Billboard Sphere
                GLES20.glUniform4f(baseColorLoc, 1.0f, 0.85f, 0.2f, 1.0f)
                GLES20.glUniform1f(metallicLoc, 0f)
                GLES20.glUniform1f(roughnessLoc, 0.1f)
                GLES20.glUniform1f(specularLoc, 0.9f)
                GLES20.glUniform4f(emissColorLoc, 1.0f, 0.9f, 0.3f, 1.0f)
                GLES20.glUniform1f(emissIntensLoc, 3.5f)
                GLES20.glUniform1f(transLoc, 0f)
                GLES20.glUniform1f(iorLoc, 1.0f)
                GLES20.glUniform1f(clearCoatLoc, 0f)
                GLES20.glUniform1f(clearCoatRoughLoc, 0.05f)
                GLES20.glUniform1f(sheenLoc, 0f)
                GLES20.glUniform1f(anisoLoc, 0f)
                GLES20.glUniform1f(aoLoc, 1.0f)

                val sphereMesh = primitiveMeshes[PrimitiveType.SPHERE]!!
                sphereMesh.vertexBuffer.position(MeshData.POS_OFFSET / 4)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, MeshData.STRIDE, sphereMesh.vertexBuffer)

                sphereMesh.vertexBuffer.position(MeshData.NORMAL_OFFSET / 4)
                GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, MeshData.STRIDE, sphereMesh.vertexBuffer)

                sphereMesh.vertexBuffer.position(MeshData.UV_OFFSET / 4)
                GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, MeshData.STRIDE, sphereMesh.vertexBuffer)

                GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereMesh.indexCount, GLES20.GL_UNSIGNED_SHORT, sphereMesh.indexBuffer)
            }
        }

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(normLoc)
        GLES20.glDisableVertexAttribArray(texLoc)
    }

    private fun drawTransformGizmo(obj: SceneObject) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glUseProgram(flatProgram)

        val mvpLoc = GLES20.glGetUniformLocation(flatProgram, "u_MVPMatrix")
        val colorLoc = GLES20.glGetUniformLocation(flatProgram, "u_Color")
        val posLoc = GLES20.glGetAttribLocation(flatProgram, "a_Position")

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, obj.transform.position.x, obj.transform.position.y, obj.transform.position.z)
        if (transformSpace == TransformSpace.LOCAL) {
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.z, 0f, 0f, 1f)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.y, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.x, 1f, 0f, 0f)
        }

        // Scale gizmo slightly proportional to camera distance so it stays comfortably sized
        val camDist = (camera.eye - obj.transform.position).length()
        val gizmoScale = (camDist * 0.18f).coerceIn(0.5f, 5.0f)
        Matrix.scaleM(modelMatrix, 0, gizmoScale, gizmoScale, gizmoScale)

        Matrix.multiplyMM(mvpMatrix, 0, camera.viewProjMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(posLoc)

        when (gizmoMode) {
            GizmoMode.TRANSLATE -> {
                GLES20.glLineWidth(4.0f)

                // Red X
                GLES20.glUniform4f(colorLoc, 1.0f, 0.2f, 0.2f, 1.0f)
                translateGizmoBuffer.position(0)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, translateGizmoBuffer)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 6)

                // Green Y
                GLES20.glUniform4f(colorLoc, 0.1f, 0.9f, 0.3f, 1.0f)
                translateGizmoBuffer.position(18)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, translateGizmoBuffer)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 6)

                // Blue Z
                GLES20.glUniform4f(colorLoc, 0.15f, 0.6f, 1.0f, 1.0f)
                translateGizmoBuffer.position(36)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, translateGizmoBuffer)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 6)
            }

            GizmoMode.ROTATE -> {
                GLES20.glLineWidth(3.5f)

                // Red X Ring
                GLES20.glUniform4f(colorLoc, 1.0f, 0.25f, 0.25f, 1.0f)
                rotateGizmoBufferX.position(0)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, rotateGizmoBufferX)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, rotateSegments * 2)

                // Green Y Ring
                GLES20.glUniform4f(colorLoc, 0.1f, 0.9f, 0.35f, 1.0f)
                rotateGizmoBufferY.position(0)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, rotateGizmoBufferY)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, rotateSegments * 2)

                // Blue Z Ring
                GLES20.glUniform4f(colorLoc, 0.15f, 0.6f, 1.0f, 1.0f)
                rotateGizmoBufferZ.position(0)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, rotateGizmoBufferZ)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, rotateSegments * 2)
            }

            GizmoMode.SCALE -> {
                GLES20.glLineWidth(4.0f)

                // Red X Scale
                GLES20.glUniform4f(colorLoc, 1.0f, 0.2f, 0.2f, 1.0f)
                scaleGizmoBuffer.position(0)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, scaleGizmoBuffer)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 4)

                // Green Y Scale
                GLES20.glUniform4f(colorLoc, 0.1f, 0.9f, 0.3f, 1.0f)
                scaleGizmoBuffer.position(12)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, scaleGizmoBuffer)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 4)

                // Blue Z Scale
                GLES20.glUniform4f(colorLoc, 0.15f, 0.6f, 1.0f, 1.0f)
                scaleGizmoBuffer.position(24)
                GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 3 * 4, scaleGizmoBuffer)
                GLES20.glDrawArrays(GLES20.GL_LINES, 0, 4)
            }
            GizmoMode.NONE -> {}
        }

        GLES20.glLineWidth(1f)
        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }
}
