#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

// Edge-Blur / Tunnelblick: Mitte bleibt scharf, zum Rand hin zunehmend
// verschwommen. Der Blur-Radius skaliert mit dem Abstand zur Bildmitte.
void main() {
    vec2 oneTexel = 1.0 / InSize;
    float d = distance(texCoord, vec2(0.5));
    float edge = smoothstep(0.30, 0.82, d);
    float r = edge * 4.0;

    vec4 c = texture(InSampler, texCoord) * 0.40;
    c += texture(InSampler, texCoord + oneTexel * vec2( r, 0.0)) * 0.15;
    c += texture(InSampler, texCoord + oneTexel * vec2(-r, 0.0)) * 0.15;
    c += texture(InSampler, texCoord + oneTexel * vec2(0.0,  r)) * 0.15;
    c += texture(InSampler, texCoord + oneTexel * vec2(0.0, -r)) * 0.15;
    fragColor = vec4(c.rgb, 1.0);
}
