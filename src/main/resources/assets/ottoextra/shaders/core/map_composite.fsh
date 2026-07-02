#version 330

// Portierung des OttoMap-Composite-Shaders auf die 1.21.11-Pipeline (UBO statt
// Einzel-Uniforms). Sampler0 = gemalte Karte (Details), Sampler1 = Screen-Copy
// (Xaero-Terrain), Sampler2 = obere Ebene (Beschriftung), Sampler3 = Karte ohne
// Details. Luma-Maske: schwarzes (unerkundetes) Terrain zeigt die gemalte Karte,
// erkundetes bleibt sichtbar — mit weichem 40-Tap-Rand.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;

layout(std140) uniform MapParams {
    float FadeScale;
    float HudGuardFB;
    float NightBrightness;
    float DetailBlend;  // 1.0 = volle Details, 0.0 = ohne Details
    float UpperAlpha;   // obere Ebene sichtbar
    float LowerAlpha;   // 1.0 = volle Farben, 0.0 = abgedunkelt
    float OverallAlpha; // Gesamt-Fade (hoher Zoom)
    float FullCover;    // 1.0 = Karte flächendeckend (Xaero-Terrain verdeckt)
};

in vec2 texCoord0;
out vec4 fragColor;

float isLoaded(vec2 screenUV) {
    vec3 c = texture(Sampler1, screenUV).rgb;
    return step(0.03, dot(c, vec3(0.299, 0.587, 0.114)));
}

vec4 sampleLower(vec2 uv) {
    return mix(texture(Sampler3, uv), texture(Sampler0, uv), DetailBlend);
}

vec4 sampleComposite(vec2 uv) {
    vec4 lower = sampleLower(uv);
    vec4 upper = texture(Sampler2, uv);
    return vec4(mix(lower.rgb, upper.rgb, upper.a * UpperAlpha), lower.a);
}

void main() {
    vec4 mapColor = sampleComposite(texCoord0);

    vec2 screenSize = vec2(textureSize(Sampler1, 0));
    vec2 screenUV = gl_FragCoord.xy / screenSize;
    vec3 screenColor = texture(Sampler1, screenUV).rgb;
    float screenLum = dot(screenColor, vec3(0.299, 0.587, 0.114));

    float cover = clamp(FullCover, 0.0, 1.0);

    // HUD-Schutzbaender (Xaero-Koordinaten oben / Zoom unten). Nur im
    // Masken-Modus: bei Vollabdeckung ist das Terrain ueberall hell und die
    // Luma-Heuristik wuerde das ganze Band aufreissen.
    float normX = gl_FragCoord.x / screenSize.x;
    bool inTopBand = normX > 0.30 && normX < 0.70;
    bool inBotBand = normX > 0.35 && normX < 0.65;
    bool topHit = gl_FragCoord.y < HudGuardFB && inTopBand && screenLum > 0.15;
    bool botHit = (screenSize.y - gl_FragCoord.y) < (HudGuardFB * 4.0) && inBotBand && screenLum > 0.15;
    if ((topHit || botHit) && cover <= 0.5) {
        fragColor = vec4(0.0);
        return;
    }

    float lowerBrightness = mix(0.92, 1.0, clamp(LowerAlpha, 0.0, 1.0));

    // Ausserhalb der Karten-Geobounds: stabile Fuellfarbe
    if (texCoord0.x < 0.0 || texCoord0.x > 1.0 || texCoord0.y < 0.0 || texCoord0.y > 1.0) {
        vec3 fillBase = vec3(0.431, 0.481, 0.543);
        fragColor = vec4(fillBase * NightBrightness * lowerBrightness, OverallAlpha);
        return;
    }

    // Weicher Rand: 5 Ringe x 8 Richtungen im Screen-Copy abtasten
    vec2 px = 1.0 / screenSize;
    float loaded = 0.0;

    float fs = clamp(FadeScale, 0.2, 4.0);
    float r1 = 8.0 * fs; float r2 = 16.0 * fs; float r3 = 24.0 * fs;
    float r4 = 32.0 * fs; float r5 = 40.0 * fs;

    vec2 dirs[8] = vec2[8](
        vec2(1.0, 0.0), vec2(0.707, 0.707), vec2(0.0, 1.0), vec2(-0.707, 0.707),
        vec2(-1.0, 0.0), vec2(-0.707, -0.707), vec2(0.0, -1.0), vec2(0.707, -0.707));
    float radii[5] = float[5](r1, r2, r3, r4, r5);

    for (int ri = 0; ri < 5; ri++) {
        for (int di = 0; di < 8; di++) {
            loaded += isLoaded(screenUV + dirs[di] * radii[ri] * px);
        }
    }

    float ratio = loaded / 40.0;

    if (ratio > 0.95 && screenLum > 0.05 && cover <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    float nearUnloaded = 1.0 - ratio;
    float proximityAlpha = smoothstep(0.0, 0.45, nearUnloaded);
    // FullCover uebersteuert die Erkundungs-Maske: Karte ueberall deckend
    float alpha = max(proximityAlpha, cover);

    vec3 finalColor = mapColor.rgb * NightBrightness * lowerBrightness;
    fragColor = vec4(finalColor, alpha * OverallAlpha * mapColor.a);
}
