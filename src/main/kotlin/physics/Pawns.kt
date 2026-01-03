package physics

import controllers.ControllerTarget
import graphics.Graphics
import graphics.RED
import math.Coordinates
import math.InReferenceFrame
import math.Orientation
import math.PawnReferenceFrame
import math.EntityReferenceFrame
import math.HasReferenceFrame
import math.Vector2
import math.ZHeight
import models.Model


// TODO It might be neat if pawns could be agnostic to their frame of reference? (InReferenceFrame<F : ReferenceFrame>)
abstract class AbstractPawn : HasReferenceFrame<PawnReferenceFrame>, InReferenceFrame<EntityReferenceFrame>{
    abstract fun getRenderables() : List<PawnRenderablePart>

    private var coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private var zHeight: ZHeight<EntityReferenceFrame> = ZHeight(1.0)

    private var velocity: Vector2 = Vector2()

    //Temporary variable...? Currently used to make sure graphics aligns with boundary checks
    val size: Double = 0.2

    fun getVelocity() : Vector2 {return velocity}
    fun setVelocity(vel: Vector2) {
        this.velocity = vel
    }
    fun translate(translation: Vector2) {
        this.coordinates += translation
    }

    override fun getCoordinates() = coordinates
    override fun getOrientation() = orientation
    override fun getZHeight() = zHeight

    abstract fun markedForRemoval(): Boolean
}

abstract class PawnRenderablePart : InReferenceFrame<PawnReferenceFrame>{
    abstract fun getModel() : Model
    abstract fun getScale() : Double
    abstract fun getColor() : Graphics.ColorData
    abstract fun getMetadata() : Graphics.MetaData
}

class SimplePawn() : AbstractPawn(){
    override fun getRenderables(): List<PawnRenderablePart> {
        return listOf(object : PawnRenderablePart() {
            override fun getModel(): Model {
                return Model.SQUARE
            }

            override fun getScale(): Double {
                return size
            }

            override fun getColor(): Graphics.ColorData {
                return RED
            }

            override fun getMetadata(): Graphics.MetaData {
                return Graphics.MetaData(1.0f)
            }

            override fun getCoordinates(): Coordinates<PawnReferenceFrame> {
                return Coordinates(Vector2())
            }

            override fun getOrientation(): Orientation<PawnReferenceFrame> {
                return Orientation(0.0)
            }

            override fun getZHeight(): ZHeight<PawnReferenceFrame> {
                return ZHeight(0.0)
            }

        })
    }

    override fun markedForRemoval(): Boolean {
        return false;
    }
}