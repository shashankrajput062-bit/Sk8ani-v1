package com.example.engine.renderer

import android.opengl.GLES20
import android.util.Log

object ShaderUtils {
    private const val TAG = "ShaderUtils"

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Could not compile shader $type:\n$error\nSource:\n$shaderCode")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        if (vertexShader == 0) return 0

        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) return 0

        val program = GLES20.glCreateProgram()
        if (program == 0) return 0

        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "Could not link program:\n$error")
            GLES20.glDeleteProgram(program)
            return 0
        }
        return program
    }

    // --- Shaders ---

    const val MESH_VERTEX_SHADER = """
        uniform mat4 u_MVPMatrix;
        uniform mat4 u_MVMatrix;
        uniform mat4 u_ModelMatrix;
        
        attribute vec3 a_Position;
        attribute vec3 a_Normal;
        attribute vec2 a_TexCoord;
        
        varying vec3 v_PositionWorld;
        varying vec3 v_NormalWorld;
        varying vec2 v_TexCoord;
        
        void main() {
            vec4 posWorld = u_ModelMatrix * vec4(a_Position, 1.0);
            v_PositionWorld = posWorld.xyz;
            // Normal transform (assuming uniform scale or model matrix rotation)
            v_NormalWorld = normalize((u_ModelMatrix * vec4(a_Normal, 0.0)).xyz);
            v_TexCoord = a_TexCoord;
            gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
        }
    """

    const val MESH_FRAGMENT_SHADER = """
        precision mediump float;
        
        uniform vec3 u_CameraPos;
        uniform vec4 u_BaseColor;
        uniform float u_Metallic;
        uniform float u_Roughness;
        uniform float u_Specular;
        uniform vec4 u_EmissionColor;
        uniform float u_EmissionIntensity;
        
        // Sun Light
        uniform vec3 u_SunDir;
        uniform vec4 u_SunColor;
        uniform float u_SunIntensity;
        
        // Point Light
        uniform vec3 u_PointLightPos;
        uniform vec4 u_PointLightColor;
        uniform float u_PointLightIntensity;
        uniform float u_PointLightRange;
        
        // Ambient
        uniform vec4 u_AmbientColor;
        uniform float u_AmbientIntensity;
        
        // Selected highlight flag
        uniform float u_IsSelected;
        
        varying vec3 v_PositionWorld;
        varying vec3 v_NormalWorld;
        varying vec2 v_TexCoord;
        
        void main() {
            vec3 N = normalize(v_NormalWorld);
            vec3 V = normalize(u_CameraPos - v_PositionWorld);
            
            // Ambient component
            vec3 ambient = u_AmbientColor.rgb * u_AmbientIntensity * u_BaseColor.rgb;
            
            // Directional Sun lighting (Diffuse + Specular approximation)
            vec3 L_sun = normalize(-u_SunDir);
            float NdotL_sun = max(dot(N, L_sun), 0.0);
            vec3 diffuse_sun = u_SunColor.rgb * (u_SunIntensity * NdotL_sun) * u_BaseColor.rgb;
            
            // Specular / Metallic highlight (Blinn-Phong)
            vec3 H_sun = normalize(L_sun + V);
            float specPower = mix(64.0, 4.0, u_Roughness);
            float specFactor_sun = pow(max(dot(N, H_sun), 0.0), specPower);
            vec3 specColor_sun = mix(vec3(u_Specular), u_BaseColor.rgb, u_Metallic);
            vec3 specular_sun = u_SunColor.rgb * specFactor_sun * specColor_sun * (1.0 - u_Roughness * 0.7);
            
            // Point Light
            vec3 pointDir = u_PointLightPos - v_PositionWorld;
            float pointDist = length(pointDir);
            vec3 L_point = normalize(pointDir);
            float pointAtten = max(1.0 - (pointDist / max(u_PointLightRange, 0.001)), 0.0);
            pointAtten = pointAtten * pointAtten;
            
            float NdotL_point = max(dot(N, L_point), 0.0);
            vec3 diffuse_point = u_PointLightColor.rgb * (u_PointLightIntensity * NdotL_point * pointAtten) * u_BaseColor.rgb;
            
            // Emission
            vec3 emission = u_EmissionColor.rgb * u_EmissionIntensity;
            
            // Final Color combination
            vec3 finalRgb = ambient + diffuse_sun + specular_sun + diffuse_point + emission;
            
            // Selection rim highlight
            if (u_IsSelected > 0.5) {
                float rim = 1.0 - max(dot(N, V), 0.0);
                rim = smoothstep(0.4, 0.95, rim);
                finalRgb = mix(finalRgb, vec3(0.0, 0.75, 1.0), rim * 0.85);
            }
            
            gl_FragColor = vec4(finalRgb, u_BaseColor.a);
        }
    """

    const val FLAT_COLOR_VERTEX_SHADER = """
        uniform mat4 u_MVPMatrix;
        attribute vec3 a_Position;
        void main() {
            gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
        }
    """

    const val FLAT_COLOR_FRAGMENT_SHADER = """
        precision mediump float;
        uniform vec4 u_Color;
        void main() {
            gl_FragColor = u_Color;
        }
    """
}
