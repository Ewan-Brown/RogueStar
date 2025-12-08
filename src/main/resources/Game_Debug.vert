#version 330

layout (location = 6) in vec3 position;
//layout (location = 7) in vec3 color;

uniform float time;
uniform mat4 viewZ;

out vec3 interpolatedColor;

void main() {

    gl_Position = viewZ * vec4(position, 1);
//    gl_Position = vec4(position, 1.0, 1.0);
//    interpolatedColor = color;
    interpolatedColor = vec3(1.0, 1.0, 1.0);
}

