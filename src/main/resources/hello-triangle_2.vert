#version 330
#include semantic.glsl

// Incoming vertex position
layout (location = 0) in vec2 position;
layout (location = 1) in vec3 color;
layout (location = 2) in vec2 instanced_position;

uniform GlobalMatrices
{
    mat4 view;
    mat4 proj;
};


// Uniform matrix from Model Space to camera (also known as view) Space
uniform mat4 model;


// Outgoing color for the next shader (fragment in this case)
out vec3 interpolatedColor;


void main() {

    gl_Position = proj * (view * (model * (vec4(0, gl_InstanceID, 0, 0) + vec4(instanced_position + position, 0, 1))));
    interpolatedColor = color;
}