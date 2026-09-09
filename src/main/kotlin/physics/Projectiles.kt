package physics

import effects.Effect
import effects.EffectsConsumer
import graphics.HasNestedRenderables
import graphics.Renderer
import graphics.Renderer.IntermediaryRenderable
import math.Coordinates
import math.EntityReferenceFrame
import math.Orientation
import math.Pose
import math.Vector2
import math.WorldReferenceFrame
import math.ZHeight
import math.getTransformParentToLocalFrame
import models.Model
import kotlin.math.sin

sealed interface CollisionTrigger
data class PointCollisionTrigger(val point: Coordinates<EntityReferenceFrame>) : CollisionTrigger
data class LineCollisionTrigger(val point1: Coordinates<EntityReferenceFrame>, val point2: Coordinates<EntityReferenceFrame>) : CollisionTrigger
data class RadiusCollisionTrigger(val point: Coordinates<EntityReferenceFrame>, val radius: Double) : CollisionTrigger
class DisabledInteraction() : CollisionTrigger

class BulletProjectile(private val mass: Double, size: Double): ComplexEntity(){

    override fun getImmediateRenderables(): List<IntermediaryRenderable<EntityReferenceFrame>> {
        return listOf(IntermediaryRenderable<EntityReferenceFrame>(
            model = Model.SQUARE,
            pose = Pose(),
            scale = 1.0,
            colorData = Renderer.ColorData(1.0f, 0.0f, 0.0f, 1.0f),
            metaData = Renderer.MetaData()
        ))
    }

    override fun getChildren(): List<HasNestedRenderables<EntityReferenceFrame, *>> {
        return emptyList()
    }

    override fun markedForRemoval() : Boolean {
        return false
    }

    override fun update(timeStep: Double) {

    }

    override fun getCollisionTriggerData(): CollisionTrigger {
        return PointCollisionTrigger(Coordinates<EntityReferenceFrame>(Vector2()))
    }
}

class LaserProjectile(private var trailLength: Double, velocity: Vector2) : ComplexEntity(){
    init {
        this.setVelocity(velocity)
    }
    private var lastPos: Coordinates<WorldReferenceFrame>? = null
    override fun getCollisionTriggerData(): CollisionTrigger {
        if(lastPos == null)
            return DisabledInteraction()
        else
            return LineCollisionTrigger(Coordinates<EntityReferenceFrame>(Vector2()) - getVelocity(), Coordinates(Vector2()))
    }

    override fun getCenterOfMass(): Coordinates<EntityReferenceFrame> {
        return Coordinates(Vector2())
    }

    override fun popNetForce(): Vector2 {
        return Vector2()
    }

    override fun popNetTorque(): Double {
        return 0.0
    }

    override fun getLastForces(): List<Force<EntityReferenceFrame>> {
        return emptyList()
    }

    override fun getImmediateRenderables(): List<IntermediaryRenderable<EntityReferenceFrame>> {
        return listOf(IntermediaryRenderable(
            model = Model.LASER,
            pose = Pose(),
            scale = 1.0,
            colorData = Renderer.ColorData(1.0f, 0.0f, 0.0f, 1.0f),
            metaData = Renderer.MetaData()
        ))
    }

    override fun getOrientation(): Orientation<WorldReferenceFrame> {
        val a = getVelocity().getAngleTo(Vector2(1.0, 0.0))
        return Orientation(a)
    }

    override fun applyLocalForce(force: Force<EntityReferenceFrame>) {}

    override fun applyWorldForce(force: Force<WorldReferenceFrame>) {}

    override fun update(timeStep: Double) {
        super.update(timeStep)
        lastPos = getCoordinates()
    }

}