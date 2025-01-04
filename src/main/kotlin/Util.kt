import org.dyn4j.geometry.Vector2
import java.awt.Point
import java.awt.Polygon
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

operator fun Vector2.component1() = this.x
operator fun Vector2.component2() = this.y

operator fun Vector2.plus(vec: Vector2): Vector2 = this.sum(vec)
operator fun Vector2.minus(vec: Vector2): Vector2 = this.difference(vec)
operator fun Vector2.times(factor: Double): Vector2 = this.product(factor)
operator fun Vector2.div(factor: Double): Vector2 = this.quotient(factor)

operator fun List<Vector2>.plus(vec: Vector2): List<Vector2> {return this.map { it.add(vec) }}
operator fun List<Vector2>.minus(vec: Vector2): List<Vector2> {return this.map { it.minus(vec) }}
operator fun List<Vector2>.times(factor: Double): List<Vector2> {return this.map { it.multiply(factor) }}
operator fun List<Vector2>.div(factor: Double): List<Vector2> {return this.map { it.div(factor) }}

fun vectorListToPolygon(vec: List<Vector2>) : Polygon {
    return Polygon(vec.map { it -> it.x.toInt() }.toIntArray(), vec.map { it -> it.y.toInt() }.toIntArray(), vec.size)
}

fun Vector2.getSlope(): Double = this.y/this.x
fun Vector2.floor(): Vector2 = Vector2(floor(this.x), floor(this.y))
fun Vector2.round(): Vector2 = Vector2(round(this.x), round(this.y))
fun Vector2.ceil(): Vector2 = Vector2(ceil(this.x), ceil(this.y))
fun Vector2.flip(): Vector2 = this.product(-1.0)
fun Point.toVector(): Vector2 = Vector2(this.getX(), this.getY())
fun List<Vector2>.getCentroid() : Vector2{
    var A = 0.0;
    for(i in indices){
        val p = this[i]
        val nextP = this[(i + 1) % this.size]
        A += (p.x * nextP.y) - (nextP.x * p.y)
    }
    A /= 2.0

//    println(A)

    var Cx = 0.0
    var Cy = 0.0
    for(i in indices) {
        val p = this[i]
        val nextP = this[(i + 1) % this.size]

        val intermed = p.x*nextP.y - nextP.x * p.y

        Cx += (p.x + nextP.x)*(intermed)
        Cy += (p.y + nextP.y)*(intermed)
    }
    Cx /= 6 * A
    Cy /= 6 * A
    return Vector2(Cx, Cy)
}

fun Boolean.toInt() = if (this) 1 else 0
