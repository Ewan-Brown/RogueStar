import org.dyn4j.geometry.Vector2

operator fun Vector2.component1() = this.x
operator fun Vector2.component2() = this.y

operator fun Vector2.plus(vec: Vector2): Vector2 = this.sum(vec)
operator fun Vector2.minus(vec: Vector2): Vector2 = this.difference(vec)
operator fun Vector2.times(factor: Double): Vector2 = this.multiply(factor)
operator fun Vector2.div(factor: Double): Vector2 = this.quotient(factor)