#version 330

// Incoming interpolated (between vertices) color from the vertex shader.
in vec3 interpolatedColor;

// Outgoing final color.
layout (location = 0) out vec4 outputColor;

uniform float time;
uniform mat4 cameraViewMatrix;

void main()
{
    outputColor = vec4(interpolatedColor, 1) ;
}