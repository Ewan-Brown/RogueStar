package physics

import EffectsConsumer
import EntityConsumer
import effects.Effect
import effects.SimpleParticle
import graphics.Graphics
import math.*
import kotlin.contracts.SimpleEffect
import kotlin.math.sin

interface KinematicEntityI{
    fun getVelocity() : Vector2
    fun getRotationalVelocity() : Double

    fun setVelocity(vel: Vector2)
    fun setRotationalVelocity(rotVel: Double)

    fun translate(translation: Vector2)
    fun rotate(rotation: Double)

    fun getWorldTransform() : Transformation2

    fun scale(scale: Double)

    fun getCenterOfMass() : Vector2
    fun getMass() : Double
    fun update(timeStep: Double)

    fun applyForce(force: Force)
    fun applyTorque(torque: Double)
    fun checkNetForce() : Vector2
    fun checkNetTorque() : Double

    fun getLastForces(): List<Force>

    fun getParts(): List<EntityPartI>
    fun addPart(part: EntityPartI)
    fun addParts(parts: List<EntityPartImpl>)
}

abstract class AbstractKinematicEntity() : KinematicEntityI{

    private var effectsConsumer: EffectsConsumer? = null
    private var entityConsumer: EntityConsumer? = null

    fun setEffectsConsumer(effectsConsumer: EffectsConsumer){this.effectsConsumer = effectsConsumer}
    fun setEntityConsumer(entityConsumer: EntityConsumer){this.entityConsumer = entityConsumer}

    protected fun sendEffect(effect: Effect){
        if(effectsConsumer != null){effectsConsumer!!.addEffect(effect)}
    }
    protected fun sendEntity(entity: AbstractKinematicEntity){
        if(entityConsumer != null){entityConsumer!!.addEntity(entity)}
    }
    private var position = Vector2()
    private var zpos = 0.0;
    private var rotation = 0.0
    private var scale = 1.0
    private var vel = Vector2()
    private var rotVelocity = 0.0

    private var lastForces = listOf<Force>()
    private var currentForces = mutableListOf<Force>()

    private var forceAccumulator = Vector2()
    private var torqueAccumulator = 0.0
    
    private var parts = mutableListOf<EntityPartI>()

    override fun getWorldTransform(): Transformation2 {
        return Transformation2(Vector2(position.getX(), position.getY()), rotation, scale)
    }

    //TODO This assumes that (0.0) of each part is also its center of mass, and that the mass of each part is equal!
    override fun getCenterOfMass() : Vector2{
        var massVector = Vector2(0.0, 0.0)
        getParts().forEach {
            massVector += it.getLocalTransform().translation
        }
        return (massVector / getParts().size.toDouble()).rotate(getWorldTransform().rotation) + getWorldTransform().translation
    }

    private fun getFinalTransform2D(localTransform: Transformation2) : Transformation2{
        val finalTranslation = getWorldTransform().translation + (localTransform.translation * getWorldTransform().scale).rotate(getWorldTransform().rotation)
        val finalRotation = getWorldTransform().rotation + localTransform.rotation
        val finalScale = getWorldTransform().scale * localTransform.scale
        return Transformation2(finalTranslation, finalRotation, finalScale)
    }

    fun getRenderables() : List<Graphics.Renderable> {
        return getParts().map {
            Graphics.Renderable(
                it.getModel(), Transformation3(getFinalTransform2D(it.getLocalTransform()), this.zpos + it.getLocalZPos()), it.getColor(), it.getMetadata()
            )
        }
    }

    override fun getParts(): List<EntityPartI> {
        return parts
    }

    abstract fun markedForRemoval() : Boolean
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
        currentForces.add(force)

        val comToForce: Vector2 = force.origin - getCenterOfMass()
        val r = comToForce.getMagnitude();
        val theta = force.vector.getAngleTo(comToForce)
        val torque = r * force.vector.getMagnitude() * sin(theta)
        applyTorque(torque)
    }

    override fun applyTorque(torque: Double) {
        torqueAccumulator += torque
    }

    final override fun checkNetForce(): Vector2 {
        val netForce = forceAccumulator
        forceAccumulator = Vector2(0.0, 0.0)
        lastForces = currentForces
        currentForces = mutableListOf<Force>()
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

    final override fun addPart(part: EntityPartI) {
        parts.add(part)
    }

    override fun addParts(parts: List<EntityPartImpl>) {
        for(part in parts){
            addPart(part)
        }
    }

    override fun getLastForces(): List<Force> {
        return lastForces
    }

}

open class DumbEntity() : AbstractKinematicEntity() {
    init {
        addPart(EntityPartImpl())
    }
    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }

}

class ControllableEntity() : AbstractKinematicEntity() {
    init {
        val thruster = BasicThruster()
        val cockpit = Cockpit()
        val block = BasicThruster()

        val color = Graphics.ColorData(1.0f, 1.0f, 1.0f, 1.0f)

        cockpit.setColor(color)
        cockpit.translate(Vector2(0.0, 0.0))

        thruster.setColor(color)
        thruster.translate(Vector2(-1.0, 0.0))

        block.setColor(color)
        block.translate(Vector2(1.0, 0.0))

        addParts(listOf(
            thruster,
            cockpit,
            block))
    }

    fun getThrusters() : List<Thruster> {return getParts().filterIsInstance<Thruster>()}
    fun getTorquers() : List<Torquer> {return getParts().filterIsInstance<Torquer>()}
    fun getRadars() : List<Radar> {return getParts().filterIsInstance<Radar>()}

    override fun update(timeStep: Double) {
        getParts().filterIsInstance<Thruster>().forEach {
            if(it.getCurrentThrust().getMagnitude() > Double.MIN_VALUE){
                val force = Force(it.getCurrentThrust(), it.getLocalTransform().translation.rotate(this.getWorldTransform().rotation) + this.getWorldTransform().translation)
                this.applyForce(force);
                for(i in 0 until 100){
                    sendEffect(SimpleParticle(force.origin,
                        (it.getCurrentThrust() + this.getVelocity()).rotate(Math.random() * 0.1 * getRandomSign()) * (0.8 + Math.random()*0.2),
                        it.getLocalTransform().rotation + this.getWorldTransform().rotation,
                        Math.random() * 0.2 + 0.2,
                        100))
                }

            }
        }
        getParts().filterIsInstance<Torquer>().forEach {
            this.applyTorque(it.getTorque())
        }
    }

    override fun markedForRemoval(): Boolean {return false }
}