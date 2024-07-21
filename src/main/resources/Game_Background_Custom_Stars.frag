#version 330


uniform vec2 position;
uniform float time;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;

#define ANIMATE

vec2 random2( vec2 p ) {
    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);
}

vec2 hash2(vec2 v){
    return random2(v);
}

vec3 voronoi( in vec2 x )
{
    vec2 ip = floor(x);
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
        #ifdef ANIMATE
        o = 0.5 + 0.5*sin( time*0.0001 + 6.2831*o );
        #endif
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
    vec2 st = xyVarying;
    vec3 color = vec3(.0);
    vec3 c = voronoi( 8.0*st );

    // isolines
    vec3 col = c.x*(0.5 + 0.5*sin(64.0*c.x))*vec3(1.0);
    // borders
    col = mix( vec3(1.0,0.6,0.0), col, smoothstep( 0.04, 0.07, c.x ) );
    // feature points
    float dd = length( c.yz );
    col = mix( vec3(1.0,0.6,0.1), col, smoothstep( 0.0, 0.12, dd) );
    col += vec3(1.0,0.6,0.1)*(1.0-smoothstep( 0.0, 0.04, dd));
    outputColor = vec4(col,1.0);
}