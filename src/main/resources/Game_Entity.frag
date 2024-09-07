#version 330

// Incoming interpolated (between vertices) color from the vertex shader.
in vec3 interpolatedColor;
// Outgoing final color.
layout (location = 0) out vec4 outputColor;
//layout (location = 5) in float instanced_health;

in float health_out;
in vec2 xyVarying;

float random(vec2 st)
{
    return fract(sin(dot(st.xy, vec2(12.9898,78.233))) * 43758.5453123);
}

void main()
{
    int x = int(floor(xyVarying.x/0.2));
    int y = int(floor(xyVarying.y/0.2));
    float r = random(vec2(x,y));
    float s0 = r + health_out*2 - 1;
    float s0_clamped = clamp(s0, 0, 1);

    outputColor = vec4(interpolatedColor * s0_clamped, 1) ;
}