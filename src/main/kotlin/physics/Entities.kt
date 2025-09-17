package physics

import graphics.Graphics
import math.Transformation3
import math.Vector2
import math.Vector3
import physics.PhysicsLayer.*
import kotlin.math.abs

interface EntityI {
    fun getGlobalTransform() : Transformation3
    fun markedForRemoval() : Boolean
}

interface KinematicEntity : EntityI{
    fun getVelocity() : Vector2
    fun getRotationalVelocity() : Double

    fun setVelocity(vel: Vector2)
    fun setRotationalVelocity(rotVel: Double)

    fun translate(translation: Vector3)
    fun rotate(rotation: Double)

    fun getCenterOfMass() : Vector2
    fun getMass() : Double
    fun getRenderableComponents() : List<Graphics.Renderable>

    abstract fun update(timeStep: Double)
}

//TODO Add a way for entity to reference PhysicsLayerI to add new entities or create effects etc.
abstract class Entity(transform: Transformation3, kinematicData: KinematicData) : KinematicEntity{

    private var position = transform.translation
    private var rotation = transform.rotation
    private var scale = transform.scale
    private var vel = kinematicData.velocity
    private var rotVelocity = kinematicData.rotationalVelocity

    override fun getGlobalTransform(): Transformation3 {
        return Transformation3(position, rotation, scale)
    }

    protected abstract fun getEntityParts() : List<EntityPart>

    //TODO This assumes that (0.0) of each part is also its center of mass! that is not enforced anywhere
    override fun getCenterOfMass() : Vector2{
        var massVector = Vector2(0.0, 0.0)
        getEntityParts().forEach {
            massVector += Vector2(it.getLocalTransform().translation)
        }
        return massVector / getEntityParts().size.toDouble()
    }

    override fun getRenderableComponents() : List<Graphics.Renderable> {
        val getFinalTransform = fun(localTransform : Transformation3) : Transformation3 {
            val finalTranslation = getGlobalTransform().translation + (localTransform.translation * getGlobalTransform().scale).rotate(getGlobalTransform().rotation)
            val finalRotation = getGlobalTransform().rotation + localTransform.rotation
            val finalScale = getGlobalTransform().scale * localTransform.scale
            return Transformation3(finalTranslation, finalRotation, finalScale)
        }
        return getEntityParts().map {
            Graphics.Renderable(
                it.getModel(), getFinalTransform(it.getLocalTransform()), it.getColor(), it.getMetadata()
            )
        }
    }
    abstract override fun markedForRemoval() : Boolean
    override fun getRotationalVelocity(): Double {
        return rotVelocity
    }

    override fun getVelocity(): Vector2 {
        return vel
    }

    override fun setRotationalVelocity(rotVel: Double) {
        rotVelocity = rotVel
    }

    override fun setVelocity(vel: Vector2) {
        this.vel = vel
    }

    override fun rotate(rotation: Double) {
        this.rotation += rotation
    }

    override fun translate(translation: Vector3) {
        this.position += translation
    }

    //TODO Flesh this out
    override fun getMass(): Double {
        return 1.0
    }

    fun accelerate(acceleration: Vector2){
        this.vel += acceleration
    }
    fun rotationallyAccelerate(acceleration: Double){
        this.rotVelocity += acceleration
    }
}

open class DumbEntity(transform: Transformation3, kinematicData: KinematicData) : Entity(transform, kinematicData) {
    private val parts: List<EntityPart> = listOf(DumbPart(Transformation3(Vector3(0.0, 0.0, 0.0), 0.0, 1.0),1.0))
    override fun getEntityParts(): List<EntityPart> { return parts }
    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }
}


class ControllableEntity(transform: Transformation3, kinematicData: KinematicData) : Entity(transform, kinematicData) {

    private val parts: List<EntityPart> = listOf(SuperPart(Transformation3(Vector3(0.0, 0.0, 0.0), 0.0, 1.0),1.0))

    override fun getEntityParts(): List<EntityPart> { return parts }

    public fun getThrusters() : List<Thruster> {return parts.filterIsInstance<Thruster>()}
    public fun getTorquers() : List<Torquer> {return parts.filterIsInstance<Torquer>()}
    public fun getRadars() : List<Radar> {return parts.filterIsInstance<Radar>()}

    override fun update(timeStep: Double) {
        var appliedThrust: Vector2 = Vector2(0.0, 0.0)
        var appliedTorque: Double = 0.0
        getEntityParts().filterIsInstance<Thruster>().forEach {
            appliedThrust += it.getCurrentThrust()
            val offsetVector = it.getLocalTransform()
        }
        getEntityParts().filterIsInstance<Torquer>().forEach {
            appliedTorque += it.getTorque()
        }
        if(appliedThrust.getMagnitude() > Double.MIN_VALUE){
            this.accelerate(appliedThrust/getMass())
        }
        if(abs(appliedTorque) > Double.MIN_VALUE){
            this.rotationallyAccelerate(appliedTorque/getMass())
        }
    }

    override fun markedForRemoval(): Boolean {return false }
}