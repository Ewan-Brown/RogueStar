#version 330

// Incoming interpolated (between vertices) color from the vertex shader.
in vec3 interpolatedColor;
// Outgoing final color.
layout (location = 0) out vec4 outputColor;
void main()
{
    // We simply pad the interpolatedColor to vec4
    outputColor = vec4(interpolatedColor, 1);
}