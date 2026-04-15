package math

import kotlin.math.*

operator fun List<Vector2>.plus(vec: Vector2): List<Vector2> {return this.map { it.plus(vec) }}
operator fun List<Vector2>.minus(vec: Vector2): List<Vector2> {return this.map { it.minus(vec) }}
operator fun List<Vector2>.times(factor: Double): List<Vector2> {return this.map { it.times(factor) }}
operator fun List<Vector2>.div(factor: Double): List<Vector2> {return this.map { it.div(factor) }}

class Vector2(private val x :Double, private val y :Double) {
    constructor() : this (0.0, 0.0)
    constructor(angle: Double) : this(cos(angle), sin(angle))
    constructor(point: java.awt.Point) : this(point.x.toDouble(), point.y.toDouble())
    constructor(point: com.jogamp.nativewindow.util.Point) : this(point.x.toDouble(), point.y.toDouble())
    constructor(vector3: Vector3) : this(vector3.getX(), vector3.getY())
    fun getMagnitude(): Double = sqrt(x.pow(2.0) + y.pow(2.0))
    fun getX(): Double {
        return x
    }
    fun getY(): Double {
        return y
    }
    fun normalize() : Vector2 {
        val length = getMagnitude()
        return this / length
    }

    operator fun plus(v : Vector2) : Vector2 {
        return Vector2(getX() + v.getX(), getY() + v.getY())
    }

    operator fun minus(v : Vector2) : Vector2 {
        return Vector2(getX() - v.getX(), getY() - v.getY())
    }

    operator fun times(scalar : Double) : Vector2 {
        return elementwiseOperation { it*scalar }
    }

    fun dot(vec : Vector2) : Double {
        return getX() * vec.getX() + getY() * vec.getY()
    }

    operator fun div(scalar : Double) : Vector2 {
        return elementwiseOperation { it/scalar }
    }

    fun elementwiseOperation(op: (Double) -> Double) : Vector2 {
        return Vector2(op(getX()), op(getY()))
    }

    fun floor() : Vector2 {
        return elementwiseOperation { floor(it) }
    }

    fun ceil() : Vector2 {
        return elementwiseOperation { ceil(it) }
    }

    fun round() : Vector2 {
        return elementwiseOperation { round(it) }
    }

    fun rotate(angle : Double) : Vector2 {
        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        return Vector2(getX() * cosTheta - getY() * sinTheta, getX() * sinTheta + getY() * cosTheta)
    }

    fun getAngleTo(otherVector: Vector2) : Double{
        return atan2(otherVector.x, otherVector.y) - atan2(this.x, this.y)
    }

    fun getAngleTo(angle: Double) : Double{
        return angle - atan2(this.x, this.y)
    }

    fun getSlope(): Double = this.getY()/this.getX()
    override fun toString(): String {
        return "[x = $x, y = $y]"
    }

    fun projectOnto(otherVector: Vector2) : Vector2 {
        val dotProduct = this.dot(otherVector)
        return Vector2(
            (dotProduct / (otherVector.x * otherVector.x + otherVector.y * otherVector.y)) * otherVector.x,
            (dotProduct / (otherVector.x * otherVector.x + otherVector.y * otherVector.y)) * otherVector.y)
    }

    fun leftHandNormal() : Vector2 {
        return Vector2(
            this.y,
            -this.x
        )
    }

    fun rightHandNormal() : Vector2 {
        return Vector2(
            -this.y,
            this.x
        )
    }

    fun applyTransform(transform: Transform<*, *>): Vector2 {
        return this.rotate(transform.rotation) + transform.translation
    }

}

/**
 * Mutable fields.
 * Only to be used ephemerally.
 * TODO Find a way to enforce this?
 */
data class Transformation2(var translation: Vector2, var rotation: Double, var scale: Double){
    constructor() : this(Vector2(), 0.0, 1.0)
    init {
        if(scale < Double.MIN_VALUE){
            throw Exception("Attempted to create a Transformation2 with an invalid scale value: $scale")
        }
    }
    fun copy() : Transformation2 {
        return Transformation2(translation, rotation, scale)
    }
}

/**
 * Mutable fields.
 * Only to be used ephemerally.
 * TODO Find a way to enforce this?
 */
data class Transformation3(var translation: Vector3, var rotation: Double, var scale: Double){
    constructor(transform: Transformation2) : this(Vector3(transform.translation), transform.rotation, transform.scale)
    constructor(transform: Transformation2, z: Double) : this(Vector3(transform.translation, z), transform.rotation, transform.scale)

    constructor() : this(Vector3(), 0.0, 1.0)
    init {
        if(scale < Double.MIN_VALUE){
            throw Exception("Attempted to create a Transformation2 with an invalid scale value: $scale")
        }
    }
    fun copy() : Transformation3 {
        return Transformation3(translation, rotation, scale)
    }
}

class Polygon2(val points : List<Vector2>) {
    fun encloses(point: Vector2) : Boolean {
        return doesPolygonContainPoint(points, point)
    }
}

// https://wrfranklin.org/Research/Short_Notes/pnpoly.html
// Point Inclusion in Polygon Test
fun doesPolygonContainPoint(points: List<Vector2>, point: Vector2) : Boolean {
    var i: Int = 0
    var j: Int = points.size - 1
    var c: Boolean = false;
    while(i < points.size){
        if(
            ((points[i].getY() > point.getY()) != (points[j].getY() > point.getY()))
            && (point.getX() < (points[j].getX() - points[i].getX()) * (point.getY() - points[i].getY()) / (points[j].getY() - points[i].getY()) + points[i].getX() ))
            c =!c

        j = i++
    }
    return c;
}

fun List<Vector2>.getCentroid() : Vector2 {
    var A = 0.0;
    for(i in indices){
        val p = this[i]
        val nextP = this[(i + 1) % this.size]
        A += (p.getX() * nextP.getY()) - (nextP.getX() * p.getY())
    }
    A /= 2.0

    var Cx = 0.0
    var Cy = 0.0
    for(i in indices) {
        val p = this[i]
        val nextP = this[(i + 1) % this.size]

        val intermed = p.getX()*nextP.getY() - nextP.getX() * p.getY()

        Cx += (p.getX() + nextP.getX())*(intermed)
        Cy += (p.getY() + nextP.getY())*(intermed)
    }
    Cx /= 6 * A
    Cy /= 6 * A
    return Vector2(Cx, Cy)
}

class Vector3(private val x :Double, private val y :Double, private val z :Double){
    constructor() : this(0.0, 0.0, 0.0)
    constructor(vector2: Vector2) : this(vector2.getX(), vector2.getY(), 0.0)
    constructor(vector2: Vector2, z: Double) : this(vector2.getX(), vector2.getY(), z)
    fun getX(): Double {
        return x
    }
    fun getY(): Double {
        return y
    }
    fun getZ(): Double {
        return z
    }


    operator fun plus(v : Vector3) : Vector3 {
        return Vector3(getX() + v.getX(), getY() + v.getY(), getZ() + v.getZ())
    }

    operator fun minus(v : Vector3) : Vector3 {
        return Vector3(getX() - v.getX(), getY() - v.getY(), getZ() - v.getZ())
    }

    operator fun times(scalar : Double) : Vector3 {
        return elementwiseOperation { it*scalar }
    }

    operator fun div(scalar : Double) : Vector3 {
        return elementwiseOperation { it/scalar }
    }

    fun elementwiseOperation(op: (Double) -> Double) : Vector3 {
        return Vector3(op(getX()), op(getY()), op(getZ()))
    }

    fun floor() : Vector3 {
        return elementwiseOperation { floor(x) }
    }

    fun ceil() : Vector3 {
        return elementwiseOperation { ceil(x) }
    }

    fun round() : Vector3 {
        return elementwiseOperation { round(x) }
    }

    fun getMagnitude(): Double = sqrt(x.pow(2.0) + y.pow(2.0) + z.pow(2.0))

    fun getNormalized() : Vector3 {
        val length = getMagnitude()
        return Vector3(getX()/length, getY()/length, getZ()/length)
    }

    //TODO this just rotates by Z axis
    fun rotate(angle : Double) : Vector3 {
        val cosTheta = cos(angle)
        val sinTheta = sin(angle)
        return Vector3(getX() * cosTheta - getY() * sinTheta, getX() * sinTheta + getY() * cosTheta, getZ() * sinTheta)
    }
    override fun toString(): String {
        return "x = $x, y = $y, z = $z"
    }
}

fun Vector2.extruded(z : Double) : Vector3 = Vector3(getX(), getY(), z)

fun getRandomSign(): Double{
    if(Math.random() < 0.5){
        return -1.0
    }else{
        return 1.0
    }
}

//class Rotation(private val rotation: Double){
//    fun getRotation(): Double {return rotation}
//    operator fun plus(r : Rotation) : Rotation {
//        return Rotation((getRotation() + r.getRotation()))
//    }
//    operator fun minus(r : Rotation) : Rotation {
//        return Rotation(getRotation() - r.getRotation())
//    }
//}

