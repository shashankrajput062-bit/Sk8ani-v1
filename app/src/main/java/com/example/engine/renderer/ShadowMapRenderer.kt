package com.example.engine.renderer

import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.example.engine.math.Vector3
import com.example.scene.LightType
import com.example.scene.ObjectType
import com.example.scene.Scene
import java.nio.FloatBuffer

class ShadowMapRenderer(
    var shadowMapSize: Int = 1024
) {
    private val TAG = "ShadowMapRenderer"

    var fboId: Int = 0
        private set
    var depthTextureId: Int = 0
        private set
    var shadowProgram: Int = 0
        private set

    val lightSpaceMatrix = FloatArray(16)
    private val lightProjectionMatrix = FloatArray(16)
    private val lightViewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val lightSpaceMVP = FloatArray(16)

    private var isInitialized = false

    fun init() {
        if (isInitialized) return

        // 1. Create Depth Shader
        shadowProgram = ShaderUtils.createProgram(
            PbrShaders.SHADOW_DEPTH_VERTEX,
            PbrShaders.SHADOW_DEPTH_FRAGMENT
        )

        // 2. Create Depth FBO and Texture
        val fbos = IntArray(1)
        GLES20.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        depthTextureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTextureId)
        // Allocate texture for depth (Using GL_RGBA / GL_UNSIGNED_BYTE or GL_DEPTH_COMPONENT for broad GLES compatibility)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_DEPTH_COMPONENT,
            shadowMapSize,
            shadowMapSize,
            0,
            GLES20.GL_DEPTH_COMPONENT,
            GLES20.GL_UNSIGNED_INT,
            null
        )

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Attach depth texture to FBO
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_DEPTH_ATTACHMENT,
            GLES20.GL_TEXTURE_2D,
            depthTextureId,
            0
        )

        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.w(TAG, "Shadow FBO incomplete with GL_DEPTH_COMPONENT ($status), falling back to renderbuffer")
            // Fallback for hardware with standard depth renderbuffer
            val rbos = IntArray(1)
            GLES20.glGenRenderbuffers(1, rbos, 0)
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rbos[0])
            GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, shadowMapSize, shadowMapSize)
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, rbos[0])
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        isInitialized = true
    }

    fun computeLightMatrix(scene: Scene) {
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

        val lightPos = Vector3(-sunDir.x * 12f, -sunDir.y * 12f, -sunDir.z * 12f)
        val target = Vector3(0f, 0f, 0f)
        val up = Vector3(0f, 1f, 0f)

        // Orthogonal projection for directional sun shadows (covering the scene bounds)
        val orthoSize = 12.0f
        Matrix.orthoM(lightProjectionMatrix, 0, -orthoSize, orthoSize, -orthoSize, orthoSize, 1.0f, 30.0f)
        Matrix.setLookAtM(lightViewMatrix, 0, lightPos.x, lightPos.y, lightPos.z, target.x, target.y, target.z, up.x, up.y, up.z)
        Matrix.multiplyMM(lightSpaceMatrix, 0, lightProjectionMatrix, 0, lightViewMatrix, 0)
    }

    fun renderShadowPass(
        scene: Scene,
        primitiveMeshes: Map<com.example.engine.mesh.PrimitiveType, com.example.engine.mesh.MeshData>
    ) {
        if (!isInitialized || !scene.environment.shadowEnabled) return

        computeLightMatrix(scene)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, shadowMapSize, shadowMapSize)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glCullFace(GLES20.GL_FRONT) // Cull front faces to prevent self-shadowing shadow acne

        GLES20.glUseProgram(shadowProgram)
        val mvpLoc = GLES20.glGetUniformLocation(shadowProgram, "u_LightSpaceMVP")
        val posLoc = GLES20.glGetAttribLocation(shadowProgram, "a_Position")

        GLES20.glEnableVertexAttribArray(posLoc)

        for (obj in scene.objects) {
            if (!obj.isVisible || obj.type != ObjectType.MESH) continue
            val mesh = primitiveMeshes[obj.primitiveType] ?: primitiveMeshes[com.example.engine.mesh.PrimitiveType.CUBE] ?: continue

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, obj.transform.position.x, obj.transform.position.y, obj.transform.position.z)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.z, 0f, 0f, 1f)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.y, 0f, 1f, 0f)
            Matrix.rotateM(modelMatrix, 0, obj.transform.rotation.x, 1f, 0f, 0f)
            Matrix.scaleM(modelMatrix, 0, obj.transform.scale.x, obj.transform.scale.y, obj.transform.scale.z)

            Matrix.multiplyMM(lightSpaceMVP, 0, lightSpaceMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(mvpLoc, 1, false, lightSpaceMVP, 0)

            mesh.vertexBuffer.position(com.example.engine.mesh.MeshData.POS_OFFSET / 4)
            GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, com.example.engine.mesh.MeshData.STRIDE, mesh.vertexBuffer)
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, mesh.indexCount, GLES20.GL_UNSIGNED_SHORT, mesh.indexBuffer)
        }

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun release() {
        if (!isInitialized) return
        if (fboId != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        if (depthTextureId != 0) GLES20.glDeleteTextures(1, intArrayOf(depthTextureId), 0)
        if (shadowProgram != 0) GLES20.glDeleteProgram(shadowProgram)
        isInitialized = false
    }
}
