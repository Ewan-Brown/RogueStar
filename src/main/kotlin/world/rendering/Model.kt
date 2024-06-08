package world.rendering

import org.dyn4j.geometry.Vector2

class Model(val points: Set<Vector2>) {
    val doubleArray: DoubleArray = points.flatMap{ listOf(it.x, it.y) }.toDoubleArray()
    val floatArray: FloatArray = points.flatMap{ listOf(it.x.toFloat(), it.y.toFloat()) }.toFloatArray()
}

object Models {
    val square = Model(setOf(Vector2(-1.0,-1.0),Vector2(-1.0,1.0),Vector2(1.0,1.0),Vector2(1.0,-1.0)))
    val triangle = Model(setOf(Vector2(-1.0,-1.0),Vector2(0.0,2.0),Vector2(1.0,-1.0)))
}