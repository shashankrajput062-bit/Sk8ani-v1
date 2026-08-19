package com.example.engine.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.example.engine.math.Vector3
import com.example.scene.ObjectType
import kotlin.math.hypot

class AnimForgeGLSurfaceView(
    context: Context,
    val renderer: AnimForgeRenderer,
    private val onObjectSelected: (String?) -> Unit,
    private val onTransformChanged: () -> Unit
) : GLSurfaceView(context) {

    private var previousX = 0f
    private var previousY = 0f
    private var previousTwoFingerX = 0f
    private var previousTwoFingerY = 0f
    private var isDraggingGizmo = false
    private var activeGizmoAxis: Int = 0 // 1: X, 2: Y, 3: Z

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            if (scaleFactor != 0f) {
                renderer.camera.zoom(1f / scaleFactor)
                requestRender()
            }
            return true
        }
    })

    private val tapDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTapSelection(e.x, e.y)
            return true
        }
    })

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        tapDetector.onTouchEvent(event)

        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
                isDraggingGizmo = false

                // Check if user touched near selected object's gizmo handles
                val selectedObj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "")
                if (selectedObj != null && renderer.gizmoMode != GizmoMode.NONE) {
                    val ray = renderer.camera.screenToRay(event.x, event.y, width, height)
                    val dist = ray.intersectSphere(selectedObj.transform.position, 1.2f)
                    if (dist != null) {
                        isDraggingGizmo = true
                        // Determine axis based on ray direction relative to camera
                        activeGizmoAxis = 1
                    }
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    previousTwoFingerX = (event.getX(0) + event.getX(1)) / 2f
                    previousTwoFingerY = (event.getY(0) + event.getY(1)) / 2f
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 1) {
                    val dx = event.x - previousX
                    val dy = event.y - previousY

                    if (isDraggingGizmo) {
                        val selectedObj = renderer.scene.findObjectById(renderer.selectedObjectId ?: "")
                        if (selectedObj != null) {
                            when (renderer.gizmoMode) {
                                GizmoMode.TRANSLATE -> {
                                    val factor = (renderer.camera.distance * 0.003f)
                                    selectedObj.transform.position.x += dx * factor
                                    selectedObj.transform.position.y -= dy * factor
                                    onTransformChanged()
                                }
                                GizmoMode.ROTATE -> {
                                    selectedObj.transform.rotation.y += dx * 0.5f
                                    selectedObj.transform.rotation.x += dy * 0.5f
                                    onTransformChanged()
                                }
                                GizmoMode.SCALE -> {
                                    val sDelta = (dx - dy) * 0.01f
                                    val s = (selectedObj.transform.scale.x + sDelta).coerceIn(0.05f, 50f)
                                    selectedObj.transform.scale.set(s, s, s)
                                    onTransformChanged()
                                }
                                GizmoMode.NONE -> {}
                            }
                        }
                    } else {
                        // Orbit Camera (Single finger drag)
                        val sensitivity = 0.35f
                        renderer.camera.orbit(dx * sensitivity, dy * sensitivity)
                    }

                    previousX = event.x
                    previousY = event.y
                } else if (pointerCount >= 2) {
                    // Two-finger Pan Camera
                    val midX = (event.getX(0) + event.getX(1)) / 2f
                    val midY = (event.getY(0) + event.getY(1)) / 2f

                    val panDx = midX - previousTwoFingerX
                    val panDy = midY - previousTwoFingerY

                    renderer.camera.pan(panDx, panDy)

                    previousTwoFingerX = midX
                    previousTwoFingerY = midY
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingGizmo = false
            }
        }

        return true
    }

    private fun handleTapSelection(touchX: Float, touchY: Float) {
        if (width <= 0 || height <= 0) return
        val ray = renderer.camera.screenToRay(touchX, touchY, width, height)

        var closestObjId: String? = null
        var closestDist = Float.MAX_VALUE

        for (obj in renderer.scene.objects) {
            if (!obj.isVisible || obj.isLocked) continue

            val radius = when (obj.type) {
                ObjectType.MESH -> (obj.transform.scale.length() * 0.55f).coerceAtLeast(0.4f)
                ObjectType.LIGHT -> 0.6f
                else -> 0.5f
            }

            val hitDist = ray.intersectSphere(obj.transform.position, radius)
            if (hitDist != null && hitDist < closestDist) {
                closestDist = hitDist
                closestObjId = obj.id
            }
        }

        renderer.selectedObjectId = closestObjId
        onObjectSelected(closestObjId)
    }
}
