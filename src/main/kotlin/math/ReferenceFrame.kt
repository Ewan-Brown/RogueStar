package math

sealed interface ReferenceFrame

//"Label" types for the different coordinate systems in the game's hierarchy.
object PawnReferenceFrame : ReferenceFrame
object PartReferenceFrame : ReferenceFrame
object EntityReferenceFrame : ReferenceFrame
object WorldReferenceFrame : ReferenceFrame

//TODO rename this...
interface ReferenceFrameVariable<S: ReferenceFrame>

// Note - as awesome and cool as this is, this isn't perfect.
// The child class must override this function with a return type equal to the type of the child class, hence the 'self'
interface Transformable<S1: ReferenceFrame, Self : ReferenceFrameVariable<S1>>{
    fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S1, S2>) : ReferenceFrameVariable<S2>
}

interface InReferenceFrame<S: ReferenceFrame>{
    /**
     * Implicitly local center is always (0, 0)
     */
    fun getCoordinates() : Coordinates<S>
    fun getOrientation() : Orientation<S>
    fun getZHeight() : ZHeight<S>
}

interface HasReferenceFrame<S: ReferenceFrame>

fun <From: ReferenceFrame, To: ReferenceFrame, T> getTransformToParentFrame(a: T) : Transform<From, To> where T : InReferenceFrame<To>, T: HasReferenceFrame<From> {
    return Transform(a.getCoordinates().getVector(), a.getOrientation().getAngle(), a.getZHeight().getZ())
}

@JvmInline
value class Coordinates<S: ReferenceFrame>(private val value: Vector2) : ReferenceFrameVariable<S>, Transformable<S, Coordinates<S>>{
    fun rotate(theta: Double) : Coordinates<S> = Coordinates(value.rotate(theta))
    operator fun plus(vec: Vector2) : Coordinates<S> = Coordinates(this.value + vec)
    operator fun minus(vec: Vector2) : Coordinates<S> = Coordinates(this.value - vec)
    operator fun minus(coordinates: Coordinates<S>) : Vector2 = this.value - coordinates.value

    fun getX() : Double = value.getX()
    fun getY() : Double = value.getY()

    //TODO Should this ever be used...?
    fun getVector() : Vector2 = value

    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): Coordinates<S2> {
        return Coordinates(value.rotate(transform.rotation) + transform.translation)
    }
}

@JvmInline
value class Orientation<S: ReferenceFrame>(private val value: Double) : ReferenceFrameVariable<S>, Transformable<S, Orientation<S>>{
    fun rotate(theta: Double) : Orientation<S> = Orientation(this.value + theta)
    operator fun plus(theta: Double) : Orientation<S> = Orientation(this.value + theta)
    operator fun minus(theta: Double) : Orientation<S> = Orientation(this.value - theta)

    fun getAngle() : Double = value
    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): Orientation<S2> {
        return Orientation(this.value + transform.rotation)
    }

}
@JvmInline
value class ZHeight<S: ReferenceFrame>(private val value: Double) : ReferenceFrameVariable<S>, Transformable<S, ZHeight<S>>{

    operator fun plus(z: Double) : ZHeight<S> = ZHeight(this.value + z)
    operator fun minus(z: Double) : ZHeight<S> = ZHeight(this.value - z)

    fun getZ() : Double = value

    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): ZHeight<S2> {
        return ZHeight(this.value + transform.zHeight)
    }
}

class Transform<from: ReferenceFrame, to: ReferenceFrame>(val translation: Vector2, val rotation: Double, val zHeight: Double){}

fun <S1: ReferenceFrame, S2: ReferenceFrame, S3: ReferenceFrame> combineTransforms(transform1: Transform<S1, S2>, transform2: Transform<S2, S3>) : Transform<S1, S3>{
    val newTranslation = transform1.translation.rotate(transform2.rotation) + transform2.translation
    val newRotation = transform1.rotation + transform2.rotation
    val newZHeight = transform1.zHeight + transform2.zHeight
    return Transform(newTranslation, newRotation, newZHeight)
}