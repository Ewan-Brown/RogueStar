#version 330
#include semantic.glsl

// Incoming vertex position
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 color;
layout (location = 2) in vec4 transform1;
layout (location = 3) in vec4 transform2;
layout (location = 4) in vec4 transform3;
layout (location = 5) in vec4 transform4;


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


    mat4 transform = mat4(
    transform1,
    transform2,
    transform3,
    transform4);

    mat4 identity = mat4(
    1,0,0,0,
    0,1,0,0,
    0,0,1,0,
    0,0,0,1);

    mat4 premat = proj * view * model * transform;

    gl_Position =  premat * (vec4(position, 1));
    interpolatedColor = color;
}

