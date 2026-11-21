#version 330
//? if >= 26.3
//#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform MotionBlurConfig {
    mat4 DeltaRotation;
    vec4 Params;
    vec4 Movement;
    vec4 DepthParams;
};

//$ layout '0' 'in' >> vec
in vec2 texCoord;
//$ layout '0' 'out' >> vec
out vec4 fragColor;

float noise(vec2 pos) {
    return fract(52.9829189 * fract(0.06711056 * pos.x + 0.00583715 * pos.y));
}

vec2 project(vec3 ray, vec2 tan, float strength, float cap) {
    float invZ = -1.0 / min(ray.z, -0.05);
    vec2 ndc0 = ray.xy * invZ / tan;
    vec2 uv0 = ndc0 * 0.5 + 0.5;

    vec2 v = (texCoord - uv0) * strength;
    float speed = length(v);
    if (speed > cap && speed > 0.0) v *= cap / speed;

    return v;
}

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec2 tan = Params.zw;

    float depth = texture(DepthSampler, texCoord).r;
    float linear = DepthParams.x / max(depth * DepthParams.y + DepthParams.z, 0.00001);

    if (linear < 0.115) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    vec2 velocity = vec2(0.0);

    if (Params.x > 0.0) {
        vec3 ray = (DeltaRotation * vec4(ndc * tan, -1.0, 0.0)).xyz;

        velocity += project(ray, tan, Params.x, Params.y);
    }

    if (Movement.w > 0.0) {
        vec3 ray = vec3(ndc * tan, -1.0) * linear + Movement.xyz;

        velocity += project(ray, tan, Movement.w, DepthParams.w);
    }

    float speed = length(velocity);
    if (isnan(speed) || isinf(speed) || speed < 0.00005) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    vec3 color = vec3(0.0);
    float total = 0.0;
    float ign = noise(gl_FragCoord.xy);

    for (int i = 0; i < 12; i++) {
        float fi = float(i);
        float jitter = fract(ign + fi * 0.6180339887);
        float step = (fi - 6.0 + jitter) / 12.0;
        float weight = 0.8 + 0.2 * cos(6.2831853 * step);

        vec2 uv = clamp(texCoord + velocity * step, vec2(0.0), vec2(1.0));
        vec3 sample0 = texture(InSampler, uv).rgb;

        color += sample0 * sample0 * weight;
        total += weight;
    }

    fragColor = vec4(sqrt(color / total), 1.0);
}
