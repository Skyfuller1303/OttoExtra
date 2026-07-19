#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

// Dezente Untersuchungslinse: Die Mitte bleibt vollständig scharf. Nur zum
// äußeren Rand hin wird leicht weichgezeichnet und minimal abgedunkelt.
void main() {
    vec2 oneTexel = 1.0 / InSize;
    float d = distance(texCoord, vec2(0.5));
    float edge = smoothstep(0.30, 0.73, d);
    float radius = edge * 1.35;

    vec4 c = texture(InSampler, texCoord) * 0.56;
    c += texture(InSampler, texCoord + oneTexel * vec2( radius, 0.0)) * 0.11;
    c += texture(InSampler, texCoord + oneTexel * vec2(-radius, 0.0)) * 0.11;
    c += texture(InSampler, texCoord + oneTexel * vec2(0.0,  radius)) * 0.11;
    c += texture(InSampler, texCoord + oneTexel * vec2(0.0, -radius)) * 0.11;

    float vignette = 1.0 - edge * 0.075;
    fragColor = vec4(c.rgb * vignette, 1.0);
}
