#version 330

// Incoming interpolated (between vertices) color from the vertex shader.
in vec3 interpolatedColor;
// Outgoing final color.
layout (location = 0) out vec4 outputColor;
//layout (location = 5) in float instanced_health;

in float health_out;
in vec2 xyVarying;

void main()
{
    float a = cos(xyVarying.x*10.0);
    float b = 1.0 - a;
    float d = a + b*health_out;
    // We simply pad the interpolatedColor to vec4
    outputColor = vec4(d, 0, 0, 1) ;
}