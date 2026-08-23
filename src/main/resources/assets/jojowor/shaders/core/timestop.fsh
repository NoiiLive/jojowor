#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D MaskSampler;

uniform mat4 InverseTransform;
uniform mat4 ForwardTransform;
uniform vec3 SphereCenter;
uniform vec4 EffectParams;
uniform vec4 RingParams;
uniform vec2 GrayRingParams;
uniform vec2 HueParams;
uniform vec4 DistortParams;
uniform float SkyDistance;

in vec2 texCoord;

out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);
const float EDGE_SOFTNESS = 0.35;
const float DESATURATION = 0.80;
const float DESATURATION_DARKEN = 0.88;
const float SKY_DEPTH = 0.9999;
const float MASK_CUTOFF = 0.35;
const float SECONDARY_WIDTH_SCALE = 0.6;
const int BLUR_SAMPLES = 14;
const float BLUR_TAIL_WEIGHT = 0.5;

vec3 reconstruct(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 position = InverseTransform * clip;
    return position.xyz / position.w;
}

vec2 project(vec3 position) {
    vec4 clip = ForwardTransform * vec4(position, 1.0);
    if (clip.w <= 0.0) {
        return vec2(-1000.0);
    }
    return (clip.xy / clip.w) * 0.5 + 0.5;
}

float sphereCoverage(vec3 direction, float surfaceDistance, bool sky, float radius) {
    float b = dot(direction, SphereCenter);
    float centerSqr = dot(SphereCenter, SphereCenter);
    float outside = centerSqr - radius * radius;
    float discriminant = b * b - outside;
    if (discriminant < 0.0) {
        return 0.0;
    }
    float root = sqrt(discriminant);
    float far = b + root;
    if (far <= 0.0) {
        return 0.0;
    }

    if (outside > 0.0) {
        float near = b - root;
        float perpendicular = sqrt(max(0.0, centerSqr - b * b));
        float silhouette = 1.0 - smoothstep(radius - EDGE_SOFTNESS, radius + EDGE_SOFTNESS, perpendicular);
        float behind = sky ? 1.0 : smoothstep(near - EDGE_SOFTNESS, near + EDGE_SOFTNESS, surfaceDistance);
        return silhouette * behind;
    }

    if (sky) {
        return 1.0;
    }
    return 1.0 - smoothstep(far - EDGE_SOFTNESS, far + EDGE_SOFTNESS, surfaceDistance);
}

float ringAt(float distance, float radius, float width, float strength) {
    if (strength <= 0.0 || width <= 0.0) {
        return 0.0;
    }
    float falloff = 1.0 - smoothstep(0.0, width, abs(distance - radius));
    return falloff * falloff * strength;
}

vec3 rgbToHsv(vec3 color) {
    vec4 k = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(color.bg, k.wz), vec4(color.gb, k.xy), step(color.b, color.g));
    vec4 q = mix(vec4(p.xyw, color.r), vec4(color.r, p.yzx), step(p.x, color.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsvToRgb(vec3 hsv) {
    vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(hsv.xxx + k.xyz) * 6.0 - k.www);
    return hsv.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), hsv.y);
}

vec3 shiftHue(vec3 color, float shift) {
    vec3 hsv = rgbToHsv(color);
    hsv.x = fract(hsv.x + shift);
    return hsvToRgb(hsv);
}

vec2 squiggle(vec3 position, float scale, float time) {
    float a = sin(position.x * scale + time * 1.7)
            + sin(position.z * scale * 1.3 - time * 1.1)
            + 0.5 * sin(position.y * scale * 2.7 + time * 2.3);
    float b = sin(position.y * scale * 1.1 + time * 1.3)
            + sin((position.x + position.z) * scale * 0.7 + time * 0.9)
            + 0.5 * sin(position.x * scale * 3.1 - time * 2.1);
    return vec2(a, b) * 0.4;
}

void main() {
    float rawDepth = texture(DepthSampler, texCoord).r;
    vec3 position = reconstruct(texCoord, rawDepth);

    float invertRadius = EffectParams.x;
    float grayRadius = EffectParams.y;
    float grayStrength = EffectParams.z;
    float invertStrength = EffectParams.w;

    bool sky = rawDepth >= SKY_DEPTH;
    float surfaceDistance = length(position);
    vec3 direction = surfaceDistance > 0.0001 ? position / surfaceDistance : vec3(0.0, 0.0, 1.0);

    float distance = sky ? SkyDistance : length(position - SphereCenter);

    float insideGray = sphereCoverage(direction, surfaceDistance, sky, grayRadius);
    float insideInvert = sphereCoverage(direction, surfaceDistance, sky, invertRadius);
    float mask = step(MASK_CUTOFF, texture(MaskSampler, texCoord).a);

    float distortion = insideInvert * (1.0 - mask);
    vec4 source;
    if (distortion > 0.0 && rawDepth < SKY_DEPTH) {
        float pull = DistortParams.x;
        float amount = DistortParams.y;
        float scale = DistortParams.z;
        float time = DistortParams.w;

        vec2 wave = squiggle(position, scale, time);
        vec2 waveOffset = wave * amount * distortion;
        float maxPull = pull * (0.6 + 0.5 * wave.x) * distortion;

        vec4 accumulated = vec4(0.0);
        float total = 0.0;
        for (int i = 0; i < BLUR_SAMPLES; i++) {
            float tap = float(i) / float(BLUR_SAMPLES - 1);
            float weight = mix(1.0, BLUR_TAIL_WEIGHT, tap);
            vec3 pulled = position + (SphereCenter - position) * maxPull * tap;
            vec2 projected = project(pulled);
            vec2 uv = projected.x > -999.0 ? projected : texCoord;
            uv = clamp(uv + waveOffset, vec2(0.0), vec2(1.0));
            accumulated += texture(DiffuseSampler, uv) * weight;
            total += weight;
        }
        source = accumulated / total;
    } else {
        source = texture(DiffuseSampler, texCoord);
    }
    vec3 color = source.rgb;

    float gray = clamp(insideGray * (1.0 - insideInvert) * grayStrength * (1.0 - mask), 0.0, 1.0);
    float luminance = dot(color, LUMA);
    vec3 drained = mix(color, vec3(luminance), DESATURATION) * DESATURATION_DARKEN;
    color = mix(color, drained, gray);

    float invert = clamp(insideInvert * invertStrength, 0.0, 1.0);
    if (invert > 0.0) {
        float hueShift = HueParams.x * 0.5 * (1.0 - cos(HueParams.y));
        vec3 inverted = shiftHue(vec3(1.0) - color, hueShift);
        color = mix(color, inverted, invert);
    }

    float secondaryRadius = invertRadius - RingParams.z;
    float ring = ringAt(distance, invertRadius, RingParams.x, RingParams.y)
            + ringAt(distance, secondaryRadius, RingParams.x * SECONDARY_WIDTH_SCALE,
                    RingParams.w * step(0.0, secondaryRadius))
            + ringAt(distance, grayRadius, GrayRingParams.x, GrayRingParams.y);
    color = mix(color, vec3(1.0), clamp(ring, 0.0, 1.0));

    fragColor = vec4(color, source.a);
}
