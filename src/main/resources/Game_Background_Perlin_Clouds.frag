#version 330


uniform vec2 u_resolution;
uniform vec2 position;
uniform float time;

in vec2 xyVarying;
layout (location = 0) out vec4 outputColor;

//
// Description : Array and textureless GLSL 2D/3D/4D simplex
//               noise functions.
//      Author : Ian McEwan, Ashima Arts.
//  Maintainer : stegu
//     Lastmod : 20201014 (stegu)
//     License : Copyright (C) 2011 Ashima Arts. All rights reserved.
//               Distributed under the MIT License. See LICENSE file.
//               https://github.com/ashima/webgl-noise
//               https://github.com/stegu/webgl-noise
//

vec3 mod289(vec3 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 mod289(vec4 x) {
    return x - floor(x * (1.0 / 289.0)) * 289.0;
}

vec4 permute(vec4 x) {
    return mod289(((x*34.0)+10.0)*x);
}

vec4 taylorInvSqrt(vec4 r)
{
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v)
{
    const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

    // First corner
    vec3 i  = floor(v + dot(v, C.yyy) );
    vec3 x0 =   v - i + dot(i, C.xxx) ;

    // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min( g.xyz, l.zxy );
    vec3 i2 = max( g.xyz, l.zxy );

    //   x0 = x0 - 0.0 + 0.0 * C.xxx;
    //   x1 = x0 - i1  + 1.0 * C.xxx;
    //   x2 = x0 - i2  + 2.0 * C.xxx;
    //   x3 = x0 - 1.0 + 3.0 * C.xxx;
    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
    vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y

    // Permutations
    i = mod289(i);
    vec4 p = permute( permute( permute(
    i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
    + i.y + vec4(0.0, i1.y, i2.y, 1.0 ))
    + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

    // Gradients: 7x7 points over a square, mapped onto an octahedron.
    // The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
    float n_ = 0.142857142857; // 1.0/7.0
    vec3  ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)

    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4( x.xy, y.xy );
    vec4 b1 = vec4( x.zw, y.zw );

    //vec4 s0 = vec4(lessThan(b0,0.0))*2.0 - 1.0;
    //vec4 s1 = vec4(lessThan(b1,0.0))*2.0 - 1.0;
    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;

    vec3 p0 = vec3(a0.xy,h.x);
    vec3 p1 = vec3(a0.zw,h.y);
    vec3 p2 = vec3(a1.xy,h.z);
    vec3 p3 = vec3(a1.zw,h.w);

    //Normalise gradients
    vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    // Mix final noise value
    vec4 m = max(0.5 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 105.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1),
    dot(p2,x2), dot(p3,x3) ) );
}

float color(vec3 xyt) { return snoise(vec3(xyt)); }

float colorCalculated(vec3 xyz, float t){
    float n2 = color(vec3(xyz.xy*16, t/16))/16;
    float n3 = color(vec3(xyz.xy*8, t/2.0))/8;
    float n4 = color(vec3(xyz.xy*4, t/4.0))/4;
    float n5 = color(vec3(xyz.xy*2, t/8.0))/2;
    float n6 = color(vec3(xyz.xy, t/16.0));

    return n2 + n3 + n4 + n5 + n6;
}

float rand(vec2 co){
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

void main()
{


    //Varible here is the 'speed' as affected by player
    vec2 posFar = xyVarying + position;
    vec2 posMedium = xyVarying + position*2;
    vec2 posClose = xyVarying + position*4;

    float farBig = color(vec3(posFar.xy, 0));
    float farNormal = color(vec3(posFar.xy*2, 0));
    float farSmall = color(vec3(posFar.xy*4, 0));

    float mediumBig = color(vec3(posMedium.xy, 100));
    float mediumNormal = color(vec3(posMedium.xy*2, 100));
    float mediumSmall = color(vec3(posMedium.xy*4, 100));

    float closeBig = color(vec3(posClose.xy, 200));
    float closeNormal = color(vec3(posClose.xy*2, 200));
    float closeSmall = color(vec3(posClose.xy*4, 200));

    float far = farBig * 2.0/3.0 + farNormal * 2.0/3.0 * 2.0/3.0 + farSmall * 2.0/3.0 * 1.0/3.0;
    float medium = mediumBig * 2.0/3.0 + mediumNormal * 2.0/3.0 * 2.0/3.0 + mediumSmall * 2.0/3.0 * 1.0/3.0;
    float close = closeBig * 2.0/3.0 + closeNormal * 2.0/3.0 * 2.0/3.0 + closeSmall * 2.0/3.0 * 1.0/3.0;

    vec2 posStars = xyVarying*10;

    vec2 starPosition = vec2(round(posStars.x), round(posStars.y));
    float starStrength = color(vec3(starPosition.xy, 0.0)) * color(vec3(starPosition.xy, 100.0));

    float val1 = cos(((posStars.x) * 3.14)) * starStrength;
    float val2 = cos(((posStars.y) * 3.14)) * starStrength;

    //Doesn't work, we're just shifting this raw map...

    float squared = val1*val2 * val1*val2;

    outputColor.xyz = vec3(far, medium, close);

}

//   Dots
//vec2 pos = (xyVarying + position)*10;
//
//float val = cos(pos.x);
//float val2 = sin(pos.y);
//
//float squared = val*val2 * val*val2 * val*val2;
//
//outputColor.xyz = vec3(squared, 0, 0);

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

//Blobbies
//float a = color(vec3(pos1.xy, 1));
//float b = color(vec3(pos2.xy, 100));
//float c = color(vec3(pos3.xy, 200));
//
//float val = max(a, b);
//val = max(val, c);