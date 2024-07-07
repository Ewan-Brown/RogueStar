#version 330

// Incoming vertex position
layout (location = 0) in vec3 position;
layout (location = 1) in vec3 color;

out vec2 xyVarying;

void main() {

//    gl_Position = proj * (view * (model * vec4(position, 1)));
    gl_Position = vec4(position.xyz, 1);
    xyVarying = position.xy;

//    interpolatedColor = color;
}

