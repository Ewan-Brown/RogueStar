package physics

import graphics.Graphics
import graphics.HasNestedRenderables
import main.TimeDuration
import main.Timestamp
import math.ComponentReferenceFrame
import math.Coordinates
import math.EntityReferenceFrame
import math.Orientation
import math.Pose
import math.Vector2
import math.ZHeight
import models.Model

fun square(color : Graphics.ColorData = Graphics.ColorData(1.0f, 1.0f, 1.0f, 1.0f)) : Graphics.IntermediaryRenderable<ComponentReferenceFrame>{ return Graphics.IntermediaryRenderable<ComponentReferenceFrame>(
    Model.SQUARE,
    Pose(),
    1.0,
    color,
    Graphics.MetaData(1.0f))
}


abstract class AbstractComponent(val boundary: List<Vector2>, val mass: Double, val centerOfMass: Vector2) : HasNestedRenderables<EntityReferenceFrame, ComponentReferenceFrame>{

    private var coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private var ZHeight: ZHeight<EntityReferenceFrame> = ZHeight(0.0)

    fun isCollideable(): Boolean {
        return true
    }

    fun getCollisionBoundary(): List<Coordinates<ComponentReferenceFrame>> {
        return boundary.map { Coordinates(it) }
    }

    override fun getCoordinates(): Coordinates<EntityReferenceFrame> {
        return coordinates
    }

    override fun getOrientation(): Orientation<EntityReferenceFrame> {
        return orientation
    }

    override fun getZHeight(): ZHeight<EntityReferenceFrame> {
        return ZHeight
    }

    fun rotate(rotation: Double) {
        this.orientation += rotation
    }

    fun translate(translation: Vector2) {
        this.coordinates += translation
    }

}

abstract class EntityHull(boundary: List<Vector2>, mass: Double, centerOfMass: Vector2) : AbstractComponent(boundary, mass, centerOfMass)
abstract class EntityModule(boundary: List<Vector2>, mass: Double, centerOfMass: Vector2) : AbstractComponent(boundary, mass, centerOfMass)

class DummyHull : EntityHull(Model.SQUARE.asVectors(), 1.0, Vector2()){

    override fun getImmediateRenderables(): List<Graphics.IntermediaryRenderable<ComponentReferenceFrame>> {
        return listOf(square(Graphics.ColorData(1.0f, 0.0f, 0.0f, 0.0f)))
    }
}

class Thruster : EntityModule(Model.SQUARE.asVectors(), 1.0, Vector2()){
    var thrusterOrientation : Orientation<ComponentReferenceFrame> = Orientation(0.0)
    var thrusterThrottle: Double = 0.0
    var thrustForceOrigin: Coordinates<ComponentReferenceFrame> = Coordinates(Vector2())

    override fun getImmediateRenderables(): List<Graphics.IntermediaryRenderable<ComponentReferenceFrame>> {
        return listOf(square(Graphics.ColorData(0.0f, 1.0f, 1.0f, 0.0f)))
    }
}

class Torquer : EntityModule(Model.SQUARE.asVectors(), 1.0, Vector2()){
    var torque: Double = 0.0

    override fun getImmediateRenderables(): List<Graphics.IntermediaryRenderable<ComponentReferenceFrame>> {
        return listOf(square(Graphics.ColorData(0.0f, 1.0f, 1.0f, 0.0f)))
    }

}

class Weapon() : EntityModule(Model.SQUARE.asVectors(), 1.0, Vector2()){
    var projectileSpawnLocation : Coordinates<ComponentReferenceFrame> = Coordinates(Vector2(2.0, 0.0))
    var isToggledOn = false;
    val maxCooldown: Double = 10.0
    var cooldownRemaining: Double = 0.0

    override fun getImmediateRenderables(): List<Graphics.IntermediaryRenderable<ComponentReferenceFrame>> {
        return listOf(square(Graphics.ColorData(1.0f, 0.0f, 1.0f, 0.0f)))
    }
}