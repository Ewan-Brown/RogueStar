#version 330

// Incoming vertex position
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 color;

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

    gl_Position = proj * (view * (model  * vec4(position, 1)));
    interpolatedColor = color;
}

