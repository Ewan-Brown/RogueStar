#version 330

// Incoming vertex position
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 color;
layout (location = 2) in vec3 instanced_data;

uniform GlobalMatrices
{
    mat4 view;
};

// Uniform matrix from Model Space to camera (also known as view) Space
uniform mat4 model;
// Outgoing color for the next shader (fragment in this case)
out vec3 interpolatedColor;

vec3 rotate(vec3 pos, float a) {
    return vec3(pos.x * cos(a) - pos.y * sin(a), pos.x * sin(a) + pos.y * cos(a), pos.z);
}

void main() {
    vec3 instanced_pos = vec3(instanced_data.xy, position.z);
    float instanced_rot = instanced_data.z;

    gl_Position = (view * (model * vec4(instanced_pos + rotate(position, instanced_rot), 1)));
    interpolatedColor = color;
}

