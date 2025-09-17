package models

import com.jogamp.opengl.GL
import math.Vector2

public class Model internal constructor(val vertexData: FloatArray, dMode: Int) {
    val points: Int = vertexData.size / 3 //Change if vertex data size changes!
    val drawMode: Int = dMode
    val area: Double

    //https://web.archive.org/web/20100405070507/http://valis.cs.uiuc.edu/~sariel/research/CG/compgeom/msg00831.html
    // Calculate area of polygon
    init {
        var a = 0.0
        for(i in 0 until points) {
            val j = (i+1) % points
            val ix = vertexData[i * 3]
            val iy = vertexData[i * 3 + 1]
            val jx = vertexData[j * 3]
            val jy = vertexData[j * 3 + 1]
            a += ix * jy
            a -= iy * jx
        }
        area = a / 2.0
    }


    companion object {
        var SQUARE: Model = Model(
            floatArrayOf(
                -0.5f, -0.5f, +0.1f,
                +0.5f, -0.5f, +0.1f,
                +0.5f, +0.5f, +0.1f,
                -0.5f, +0.5f, +0.1f
            ), GL.GL_TRIANGLE_FAN
        )
        var BACKPLATE: Model = Model(
            floatArrayOf(
                -1f, -1f, +0.4f,
                -1f, +1f, +0.4f,
                +1f, +1f, +0.4f,
                +1f, -1f, +0.4f
            ), GL.GL_TRIANGLE_FAN
        )
    }
}