#version 330


//uniform float time;
//uniform mat4 view;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;

void main()
{

    outputColor.xyz = vec3(0, 0, 0);

}

// Creepy
//vec2 pos = xyVarying + position;
//
//vec3 xyz = vec3(pos, 0);
//float a = colorCalculated(xyz, time) + cos(time + xyz.x/10.0)*0.1 - 0.2;
//float b = colorCalculated(vec3(a, a, a), time);
//outputColor.xyz = vec3(b, 0, 0)/3;

// "Slice"
//float a = colorCalculated(xyz, time);
//if(a > MIN && a < MAX){
//    a = 1;
//}else {
//    a = 0;
//}