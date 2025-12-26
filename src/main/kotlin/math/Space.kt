package math

sealed interface Space

object PartSpace : Space
object ShipSpace : Space
object WorldSpace : Space

interface SpacialConcept<S: Space>

interface Transformable<S1: Space, Self : SpacialConcept<S1>>{
    fun <S2 : Space> applyTransform(transform: Transform<S1, S2>) : SpacialConcept<S2>
}

@JvmInline
value class Coordinate<S: Space>(private val value: Vector2) : SpacialConcept<S>, Transformable<S, Coordinate<S>>{
    fun rotate(theta: Double) : Coordinate<S> = Coordinate(value.rotate(theta))
    operator fun plus(vec: Vector2) : Coordinate<S> = Coordinate(this.value + vec)
    operator fun minus(vec: Vector2) : Coordinate<S> = Coordinate(this.value - vec)
    operator fun minus(coordinate: Coordinate<S>) : Vector2 = this.value - coordinate.value

    fun getX() : Double = value.getX()
    fun getY() : Double = value.getY()

    //TODO Should this ever be used...?
    fun getVector() : Vector2 = value

    override fun <S2 : Space> applyTransform(transform: Transform<S, S2>): Coordinate<S2> {
        return Coordinate(value.rotate(transform.rotation) + transform.translation)
    }
}

@JvmInline
value class Orientation<S: Space>(private val value: Double) : SpacialConcept<S>, Transformable<S, Orientation<S>>{
    fun rotate(theta: Double) : Orientation<S> = Orientation(this.value + theta)
    operator fun plus(theta: Double) : Orientation<S> = Orientation(this.value + theta)
    operator fun minus(theta: Double) : Orientation<S> = Orientation(this.value - theta)

    fun getAngle() : Double = value
    override fun <S2 : Space> applyTransform(transform: Transform<S, S2>): Orientation<S2> {
        return Orientation(this.value + transform.rotation)
    }

}
@JvmInline
value class ZHeight<S: Space>(private val value: Double) : SpacialConcept<S>, Transformable<S, ZHeight<S>>{

    operator fun plus(z: Double) : ZHeight<S> = ZHeight(this.value + z)
    operator fun minus(z: Double) : ZHeight<S> = ZHeight(this.value - z)

    fun getZ() : Double = value

    override fun <S2 : Space> applyTransform(transform: Transform<S, S2>): ZHeight<S2> {
        return ZHeight(this.value + transform.zHeight)
    }
}

class Transform<from: Space, to: Space>(val translation: Vector2, val rotation: Double, val zHeight: Double){}

fun <S1: Space, S2: Space, S3: Space> combineTransforms(transform1: Transform<S1, S2>, transform2: Transform<S2, S3>) : Transform<S1, S3>{
    val newTranslation = transform1.translation.rotate(transform2.rotation) + transform2.translation
    val newRotation = transform1.rotation + transform2.rotation
    val newZHeight = transform1.zHeight + transform2.zHeight
    return Transform(newTranslation, newRotation, newZHeight)
}





