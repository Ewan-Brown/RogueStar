#version 330
#include semantic.glsl

// Incoming vertex position
layout (location = 0) in vec2 position;
layout (location = 1) in vec3 color;
layout (location = 2) in vec3 instanced_data;

uniform GlobalMatrices
{
    mat4 view;
    mat4 proj;
};


// Uniform matrix from Model Space to camera (also known as view) Space
uniform mat4 model;

// Outgoing color for the next shader (fragment in this case)
out vec3 interpolatedColor;

vec2 rotate(vec2 pos, float a) {
    return vec2(pos.x * cos(a) - pos.y * sin(a), pos.x * sin(a) + pos.y * cos(a));
}

void main() {
    vec2 instanced_pos = instanced_data.xy;
    float instanced_rot = instanced_data.z;


    gl_Position = proj * (view * (model  * vec4(instanced_pos + rotate(position, instanced_rot), 0, 1)));
    interpolatedColor = color;
}

