#version 330

layout (location = 6) in vec3 position;
layout (location = 7) in vec3 color;

uniform float time;
uniform mat4 cameraViewMatrix;

out vec3 interpolatedColor;

void main() {

    gl_Position = cameraViewMatrix * vec4(position, 1);
    interpolatedColor = color;
}

