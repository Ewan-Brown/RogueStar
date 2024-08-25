#version 330


uniform float time;
uniform mat4 view;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;

void main()
{

    vec4 adjustedPos = inverse(view) * vec4(xyVarying,0,1);

    outputColor.xyz = vec3(1-step(cos(adjustedPos.x), 0.95), 1-step(cos(adjustedPos.y), 0.95), 1-step(cos(adjustedPos.y), 0.95));

}