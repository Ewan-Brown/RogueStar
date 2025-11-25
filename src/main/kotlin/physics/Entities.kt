package physics

import graphics.Graphics
import math.*
import kotlin.math.abs
import kotlin.math.sin

interface EntityI {
    fun getWorldTransform() : Transformation2
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

    fun scale(scale: Double)

    fun getCenterOfMass() : Vector2
    fun getMass() : Double
    fun update(timeStep: Double)

    fun applyForce(force: Force)
    fun applyTorque(torque: Double)
    fun checkNetForce() : Vector2
    fun checkNetTorque() : Double

    fun getKinematicParts() : List<KinematicPart>
}

//TODO Add a way for entity to reference PhysicsLayerI to add new entities or create effects etc.
abstract class KinematicEntityImpl() : KinematicEntityI{

    private var position = Vector2()
    private var zpos = 0.0;
    private var rotation = 0.0
    private var scale = 1.0
    private var vel = Vector2()
    private var rotVelocity = 0.0

    private var forceAccumulator = Vector2(0.0, 0.0) //TODO Case for a mutable version of Vector2...? or is that pedantic
    private var torqueAccumulator = 0.0

    override fun getWorldTransform(): Transformation2 {
        return Transformation2(Vector2(position.getX(), position.getY()), rotation, scale)
    }

    protected abstract fun getEntityParts() : List<EntityPartI>

    //TODO This assumes that (0.0) of each part is also its center of mass! that is not enforced anywhere
    override fun getCenterOfMass() : Vector2{
        var massVector = Vector2(0.0, 0.0)
        getEntityParts().forEach {
            massVector += it.getLocalTransform().translation
        }
        return massVector / getEntityParts().size.toDouble()
    }

    private fun getFinalTransform2D(localTransform: Transformation2) : Transformation2{
        val finalTranslation = getWorldTransform().translation + (localTransform.translation * getWorldTransform().scale).rotate(getWorldTransform().rotation)
        val finalRotation = getWorldTransform().rotation + localTransform.rotation
        val finalScale = getWorldTransform().scale * localTransform.scale
        return Transformation2(finalTranslation, finalRotation, finalScale)
    }

    override fun getRenderableComponents() : List<Graphics.Renderable> {
        return getEntityParts().map {
            Graphics.Renderable(
                it.getModel(), Transformation3(getFinalTransform2D(it.getLocalTransform()), this.zpos + it.getLocalZPos()), it.getColor(), it.getMetadata()
            )
        }
    }

    override fun getKinematicParts(): List<KinematicPart> {
        return getEntityParts().map { part ->
            object : KinematicPart{
                override fun getPolygon(): List<Vector2> {
                    val transform = getFinalTransform2D(part.getLocalTransform())
                    val polygon = part.getModel().asVectors().map { point ->
                        (point * transform.scale).rotate(transform.rotation) + transform.translation
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
        this.position += translation
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
    override fun scale(scale: Double) {
        if(scale < Double.MIN_VALUE){
            throw IllegalArgumentException("scale must be above Double.MIN_VALUE, value provided is $scale")
        }
        this.scale *= scale
    }

}

open class DumbEntity() : KinematicEntityImpl() {
    private val parts: List<EntityPartI> = listOf(
        EntityPartImpl(),
    )
    override fun getEntityParts(): List<EntityPartI> { return parts }
    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }

}

class ControllableEntity() : KinematicEntityImpl() {

    private val parts: List<EntityPartI>

    init {

        val thruster = BasicThruster()
        thruster.translate(Vector2(-1.0, 0.0))
        parts = listOf(
            Cockpit(),
            BasicThruster())
    }

    override fun getEntityParts(): List<EntityPartI> { return parts }

    fun getThrusters() : List<Thruster> {return parts.filterIsInstance<Thruster>()}
    fun getTorquers() : List<Torquer> {return parts.filterIsInstance<Torquer>()}
    fun getRadars() : List<Radar> {return parts.filterIsInstance<Radar>()}

    override fun update(timeStep: Double) {
        var sumThrust: Vector2 = Vector2(0.0, 0.0)
        var sumOrigin : Vector2 = Vector2(0.0, 0.0)
        var appliedTorque: Double = 0.0
        getEntityParts().filterIsInstance<Thruster>().forEach {
            sumThrust += it.getCurrentThrust()
            sumOrigin += it.getLocalTransform().translation
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