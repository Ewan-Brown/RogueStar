#version 330


uniform float time;
uniform mat4 view;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;

void main()
{

    vec4 adjustedPos = inverse(view) * vec4(xyVarying,0,1);

    outputColor.xyz = vec3(cos(length(adjustedPos)), 0, 0);

}