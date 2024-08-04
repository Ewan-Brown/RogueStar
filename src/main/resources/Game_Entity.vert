#version 330

layout (location = 0) in vec3 position;
layout (location = 1) in vec4 instanced_pos;
layout (location = 2) in vec4 instanced_color;

//x,y,rotation,scale (z is baked into model)
//r,g,b,a

uniform GlobalMatrices
{
    mat4 view;
};

uniform mat4 model;
out vec3 interpolatedColor;

vec3 rotate(vec3 pos, float a) {
    return vec3(pos.x * cos(a) - pos.y * sin(a), pos.x * sin(a) + pos.y * cos(a), pos.z);
}

void main() {
    vec3 pos = vec3(instanced_pos.xy, position.z);
    float instanced_rot = instanced_pos.z;
    float scale = instanced_pos.w;
    vec3 scaledPosition = position * scale;

    gl_Position = (view * (model * vec4(pos + rotate(scaledPosition, instanced_rot), 1)));
    interpolatedColor = instanced_color.xyz;
}

