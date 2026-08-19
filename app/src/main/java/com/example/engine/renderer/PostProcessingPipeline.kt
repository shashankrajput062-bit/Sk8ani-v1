package com.example.engine.renderer

import android.opengl.GLES20
import android.util.Log
import com.example.scene.Scene
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PostProcessingPipeline {
    private val TAG = "PostProcessingPipeline"

    var width: Int = 1080
        private set
    var height: Int = 1920
        private set

    // HDR Scene Render Target
    var sceneFbo: Int = 0
        private set
    var sceneColorTexture: Int = 0
        private set
    var sceneDepthTexture: Int = 0
        private set

    // Bloom Ping-Pong Targets (Downscaled by 2 for performance)
    private var bloomFboA: Int = 0
    private var bloomTexA: Int = 0
    private var bloomFboB: Int = 0
    private var bloomTexB: Int = 0
    private var bloomWidth: Int = 0
    private var bloomHeight: Int = 0

    // Shader Programs
    private var brightPassProgram: Int = 0
    private var blurProgram: Int = 0
    private var compositeProgram: Int = 0

    // Fullscreen Quad Buffer
    private lateinit var quadBuffer: FloatBuffer

    private var isInitialized = false

    fun init() {
        if (isInitialized) return

        // 1. Fullscreen Quad vertices (Positions: 2, UVs: 2)
        val quadVertices = floatArrayOf(
            // Position (X, Y), UV (U, V)
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f,
             1.0f, -1.0f,  1.0f, 0.0f
        )
        quadBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply { put(quadVertices); position(0) }

        // 2. Compile Shaders
        brightPassProgram = ShaderUtils.createProgram(
            PbrShaders.POST_QUAD_VERTEX,
            PbrShaders.BLOOM_BRIGHT_PASS_FRAGMENT
        )

        blurProgram = ShaderUtils.createProgram(
            PbrShaders.POST_QUAD_VERTEX,
            PbrShaders.GAUSSIAN_BLUR_FRAGMENT
        )

        compositeProgram = ShaderUtils.createProgram(
            PbrShaders.POST_QUAD_VERTEX,
            PbrShaders.POST_COMPOSITE_FRAGMENT
        )

        isInitialized = true
    }

    fun resize(newWidth: Int, newHeight: Int) {
        if (newWidth <= 0 || newHeight <= 0) return
        if (newWidth == width && newHeight == height && sceneFbo != 0) return

        width = newWidth
        height = newHeight
        bloomWidth = (width / 2).coerceAtLeast(1)
        bloomHeight = (height / 2).coerceAtLeast(1)

        releaseFramebuffers()

        // 1. Create Scene HDR FBO
        val fbos = IntArray(3)
        GLES20.glGenFramebuffers(3, fbos, 0)
        sceneFbo = fbos[0]
        bloomFboA = fbos[1]
        bloomFboB = fbos[2]

        val textures = IntArray(4)
        GLES20.glGenTextures(4, textures, 0)
        sceneColorTexture = textures[0]
        sceneDepthTexture = textures[1]
        bloomTexA = textures[2]
        bloomTexB = textures[3]

        // Scene Color Texture (RGBA)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sceneColorTexture)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Scene Depth Texture / Renderbuffer
        val rbos = IntArray(1)
        GLES20.glGenRenderbuffers(1, rbos, 0)
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rbos[0])
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, sceneFbo)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, sceneColorTexture, 0)
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, rbos[0])

        // Bloom Ping-Pong Targets
        setupBloomTarget(bloomFboA, bloomTexA, bloomWidth, bloomHeight)
        setupBloomTarget(bloomFboB, bloomTexB, bloomWidth, bloomHeight)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    private fun setupBloomTarget(fbo: Int, texture: Int, w: Int, h: Int) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, w, h, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0)
    }

    fun beginScenePass() {
        if (sceneFbo != 0) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, sceneFbo)
            GLES20.glViewport(0, 0, width, height)
        }
    }

    fun endSceneAndRenderPostProcess(scene: Scene) {
        if (!isInitialized || sceneFbo == 0) return

        val env = scene.environment

        // -------------------------------------------------------------
        // 1. BLOOM EXTRACTION & DUAL-PASS BLUR
        // -------------------------------------------------------------
        if (env.bloomEnabled) {
            // Bright pass -> bloomFboA
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bloomFboA)
            GLES20.glViewport(0, 0, bloomWidth, bloomHeight)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)

            GLES20.glUseProgram(brightPassProgram)
            val sceneTexLoc = GLES20.glGetUniformLocation(brightPassProgram, "u_SceneTexture")
            val threshLoc = GLES20.glGetUniformLocation(brightPassProgram, "u_BloomThreshold")
            val posLoc = GLES20.glGetAttribLocation(brightPassProgram, "a_Position")
            val uvLoc = GLES20.glGetAttribLocation(brightPassProgram, "a_TexCoord")

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sceneColorTexture)
            GLES20.glUniform1i(sceneTexLoc, 0)
            GLES20.glUniform1f(threshLoc, 0.95f)

            renderQuad(posLoc, uvLoc)

            // Horizontal Blur: bloomFboA -> bloomFboB
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bloomFboB)
            GLES20.glUseProgram(blurProgram)
            val blurTexLoc = GLES20.glGetUniformLocation(blurProgram, "u_Texture")
            val blurDirLoc = GLES20.glGetUniformLocation(blurProgram, "u_Direction")
            val blurPosLoc = GLES20.glGetAttribLocation(blurProgram, "a_Position")
            val blurUvLoc = GLES20.glGetAttribLocation(blurProgram, "a_TexCoord")

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bloomTexA)
            GLES20.glUniform1i(blurTexLoc, 0)
            GLES20.glUniform2f(blurDirLoc, 1.0f / bloomWidth.toFloat(), 0.0f)

            renderQuad(blurPosLoc, blurUvLoc)

            // Vertical Blur: bloomFboB -> bloomFboA
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, bloomFboA)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bloomTexB)
            GLES20.glUniform1i(blurTexLoc, 0)
            GLES20.glUniform2f(blurDirLoc, 0.0f, 1.0f / bloomHeight.toFloat())

            renderQuad(blurPosLoc, blurUvLoc)
        }

        // -------------------------------------------------------------
        // 2. FINAL COMPOSITE & ACES FILMIC TONE MAPPING -> DEFAULT SCREEN
        // -------------------------------------------------------------
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        GLES20.glUseProgram(compositeProgram)

        val compSceneTexLoc = GLES20.glGetUniformLocation(compositeProgram, "u_SceneTexture")
        val compBloomTexLoc = GLES20.glGetUniformLocation(compositeProgram, "u_BloomTexture")
        val bloomIntensLoc = GLES20.glGetUniformLocation(compositeProgram, "u_BloomIntensity")
        val bloomEnLoc = GLES20.glGetUniformLocation(compositeProgram, "u_BloomEnabled")
        val exposureLoc = GLES20.glGetUniformLocation(compositeProgram, "u_Exposure")
        val contrastLoc = GLES20.glGetUniformLocation(compositeProgram, "u_Contrast")
        val satLoc = GLES20.glGetUniformLocation(compositeProgram, "u_Saturation")
        val vigStrLoc = GLES20.glGetUniformLocation(compositeProgram, "u_VignetteStrength")
        val vigRadLoc = GLES20.glGetUniformLocation(compositeProgram, "u_VignetteRadius")
        val toneMapLoc = GLES20.glGetUniformLocation(compositeProgram, "u_ToneMappingMode")
        val fxaaLoc = GLES20.glGetUniformLocation(compositeProgram, "u_FXAAEnabled")
        val screenLoc = GLES20.glGetUniformLocation(compositeProgram, "u_ScreenSize")

        val compPosLoc = GLES20.glGetAttribLocation(compositeProgram, "a_Position")
        val compUvLoc = GLES20.glGetAttribLocation(compositeProgram, "a_TexCoord")

        // Bind Scene Color Texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sceneColorTexture)
        GLES20.glUniform1i(compSceneTexLoc, 0)

        // Bind Bloom Texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bloomTexA)
        GLES20.glUniform1i(compBloomTexLoc, 1)

        // Set Post-Processing Parameters
        GLES20.glUniform1f(bloomIntensLoc, if (env.bloomEnabled) 0.65f else 0.0f)
        GLES20.glUniform1i(bloomEnLoc, if (env.bloomEnabled) 1 else 0)
        GLES20.glUniform1f(exposureLoc, 0.05f) // Subtle EV boost
        GLES20.glUniform1f(contrastLoc, 1.08f) // Eevee filmic punch
        GLES20.glUniform1f(satLoc, 1.05f)
        GLES20.glUniform1f(vigStrLoc, 0.35f)
        GLES20.glUniform1f(vigRadLoc, 0.85f)
        GLES20.glUniform1i(toneMapLoc, 0) // 0 = ACES Filmic
        GLES20.glUniform1i(fxaaLoc, 1) // FXAA enabled
        GLES20.glUniform2f(screenLoc, width.toFloat(), height.toFloat())

        renderQuad(compPosLoc, compUvLoc)

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    }

    private fun renderQuad(posLoc: Int, uvLoc: Int) {
        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glEnableVertexAttribArray(uvLoc)

        quadBuffer.position(0)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 4 * 4, quadBuffer)

        quadBuffer.position(2)
        GLES20.glVertexAttribPointer(uvLoc, 2, GLES20.GL_FLOAT, false, 4 * 4, quadBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(uvLoc)
    }

    private fun releaseFramebuffers() {
        if (sceneFbo != 0) GLES20.glDeleteFramebuffers(3, intArrayOf(sceneFbo, bloomFboA, bloomFboB), 0)
        if (sceneColorTexture != 0) GLES20.glDeleteTextures(4, intArrayOf(sceneColorTexture, sceneDepthTexture, bloomTexA, bloomTexB), 0)
        sceneFbo = 0
        bloomFboA = 0
        bloomFboB = 0
        sceneColorTexture = 0
        sceneDepthTexture = 0
        bloomTexA = 0
        bloomTexB = 0
    }

    fun release() {
        releaseFramebuffers()
        if (brightPassProgram != 0) GLES20.glDeleteProgram(brightPassProgram)
        if (blurProgram != 0) GLES20.glDeleteProgram(blurProgram)
        if (compositeProgram != 0) GLES20.glDeleteProgram(compositeProgram)
        isInitialized = false
    }
}
