#version 430

layout(location = 0) in vec3 position;

//uniform mat4 mv_matrix;

out vec4 varyingColor;

void main(void){

//    gl_Position = mv_matrix * vec4(position,1.0);
    gl_Position = vec4(position, 1.0);
    varyingColor = vec4(0.6,0.6,0.6,0.6);
}