#version 330

uniform float time;
uniform mat4 view;
//uniform vec2 speed;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;

vec2 random2( vec2 p ) {
    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
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

void main() {
    vec2 localPos = xyVarying;
    vec4 globalPos = inverse(view) * vec4(localPos,0,1);
    vec2 scaledPos = globalPos.xy/100.0;
    vec3 color = voronoi( 8.0*scaledPos );

    vec3 col = color.x*(0.5 + 0.5*sin(64.0*color.x))*vec3(1.0);
    float dd = length( color.yz );
    col = mix( vec3(0.0,0.0,0.0), col, smoothstep( 0.0, 0.05, dd) );
    outputColor = vec4(1- col.x, 1- col.y, 0,1.0);
}
