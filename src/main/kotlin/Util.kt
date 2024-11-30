import org.dyn4j.geometry.Vector2
import kotlin.math.floor

operator fun Vector2.component1() = this.x
operator fun Vector2.component2() = this.y

operator fun Vector2.plus(vec: Vector2): Vector2 = this.sum(vec)
operator fun Vector2.minus(vec: Vector2): Vector2 = this.difference(vec)
operator fun Vector2.times(factor: Double): Vector2 = this.multiply(factor)
operator fun Vector2.div(factor: Double): Vector2 = this.quotient(factor)

fun Vector2.floor(): Vector2 = Vector2(floor(this.x), floor(this.y))
fun Vector2.flip(): Vector2 = this.product(-1.0)

fun Boolean.toInt() = if (this) 1 else 0
