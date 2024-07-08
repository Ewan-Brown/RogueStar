#version 330


uniform vec2 u_resolution;
uniform vec2 position;
uniform float time;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;


void main()
{
    // map (-1,-1)(+1,+1) to red-green ramps.
    vec2 xyVaryingT = xyVarying + position;
    outputColor = vec4(xyVaryingT.x / 2.0 + 0.5,
                     xyVaryingT.y / 2.0 + 0.5,
                     0.0 ,1.0);
}