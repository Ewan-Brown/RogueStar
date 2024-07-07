#version 330


uniform vec2 u_resolution;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;
void main()
{
    // map (-1,-1)(+1,+1) to red-green ramps.
    outputColor = vec4(xyVarying.x / 2.0 + 0.5,
                     xyVarying.y / 2.0 + 0.5,
                     0.0 ,1.0);
}