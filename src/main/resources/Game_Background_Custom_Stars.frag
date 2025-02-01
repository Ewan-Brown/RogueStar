#version 330

uniform mat4 viewZ;
uniform vec2 velocity;
uniform float time;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;

vec2 random2( vec2 p ) {
    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
}


float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}


vec2 hash2(vec2 v){
    return random2(v);
}

vec3 voronoi( in vec2 x )
{
    //Cell Index
    vec2 ip = floor(x);
    //Local Position
    vec2 fp = fract(x);

    //----------------------------------
    // first pass: regular voronoi
    //----------------------------------
    vec2 mg, mr;

    float md = 8.0;
    for( int j=-1; j<=1; j++ )
    for( int i=-1; i<=1; i++ )
    {
        vec2 g = vec2(float(i),float(j));
        vec2 o = hash2( ip + g );
        vec2 r = g + o - fp;
        float d = dot(r,r);

        if( d<md )
        {
            md = d;
            mr = r;
            mg = g;
        }
    }

    return vec3( 2.0, mr );
}

float val(vec2 absPos){
    vec2 gridPos = floor(absPos);
    float gridPosRand = rand(gridPos);
    vec2 localPos = absPos - gridPos;
    vec2 localStar = vec2(rand(gridPos.xy) - 0.5, rand(gridPos.yx) - 0.5)/1.5 + vec2(0.5,0.5);
    float gridRand = rand(gridPos);
    gridRand = length(localPos - localStar);
    return (1.0-gridRand*6.0) * step(0.95, gridPosRand);
}

void main() {
    vec2 localPos = xyVarying;
    vec4 globalPos = inverse(viewZ) * vec4(localPos,0,1);
//    vec2 scaledPos = globalPos.xy/100.0;
//    vec3 color = voronoi( 8.0*scaledPos );
//    vec3 col = color.x*(0.5 + 0.5*sin(64.0*color.x))*vec3(1.0);
//    float dd = length( color.yz );
//    col = mix( vec3(0.0,0.0,0.0), col, smoothstep( 0.0, 0.05, dd) );
//  outputColor = vec4(1- col.x, 1- col.y, 0,1.0);
    float val = val(globalPos.xy) + val(globalPos.xy * 2.0);
    outputColor = vec4(val, val, val, 0.0);
}
