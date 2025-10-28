package physics

import graphics.Graphics
import math.Transformation3
import math.Vector2
import math.Vector3
import math.extruded
import physics.PhysicsLayer.*
import kotlin.math.abs
import kotlin.math.sin

interface EntityI {
    fun getGlobalTransform() : Transformation3
    fun markedForRemoval() : Boolean
    fun getRenderableComponents() : List<Graphics.Renderable>
}

interface KinematicPart{
    fun getPolygon() : List<Vector2>
}

data class Force(val vector: Vector2, val localOrigin: Vector2)

interface KinematicEntityI : EntityI{
    fun getVelocity() : Vector2
    fun getRotationalVelocity() : Double

    fun setVelocity(vel: Vector2)
    fun setRotationalVelocity(rotVel: Double)

    fun translate(translation: Vector2)
    fun rotate(rotation: Double)

    fun getCenterOfMass() : Vector2
    fun getMass() : Double
    fun update(timeStep: Double)

    fun applyForce(force: Force)
    fun applyTorque(torque: Double)
    fun checkNetForce() : Vector2
    fun checkNetTorque() : Double

    fun getKinematicParts() : List<KinematicPart>
}

interface PointProjectileI : KinematicEntityI{
    fun getPointOfContact() : Vector2
    fun doesCollide(otherEntity: KinematicEntityI) : Boolean
}

//TODO Add a way for entity to reference PhysicsLayerI to add new entities or create effects etc.
abstract class KinematicEntityImpl(transform: Transformation3, kinematicData: KinematicData) : KinematicEntityI{

    private var position = transform.translation
    private var rotation = transform.rotation
    private var scale = transform.scale
    private var vel = kinematicData.velocity
    private var rotVelocity = kinematicData.rotationalVelocity

    private var forceAccumulator = Vector2(0.0, 0.0) //TODO Case for a mutable version of Vector2...? or is that pedantic
    private var torqueAccumulator = 0.0

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
    private fun getFinalTransform(localTransform : Transformation3) : Transformation3 {
        val finalTranslation = getGlobalTransform().translation + (localTransform.translation * getGlobalTransform().scale).rotate(getGlobalTransform().rotation)
        val finalRotation = getGlobalTransform().rotation + localTransform.rotation
        val finalScale = getGlobalTransform().scale * localTransform.scale
        return Transformation3(finalTranslation, finalRotation, finalScale)
    }
    override fun getRenderableComponents() : List<Graphics.Renderable> {
        return getEntityParts().map {
            Graphics.Renderable(
                it.getModel(), getFinalTransform(it.getLocalTransform()), it.getColor(), it.getMetadata()
            )
        }
    }

    override fun getKinematicParts(): List<KinematicPart> {
        return getEntityParts().map { part ->
            object : KinematicPart{
                override fun getPolygon(): List<Vector2> {
                    val transform = getFinalTransform(part.getLocalTransform())
                    val polygon = part.getModel().asVectors().map { point ->
                        (point * transform.scale).rotate(transform.rotation) + Vector2(transform.translation)
                    }
                    return polygon
                }
            }
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

    override fun translate(translation: Vector2) {
        this.position += translation.extruded(0.0)
    }

    //TODO Flesh this out
    override fun getMass(): Double {
        return 1.0
    }

    // Just to double check https://www.physics.uoguelph.ca/torque-and-rotational-motion-tutorial
    override fun applyForce(force: Force) {
        forceAccumulator += force.vector
        val originToForce: Vector2 = force.localOrigin - getCenterOfMass()
        val r = originToForce.getMagnitude();
        val theta = force.vector.getAngleTo(originToForce)
        applyTorque(r * force.vector.getMagnitude() * sin(theta))
    }

    override fun applyTorque(torque: Double) {
        torqueAccumulator += torque
    }

    final override fun checkNetForce(): Vector2 {
        val netForce = forceAccumulator
        forceAccumulator = Vector2(0.0, 0.0)
        return netForce
    }

    final override fun checkNetTorque(): Double {
        val netTorque = torqueAccumulator
        torqueAccumulator = 0.0;
        return netTorque
    }

}

open class DumbEntity(transform: Transformation3, kinematicData: KinematicData) : KinematicEntityImpl(transform, kinematicData) {
    private val parts: List<EntityPart> = listOf(
        DumbPart(Transformation3(Vector3(0.0, 0.0, 0.0), 0.0, 1.0)),
    )
    override fun getEntityParts(): List<EntityPart> { return parts }
    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }
}

open class DumbProjectile(transform: Transformation3, kinematicData: KinematicData) : DumbEntity(transform, kinematicData), PointProjectileI{
    override fun getPointOfContact(): Vector2 {
        return Vector2(0.0, 0.0)
    }

    override fun doesCollide(otherEntity: KinematicEntityI): Boolean {
        return false;
    }

}

class ControllableEntity(transform: Transformation3, kinematicData: KinematicData) : KinematicEntityImpl(transform, kinematicData) {

    private val parts: List<EntityPart> = listOf(
        SuperPart(Transformation3(Vector3(0.0, 0.0, 0.0), 0.0, 1.0)),
    )

    override fun getEntityParts(): List<EntityPart> { return parts }

    public fun getThrusters() : List<Thruster> {return parts.filterIsInstance<Thruster>()}
    public fun getTorquers() : List<Torquer> {return parts.filterIsInstance<Torquer>()}
    public fun getRadars() : List<Radar> {return parts.filterIsInstance<Radar>()}

    override fun update(timeStep: Double) {
        var sumThrust: Vector2 = Vector2(0.0, 0.0)
        var sumOrigin : Vector2 = Vector2(0.0, 0.0)
        var appliedTorque: Double = 0.0
        getEntityParts().filterIsInstance<Thruster>().forEach {
            sumThrust += it.getCurrentThrust()
            sumOrigin += Vector2((it as EntityPart).getLocalTransform().translation) //TODO This is awkward.
        }
        val avgOrigin = sumOrigin/getEntityParts().filterIsInstance<Thruster>().size.toDouble()
        getEntityParts().filterIsInstance<Torquer>().forEach {
            appliedTorque += it.getTorque()
        }
        if(sumThrust.getMagnitude() > Double.MIN_VALUE){
            this.applyForce(Force(sumThrust, avgOrigin))
        }
        if(abs(appliedTorque) > Double.MIN_VALUE){
            this.applyTorque(appliedTorque)
        }
    }

    override fun markedForRemoval(): Boolean {return false }
}