package com.example.engine.renderer

object PbrShaders {

    // =========================================================================
    // 1. SHADOW MAP DEPTH SHADERS
    // =========================================================================
    const val SHADOW_DEPTH_VERTEX = """
        uniform mat4 u_LightSpaceMVP;
        attribute vec3 a_Position;
        
        void main() {
            gl_Position = u_LightSpaceMVP * vec4(a_Position, 1.0);
        }
    """

    const val SHADOW_DEPTH_FRAGMENT = """
        precision mediump float;
        void main() {
            // Write normalized depth to color buffer for platforms without depth textures
            // gl_FragCoord.z is automatically written to depth buffer
        }
    """

    // =========================================================================
    // 2. MAIN COOK-TORRANCE GGX PBR SHADERS (Blender Eevee style)
    // =========================================================================
    const val PBR_VERTEX_SHADER = """
        uniform mat4 u_MVPMatrix;
        uniform mat4 u_ModelMatrix;
        uniform mat4 u_ViewMatrix;
        uniform mat4 u_LightSpaceMatrix;
        
        attribute vec3 a_Position;
        attribute vec3 a_Normal;
        attribute vec2 a_TexCoord;
        
        varying vec3 v_PositionWorld;
        varying vec3 v_PositionView;
        varying vec3 v_NormalWorld;
        varying vec2 v_TexCoord;
        varying vec4 v_ShadowCoord;
        
        void main() {
            vec4 posWorld = u_ModelMatrix * vec4(a_Position, 1.0);
            v_PositionWorld = posWorld.xyz;
            v_PositionView = (u_ViewMatrix * posWorld).xyz;
            
            // Calculate normal in world space
            v_NormalWorld = normalize((u_ModelMatrix * vec4(a_Normal, 0.0)).xyz);
            v_TexCoord = a_TexCoord;
            
            // Transform position to light space for shadow mapping
            v_ShadowCoord = u_LightSpaceMatrix * posWorld;
            
            gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
        }
    """

    const val PBR_FRAGMENT_SHADER = """
        precision highp float;
        
        #define PI 3.14159265359
        
        // Camera & Matrices
        uniform vec3 u_CameraPos;
        
        // Material Properties
        uniform vec4 u_BaseColor;
        uniform float u_Metallic;
        uniform float u_Roughness;
        uniform float u_Specular;
        uniform vec4 u_EmissionColor;
        uniform float u_EmissionIntensity;
        uniform float u_Transmission;
        uniform float u_IOR;
        uniform float u_ClearCoat;
        uniform float u_ClearCoatRoughness;
        uniform float u_Sheen;
        uniform float u_Anisotropy;
        uniform float u_AO;
        
        // Directional Sun Light
        uniform vec3 u_SunDir;
        uniform vec4 u_SunColor;
        uniform float u_SunIntensity;
        
        // Point Light
        uniform vec3 u_PointLightPos;
        uniform vec4 u_PointLightColor;
        uniform float u_PointLightIntensity;
        uniform float u_PointLightRange;
        
        // Ambient / IBL
        uniform vec4 u_AmbientColor;
        uniform float u_AmbientIntensity;
        uniform int u_ViewportThemeId;
        
        // Shadows
        uniform sampler2D u_ShadowMap;
        uniform float u_ShadowBias;
        uniform int u_ShadowsEnabled;
        uniform int u_ContactShadowsEnabled;
        
        // Selection Rim
        uniform float u_IsSelected;
        
        varying vec3 v_PositionWorld;
        varying vec3 v_PositionView;
        varying vec3 v_NormalWorld;
        varying vec2 v_TexCoord;
        varying vec4 v_ShadowCoord;

        // --- PBR Functions ---

        // Normal Distribution Function: GGX / Trowbridge-Reitz
        float DistributionGGX(vec3 N, vec3 H, float roughness) {
            float a = roughness * roughness;
            float a2 = a * a;
            float NdotH = max(dot(N, H), 0.0);
            float NdotH2 = NdotH * NdotH;
            
            float nom = a2;
            float denom = (NdotH2 * (a2 - 1.0) + 1.0);
            denom = PI * denom * denom;
            
            return nom / max(denom, 0.000001);
        }

        // Geometric Shadowing: Smith's Schlick-GGX
        float GeometrySchlickGGX(float NdotV, float roughness) {
            float r = (roughness + 1.0);
            float k = (r * r) / 8.0;
            
            float nom = NdotV;
            float denom = NdotV * (1.0 - k) + k;
            
            return nom / max(denom, 0.000001);
        }

        float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness) {
            float NdotV = max(dot(N, V), 0.0);
            float NdotL = max(dot(N, L), 0.0);
            float ggx2 = GeometrySchlickGGX(NdotV, roughness);
            float ggx1 = GeometrySchlickGGX(NdotL, roughness);
            
            return ggx1 * ggx2;
        }

        // Fresnel: Schlick's approximation
        vec3 FresnelSchlick(float cosTheta, vec3 F0) {
            return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
        }

        // Fresnel with Roughness for IBL
        vec3 FresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness) {
            return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
        }

        // Charlie Sheen (velvet/cloth microfibers)
        float CharlieSheen(float NdotH, float roughness) {
            float invR = 1.0 / max(roughness, 0.001);
            float cos2h = NdotH * NdotH;
            float sin2h = max(1.0 - cos2h, 0.0078125);
            return (2.0 + invR) * pow(sin2h, invR * 0.5) / (2.0 * PI);
        }

        // High-Quality Percentage-Closer-Filtering (PCF) Shadows
        float CalculateShadow(vec4 shadowCoord, vec3 N, vec3 L) {
            if (u_ShadowsEnabled == 0) return 1.0;
            
            // Perspective divide
            vec3 projCoords = shadowCoord.xyz / shadowCoord.w;
            // Map from [-1, 1] to [0, 1]
            projCoords = projCoords * 0.5 + 0.5;
            
            if (projCoords.z > 1.0 || projCoords.x < 0.0 || projCoords.x > 1.0 || projCoords.y < 0.0 || projCoords.y > 1.0) {
                return 1.0;
            }
            
            float currentDepth = projCoords.z;
            
            // Slope-scaled adaptive bias to prevent shadow acne and shimmering
            float cosTheta = max(dot(N, L), 0.0);
            float bias = max(u_ShadowBias * (1.0 - cosTheta), u_ShadowBias * 0.2);
            
            float shadow = 0.0;
            vec2 texelSize = vec2(1.0 / 1024.0);
            
            // 9-tap Poisson / Kernel PCF filter
            for (int x = -1; x <= 1; ++x) {
                for (int y = -1; y <= 1; ++y) {
                    float pcfDepth = texture2D(u_ShadowMap, projCoords.xy + vec2(float(x), float(y)) * texelSize).r;
                    shadow += currentDepth - bias > pcfDepth ? 0.0 : 1.0;
                }
            }
            shadow /= 9.0;
            
            return shadow;
        }

        // Spherical Harmonics & Environment Reflection IBL approximation
        vec3 SampleEnvironmentIBL(vec3 R, float roughness) {
            // Environment sky gradient based on viewport theme
            vec3 skyTop, skyHorizon, skyGround;
            if (u_ViewportThemeId == 0) { // Dark Studio
                skyTop = vec3(0.18, 0.22, 0.28);
                skyHorizon = vec3(0.28, 0.32, 0.38);
                skyGround = vec3(0.08, 0.09, 0.11);
            } else if (u_ViewportThemeId == 1) { // Blender 3D
                skyTop = vec3(0.24, 0.27, 0.32);
                skyHorizon = vec3(0.35, 0.38, 0.44);
                skyGround = vec3(0.12, 0.13, 0.15);
            } else if (u_ViewportThemeId == 2) { // Maya Slate
                skyTop = vec3(0.15, 0.20, 0.26);
                skyHorizon = vec3(0.26, 0.32, 0.40);
                skyGround = vec3(0.06, 0.08, 0.12);
            } else if (u_ViewportThemeId == 3) { // Cinema Charcoal
                skyTop = vec3(0.12, 0.14, 0.17);
                skyHorizon = vec3(0.22, 0.24, 0.27);
                skyGround = vec3(0.04, 0.05, 0.06);
            } else { // Light Studio
                skyTop = vec3(0.92, 0.95, 0.98);
                skyHorizon = vec3(0.85, 0.88, 0.92);
                skyGround = vec3(0.65, 0.68, 0.72);
            }
            
            float t = R.y * 0.5 + 0.5;
            vec3 envColor = mix(skyGround, mix(skyHorizon, skyTop, clamp(R.y * 1.5, 0.0, 1.0)), clamp(t, 0.0, 1.0));
            
            // Add subtle studio soft-box highlights on reflection vector
            vec3 lightBox1 = vec3(0.6, 0.7, 0.4);
            float box1 = pow(max(dot(R, normalize(lightBox1)), 0.0), mix(64.0, 4.0, roughness));
            
            vec3 lightBox2 = vec3(-0.7, 0.5, -0.5);
            float box2 = pow(max(dot(R, normalize(lightBox2)), 0.0), mix(32.0, 2.0, roughness));
            
            envColor += vec3(1.2, 1.15, 1.1) * box1 * (1.0 - roughness * 0.5) * 1.5;
            envColor += vec3(0.8, 0.9, 1.1) * box2 * (1.0 - roughness * 0.5) * 0.8;
            
            return envColor;
        }

        void main() {
            vec3 N = normalize(v_NormalWorld);
            vec3 V = normalize(u_CameraPos - v_PositionWorld);
            vec3 R = reflect(-V, N);
            
            float NdotV = max(dot(N, V), 0.0001);
            
            // Base F0 Dielectric (0.04 for standard dielectrics, scaled by specular) vs Metallic
            float dielectricF0 = 0.08 * u_Specular;
            vec3 F0 = mix(vec3(dielectricF0), u_BaseColor.rgb, u_Metallic);
            
            // -------------------------------------------------------------
            // 1. DIRECTIONAL SUN LIGHT (PBR Cook-Torrance)
            // -------------------------------------------------------------
            vec3 L_sun = normalize(-u_SunDir);
            vec3 H_sun = normalize(V + L_sun);
            float NdotL_sun = max(dot(N, L_sun), 0.0);
            
            vec3 directLighting = vec3(0.0);
            
            if (NdotL_sun > 0.0) {
                // Cook-Torrance Specular BRDF terms
                float D_sun = DistributionGGX(N, H_sun, u_Roughness);
                float G_sun = GeometrySmith(N, V, L_sun, u_Roughness);
                vec3 F_sun = FresnelSchlick(max(dot(H_sun, V), 0.0), F0);
                
                vec3 nominator_sun = D_sun * G_sun * F_sun;
                float denominator_sun = 4.0 * NdotV * NdotL_sun;
                vec3 specular_sun = nominator_sun / max(denominator_sun, 0.0001);
                
                // Energy conservation: kD (diffuse) + kS (specular) = 1.0
                vec3 kS_sun = F_sun;
                vec3 kD_sun = (vec3(1.0) - kS_sun) * (1.0 - u_Metallic) * (1.0 - u_Transmission);
                
                // Diffuse Lambert
                vec3 diffuse_sun = kD_sun * u_BaseColor.rgb / PI;
                
                // Sheen contribution (velvety highlight)
                if (u_Sheen > 0.0) {
                    float D_sheen = CharlieSheen(max(dot(N, H_sun), 0.0), u_Roughness);
                    specular_sun += vec3(D_sheen * u_Sheen * 0.25);
                }
                
                // Clearcoat layer
                if (u_ClearCoat > 0.0) {
                    float D_cc = DistributionGGX(N, H_sun, u_ClearCoatRoughness);
                    float G_cc = GeometrySmith(N, V, L_sun, u_ClearCoatRoughness);
                    vec3 F_cc = FresnelSchlick(max(dot(H_sun, V), 0.0), vec3(0.04));
                    vec3 ccSpecular = (D_cc * G_cc * F_cc) / max(4.0 * NdotV * NdotL_sun, 0.0001);
                    specular_sun += ccSpecular * u_ClearCoat;
                }
                
                // Calculate Shadow with PCF
                float shadow = CalculateShadow(v_ShadowCoord, N, L_sun);
                
                vec3 radiance_sun = u_SunColor.rgb * u_SunIntensity;
                directLighting += (diffuse_sun + specular_sun) * radiance_sun * NdotL_sun * shadow;
            }
            
            // -------------------------------------------------------------
            // 2. POINT LIGHT (Physical Inverse-Square Attenuation PBR)
            // -------------------------------------------------------------
            vec3 pointLightDir = u_PointLightPos - v_PositionWorld;
            float pointDist = length(pointLightDir);
            
            if (pointDist < u_PointLightRange) {
                vec3 L_point = normalize(pointLightDir);
                vec3 H_point = normalize(V + L_point);
                float NdotL_pt = max(dot(N, L_point), 0.0);
                
                if (NdotL_pt > 0.0) {
                    // Physical attenuation
                    float atten = clamp(1.0 - pow(pointDist / u_PointLightRange, 4.0), 0.0, 1.0);
                    atten = (atten * atten) / (pointDist * pointDist + 1.0);
                    
                    float D_pt = DistributionGGX(N, H_point, u_Roughness);
                    float G_pt = GeometrySmith(N, V, L_point, u_Roughness);
                    vec3 F_pt = FresnelSchlick(max(dot(H_point, V), 0.0), F0);
                    
                    vec3 spec_pt = (D_pt * G_pt * F_pt) / max(4.0 * NdotV * NdotL_pt, 0.0001);
                    vec3 kS_pt = F_pt;
                    vec3 kD_pt = (vec3(1.0) - kS_pt) * (1.0 - u_Metallic) * (1.0 - u_Transmission);
                    vec3 diff_pt = kD_pt * u_BaseColor.rgb / PI;
                    
                    vec3 radiance_pt = u_PointLightColor.rgb * u_PointLightIntensity * atten * 10.0;
                    directLighting += (diff_pt + spec_pt) * radiance_pt * NdotL_pt;
                }
            }
            
            // -------------------------------------------------------------
            // 3. IMAGE-BASED LIGHTING (IBL) & AMBIENT
            // -------------------------------------------------------------
            vec3 F_ibl = FresnelSchlickRoughness(NdotV, F0, u_Roughness);
            vec3 kS_ibl = F_ibl;
            vec3 kD_ibl = (1.0 - kS_ibl) * (1.0 - u_Metallic) * (1.0 - u_Transmission);
            
            // Irradiance diffuse from ambient
            vec3 diffuseIBL = u_AmbientColor.rgb * u_AmbientIntensity * u_BaseColor.rgb * kD_ibl;
            
            // Specular IBL reflection with roughness filtering
            vec3 envReflection = SampleEnvironmentIBL(R, u_Roughness);
            vec3 specularIBL = envReflection * F_ibl * (1.0 - u_Roughness * 0.5);
            
            // Glass Transmission & Refraction
            if (u_Transmission > 0.0) {
                // Snell's Law refraction ray
                float eta = 1.0 / max(u_IOR, 1.0);
                vec3 refractRay = refract(-V, N, eta);
                if (length(refractRay) < 0.001) refractRay = R; // Total internal reflection
                
                vec3 refractedEnv = SampleEnvironmentIBL(refractRay, u_Roughness * 0.5);
                vec3 glassTransmissionColor = refractedEnv * u_BaseColor.rgb * (1.0 - F_ibl);
                diffuseIBL = mix(diffuseIBL, glassTransmissionColor, u_Transmission);
            }
            
            // Apply Ambient Occlusion
            vec3 ambientLighting = (diffuseIBL + specularIBL) * u_AO;
            
            // -------------------------------------------------------------
            // 4. EMISSION & HDR COLOR SYNTHESIS
            // -------------------------------------------------------------
            vec3 emission = u_EmissionColor.rgb * u_EmissionIntensity;
            
            // Composite HDR color
            vec3 colorHDR = directLighting + ambientLighting + emission;
            
            // Selection Rim Highlight for studio workflow
            if (u_IsSelected > 0.5) {
                float rim = 1.0 - NdotV;
                rim = smoothstep(0.4, 0.95, rim);
                colorHDR = mix(colorHDR, vec3(0.0, 0.75, 1.0) * 1.5, rim * 0.85);
            }
            
            float finalAlpha = mix(u_BaseColor.a, 1.0, u_Metallic);
            if (u_Transmission > 0.0) {
                finalAlpha = mix(u_BaseColor.a, 0.35 + 0.65 * (1.0 - NdotV), u_Transmission);
            }
            
            gl_FragColor = vec4(colorHDR, finalAlpha);
        }
    """

    // =========================================================================
    // 3. POST-PROCESSING: FULLSCREEN QUAD VERTEX SHADER
    // =========================================================================
    const val POST_QUAD_VERTEX = """
        attribute vec2 a_Position;
        attribute vec2 a_TexCoord;
        varying vec2 v_TexCoord;
        
        void main() {
            v_TexCoord = a_TexCoord;
            gl_Position = vec4(a_Position, 0.0, 1.0);
        }
    """

    // =========================================================================
    // 4. POST-PROCESSING: HDR BLOOM BRIGHT-PASS & BLUR SHADERS
    // =========================================================================
    const val BLOOM_BRIGHT_PASS_FRAGMENT = """
        precision mediump float;
        uniform sampler2D u_SceneTexture;
        uniform float u_BloomThreshold;
        varying vec2 v_TexCoord;
        
        void main() {
            vec4 color = texture2D(u_SceneTexture, v_TexCoord);
            // Calculate perceived luminance
            float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
            if (luminance > u_BloomThreshold) {
                float factor = clamp((luminance - u_BloomThreshold) / max(u_BloomThreshold * 0.5, 0.01), 0.0, 3.0);
                gl_FragColor = vec4(color.rgb * factor, color.a);
            } else {
                gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
            }
        }
    """

    const val GAUSSIAN_BLUR_FRAGMENT = """
        precision mediump float;
        uniform sampler2D u_Texture;
        uniform vec2 u_Direction; // (1/width, 0) for horiz, (0, 1/height) for vert
        varying vec2 v_TexCoord;
        
        void main() {
            vec4 result = vec4(0.0);
            // 9-tap Gaussian weights
            float weights[5];
            weights[0] = 0.2270270270;
            weights[1] = 0.1945945946;
            weights[2] = 0.1216216216;
            weights[3] = 0.0540540541;
            weights[4] = 0.0162162162;
            
            result += texture2D(u_Texture, v_TexCoord) * weights[0];
            for (int i = 1; i < 5; i++) {
                vec2 offset = u_Direction * float(i) * 1.5;
                result += texture2D(u_Texture, v_TexCoord + offset) * weights[i];
                result += texture2D(u_Texture, v_TexCoord - offset) * weights[i];
            }
            gl_FragColor = result;
        }
    """

    // =========================================================================
    // 5. POST-PROCESSING: FINAL COMPOSITE & ACES FILMIC TONE MAPPING
    // =========================================================================
    const val POST_COMPOSITE_FRAGMENT = """
        precision highp float;
        
        uniform sampler2D u_SceneTexture;
        uniform sampler2D u_BloomTexture;
        
        // Post-Processing Controls
        uniform float u_BloomIntensity;
        uniform int u_BloomEnabled;
        uniform float u_Exposure;
        uniform float u_Contrast;
        uniform float u_Saturation;
        uniform float u_VignetteStrength;
        uniform float u_VignetteRadius;
        uniform int u_ToneMappingMode; // 0 = ACES Filmic, 1 = Reinhard, 2 = Linear
        uniform int u_FXAAEnabled;
        uniform vec2 u_ScreenSize;
        
        varying vec2 v_TexCoord;
        
        // ACES Filmic Tone Mapping Curve (Blender Eevee / Film standard)
        vec3 ToneMapACESFilmic(vec3 x) {
            float a = 2.51;
            float b = 0.03;
            float c = 2.43;
            float d = 0.59;
            float e = 0.14;
            return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
        }
        
        // Reinhard Tone Mapping
        vec3 ToneMapReinhard(vec3 x) {
            return x / (x + vec3(1.0));
        }

        // Fast Approximate Anti-Aliasing (FXAA) Edge Sample
        vec3 SampleFXAA(sampler2D tex, vec2 uv, vec2 rcpFrame) {
            vec3 rgbNW = texture2D(tex, uv + vec2(-1.0, -1.0) * rcpFrame).rgb;
            vec3 rgbNE = texture2D(tex, uv + vec2( 1.0, -1.0) * rcpFrame).rgb;
            vec3 rgbSW = texture2D(tex, uv + vec2(-1.0,  1.0) * rcpFrame).rgb;
            vec3 rgbSE = texture2D(tex, uv + vec2( 1.0,  1.0) * rcpFrame).rgb;
            vec3 rgbM  = texture2D(tex, uv).rgb;
            
            vec3 luma = vec3(0.299, 0.587, 0.114);
            float lumaNW = dot(rgbNW, luma);
            float lumaNE = dot(rgbNE, luma);
            float lumaSW = dot(rgbSW, luma);
            float lumaSE = dot(rgbSE, luma);
            float lumaM  = dot(rgbM,  luma);
            
            float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
            float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));
            
            vec2 dir;
            dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
            dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));
            
            float dirReduce = max((lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * 0.125), 0.0078125);
            float rcpDirMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);
            
            dir = min(vec2(8.0, 8.0), max(vec2(-8.0, -8.0), dir * rcpDirMin)) * rcpFrame;
            
            vec3 rgbA = 0.5 * (
                texture2D(tex, uv + dir * (1.0/3.0 - 0.5)).rgb +
                texture2D(tex, uv + dir * (2.0/3.0 - 0.5)).rgb
            );
            vec3 rgbB = rgbA * 0.5 + 0.25 * (
                texture2D(tex, uv + dir * -0.5).rgb +
                texture2D(tex, uv + dir * 0.5).rgb
            );
            
            float lumaB = dot(rgbB, luma);
            if ((lumaB < lumaMin) || (lumaB > lumaMax)) {
                return rgbA;
            } else {
                return rgbB;
            }
        }
        
        void main() {
            vec3 color;
            if (u_FXAAEnabled != 0 && u_ScreenSize.x > 0.0) {
                vec2 rcpFrame = vec2(1.0 / u_ScreenSize.x, 1.0 / u_ScreenSize.y);
                color = SampleFXAA(u_SceneTexture, v_TexCoord, rcpFrame);
            } else {
                color = texture2D(u_SceneTexture, v_TexCoord).rgb;
            }
            
            // Additive HDR Bloom with energy distribution
            if (u_BloomEnabled != 0) {
                vec3 bloom = texture2D(u_BloomTexture, v_TexCoord).rgb;
                color += bloom * u_BloomIntensity;
            }
            
            // Exposure adjustment (EV)
            color *= pow(2.0, u_Exposure);
            
            // Tone Mapping: HDR -> LDR
            if (u_ToneMappingMode == 0) {
                color = ToneMapACESFilmic(color);
            } else if (u_ToneMappingMode == 1) {
                color = ToneMapReinhard(color);
            }
            
            // Contrast curve
            color = clamp((color - vec3(0.5)) * u_Contrast + vec3(0.5), 0.0, 1.0);
            
            // Saturation adjustment
            float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
            color = mix(vec3(gray), color, u_Saturation);
            
            // Vignette effect
            if (u_VignetteStrength > 0.0) {
                vec2 uvDist = v_TexCoord - vec2(0.5);
                float dist = length(uvDist);
                float vignette = smoothstep(u_VignetteRadius, u_VignetteRadius - 0.35, dist);
                color = mix(color * (1.0 - u_VignetteStrength), color, vignette);
            }
            
            // Gamma Correction (Linear -> sRGB for display)
            color = pow(color, vec3(1.0 / 2.2));
            
            gl_FragColor = vec4(color, 1.0);
        }
    """
}
