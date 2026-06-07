package physics

import graphics.Renderer.*
import graphics.HasNestedRenderables
import graphics.RED
import math.Coordinates
import math.Orientation
import math.PawnReferenceFrame
import math.EntityReferenceFrame
import math.Pose
import math.Vector2
import math.ZHeight
import models.Model
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


// TODO It might be neat if pawns could be agnostic to their frame of reference? (InReferenceFrame<F : ReferenceFrame>)
abstract class Pawn() : HasNestedRenderables<EntityReferenceFrame, PawnReferenceFrame> {
    @OptIn(ExperimentalUuidApi::class)
    val UUID = Uuid.random()

    private var position: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private var rotation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private var zHeight: ZHeight<EntityReferenceFrame> = ZHeight(1.0)

    private var vel = Vector2()

    override fun getCoordinates() = position
    override fun getOrientation() = rotation
    override fun getZHeight() = zHeight

    abstract fun getMaxMovementSpeed() : Double

    fun getVelocity(): Vector2 {
        return vel
    }

    fun setVelocity(vel: Vector2) {
        this.vel = vel
    }

    fun rotate(rotation: Double) {
        this.rotation += rotation
    }

    fun translate(translation: Vector2) {
        this.position += translation
    }

}

class DumbPawn() : Pawn(){

    override fun getMaxMovementSpeed(): Double {
        return 0.03
    }

    override fun getImmediateRenderables(): List<IntermediaryRenderable<PawnReferenceFrame>> {
        return listOf(IntermediaryRenderable(
            Model.SQUARE,
            Pose(Coordinates(Vector2()), Orientation(0.0), ZHeight(0.0)),
           0.0,
            RED,
            MetaData(1.0f))
        )
    }

    override fun getChildren(): List<HasNestedRenderables<PawnReferenceFrame, *>> {
        return emptyList()
    }

}