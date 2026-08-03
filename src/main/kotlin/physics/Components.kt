package physics

import graphics.Renderer
import graphics.HasNestedRenderables
import graphics.Renderer.IntermediaryRenderable
import math.ComponentReferenceFrame
import math.Coordinates
import math.EntityReferenceFrame
import math.Orientation
import math.Pose
import math.Vector2
import math.ZHeight
import models.Model

fun square(color : Renderer.ColorData = Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), scale: Double) : Renderer.IntermediaryRenderable<ComponentReferenceFrame>{
    return Renderer.IntermediaryRenderable(
        Model.SQUARE,
        Pose(),
        scale,
        color,
        Renderer.MetaData(1.0f)
    )
}


abstract class Component(bB: List<Vector2>, centerOfMass: Vector2) : HasNestedRenderables<EntityReferenceFrame, ComponentReferenceFrame>{

    val centerOfMass = Coordinates<ComponentReferenceFrame>(centerOfMass)
    private val boundingBox: List<Coordinates<ComponentReferenceFrame>> = bB.map { Coordinates(it) }
    private var coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private var ZHeight: ZHeight<EntityReferenceFrame> = ZHeight(0.0)
    private var staticRenderables = mutableListOf<IntermediaryRenderable<ComponentReferenceFrame>>()

    fun addStaticRenderable(renderable: IntermediaryRenderable<ComponentReferenceFrame>){
        staticRenderables.add(renderable)
    }

    fun addStaticRenderables(renderable: List<IntermediaryRenderable<ComponentReferenceFrame>>){
        staticRenderables.addAll(renderable)
    }

    fun getDynamicRenderables(): List<IntermediaryRenderable<ComponentReferenceFrame>> {
        return emptyList()
    }

    final override fun getImmediateRenderables() : List<IntermediaryRenderable<ComponentReferenceFrame>> {
        return staticRenderables + getDynamicRenderables()
    }

    fun isCollideable(): Boolean {
        return true
    }

    fun getCollisionBoundary(): List<Coordinates<ComponentReferenceFrame>> {
        return boundingBox
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

    fun translateZ(z: Double){
        this.ZHeight += z;
    }

    abstract fun getMass() : Double
}

class EntityHull(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : Component(boundingBox, centerOfMass){
//    override fun getImmediateRenderables(): List<Renderer.IntermediaryRenderable<ComponentReferenceFrame>> {
//        return listOf(square(Renderer.ColorData(1.0f, 0.0f, 0.0f, 0.0f)))
//    }

    override fun getMass(): Double {
        return mass
    }
}

abstract class EntityModule(boundingBox: List<Vector2>, centerOfMass: Vector2) : Component(boundingBox, centerOfMass)

class Thruster(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : EntityModule(boundingBox, centerOfMass){
    var thrusterOrientation : Orientation<ComponentReferenceFrame> = Orientation(0.0)
    var thrusterThrottle: Double = 0.0
    var thrustForceOrigin: Coordinates<ComponentReferenceFrame> = Coordinates(Vector2())

    override fun getMass(): Double {
        return mass
    }
}

class Torquer(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : EntityModule(boundingBox, centerOfMass){
    var torque: Double = 0.0

    override fun getMass(): Double {
        return mass
    }

}

abstract class Weapon(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : EntityModule(boundingBox, centerOfMass){

    override fun getMass(): Double {
        return mass
    }
}

class BulletWeapon(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : Weapon(boundingBox, mass, centerOfMass){
    fun getProjectileSpawnLocation() : Coordinates<ComponentReferenceFrame> = Coordinates(Vector2(2.0, 0.0))
}

class AmmoDepot(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : EntityModule(boundingBox, centerOfMass){

    var ammoStored = 0;

    override fun getMass(): Double {
        return mass
    }
}

class Battery(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : EntityModule(boundingBox, centerOfMass){

    var chargeStored = 0;

    override fun getMass(): Double {
        return mass
    }
}