package math

sealed interface ReferenceFrame

//"Label" types for the different coordinate systems in the game's hierarchy.
object PawnReferenceFrame : ReferenceFrame
object ComponentReferenceFrame : ReferenceFrame
object EntityReferenceFrame : ReferenceFrame
object WorldReferenceFrame : ReferenceFrame

//TODO rename this...
interface ReferenceFrameVariable<in S1: ReferenceFrame>{
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

fun <Local: ReferenceFrame, Parent: ReferenceFrame, T> getTransformLocalToParentFrame(a: T) : Transform<Local, Parent> where T : InReferenceFrame<Parent>, T: HasReferenceFrame<Local>
{
    return Transform(a.getCoordinates().getVector(), a.getOrientation().getAngle(), a.getZHeight().getZ())
}

fun <Local: ReferenceFrame, Parent: ReferenceFrame, T> getTransformParentToLocalFrame(a: T) : Transform<Parent, Local> where T : InReferenceFrame<Parent>, T: HasReferenceFrame<Local> {
    return Transform(
        a.getCoordinates().getVector().rotate(a.getOrientation().getAngle() * -1.0) * -1.0,
        a.getOrientation().getAngle() * -1.0,
        a.getZHeight().getZ() * -1.0)
}

@JvmInline
value class Coordinates<S: ReferenceFrame>(private val value: Vector2) : ReferenceFrameVariable<S>{
    fun rotate(theta: Double) : Coordinates<S> = Coordinates(value.rotate(theta))
    operator fun plus(vec: Vector2) : Coordinates<S> = Coordinates(this.value + vec)
    operator fun minus(vec: Vector2) : Coordinates<S> = Coordinates(this.value - vec)
    operator fun minus(coordinates: Coordinates<S>) : Vector2 = this.value - coordinates.value

    fun getX() : Double = value.getX()
    fun getY() : Double = value.getY()

    fun getVector() : Vector2 = value
    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): Coordinates<S2> {
        return Coordinates(value.rotate(transform.rotation) + transform.translation)
    }
}

@JvmInline
value class Orientation<S: ReferenceFrame>(private val value: Double) : ReferenceFrameVariable<S>{
    fun rotate(theta: Double) : Orientation<S> = Orientation(this.value + theta)
    operator fun plus(theta: Double) : Orientation<S> = Orientation(this.value + theta)
    operator fun minus(theta: Double) : Orientation<S> = Orientation(this.value - theta)
    operator fun minus(orientation: Orientation<S>) : Double = this.value - orientation.getAngle()

    fun getAngle() : Double = value
    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): Orientation<S2> {
        return Orientation(this.value + transform.rotation)
    }

}
@JvmInline
value class ZHeight<S: ReferenceFrame>(private val value: Double) : ReferenceFrameVariable<S>{

    operator fun plus(z: Double) : ZHeight<S> = ZHeight(this.value + z)
    operator fun minus(z: Double) : ZHeight<S> = ZHeight(this.value - z)

    fun getZ() : Double = value

    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): ZHeight<S2> {
        return ZHeight(this.value + transform.zHeight)
    }
}

class Transform<out from: ReferenceFrame, out to: ReferenceFrame>(val translation: Vector2, val rotation: Double, val zHeight: Double){}

fun <S1: ReferenceFrame, S2: ReferenceFrame, S3: ReferenceFrame> combineTransforms(transform1: Transform<S1, S2>, transform2: Transform<S2, S3>) : Transform<S1, S3>{
    val newTranslation = transform1.translation.rotate(transform2.rotation) + transform2.translation
    val newRotation = transform1.rotation + transform2.rotation
    val newZHeight = transform1.zHeight + transform2.zHeight
    return Transform(newTranslation, newRotation, newZHeight)
}

data class Pose<R: ReferenceFrame>(val coordinate: Coordinates<R>, val orientation: Orientation<R>, val zHeight: ZHeight<R> ){
    constructor() : this(Coordinates(Vector2()), Orientation(0.0), ZHeight(0.0))
    fun <R2 : ReferenceFrame> applyTransform(transform: Transform<R, R2>) : Pose<R2>{
        return Pose<R2>(
            this.coordinate.applyTransform(transform),
            this.orientation.applyTransform(transform),
            this.zHeight.applyTransform(transform)
        )
    }
}
