#version 330

layout (location = 0) in vec3 position;
layout (location = 1) in vec3 instanced_pos;
layout (location = 2) in float instanced_rotation;
layout (location = 3) in float instanced_scale;
layout (location = 4) in vec3 instanced_color;
layout (location = 5) in float instanced_health;

//x,y,rotation,scale (z is baked into model)
//r,g,b,a

uniform float time;
uniform mat4 viewZ;
uniform vec2 velocity;
out vec3 interpolatedColor;
out float health_out;

//I dunno if this is necessary but thought it was interesting and wanted to use it (interface block)
out vec2 xyVarying;

vec3 rotate(vec3 pos, float a) {
    return vec3((pos.x) * cos(a) - pos.y * sin(a), pos.x * sin(a) + pos.y * cos(a), pos.z);
}

void main() {
    vec3 pos = instanced_pos;
    vec3 scaledPosition = position * instanced_scale;

    gl_Position = viewZ * vec4(pos + rotate(scaledPosition, instanced_rotation), 1);
    interpolatedColor = instanced_color;
    health_out = instanced_health;
    xyVarying = scaledPosition.xy;
}

