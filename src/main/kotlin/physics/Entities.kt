package physics

import EffectsConsumer
import EntityConsumer
import effects.Effect
import effects.SimpleParticle
import graphics.GREEN
import graphics.Graphics
import math.*
import math.Orientation
import kotlin.math.sin

interface KinematicEntityI{
    fun getVelocity() : Vector2
    fun getRotationalVelocity() : Double

    fun setVelocity(vel: Vector2)
    fun setRotationalVelocity(rotVel: Double)

    fun translate(translation: Vector2)
    fun rotate(rotation: Double)

    fun getCenterOfMass() : Coordinate<ShipSpace>
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

    fun getTransform() : Transform<ShipSpace, WorldSpace>
    fun getOrientation() : Orientation<WorldSpace>
    fun getCoordinate() : Coordinate<WorldSpace>
}

abstract class AbstractKinematicEntity() : KinematicEntityI{

    private var effectsConsumer: EffectsConsumer? = null
    private var entityConsumer: EntityConsumer? = null

    fun setEffectsConsumer(effectsConsumer: EffectsConsumer){this.effectsConsumer = effectsConsumer}
    fun setEntityConsumer(entityConsumer: EntityConsumer){this.entityConsumer = entityConsumer}

    protected fun sendEffect(effect: Effect){
        if(effectsConsumer != null){
            effectsConsumer!!.addEffect(effect)
        }else{
            throw NullPointerException("EffectsConsumer not set!")
        }
    }
    protected fun sendEntity(entity: AbstractKinematicEntity){
        if(entityConsumer != null){
            entityConsumer!!.addEntity(entity)
        }else{
            throw NullPointerException("EntityConsumer not set!")
        }
    }
    private var position = Vector2()
    private var zpos = 0.0;
    private var rotation = 0.0
    private var vel = Vector2()
    private var rotVelocity = 0.0

    private var lastForces = listOf<Force>()
    private var currentForces = mutableListOf<Force>()

    private var forceAccumulator = Vector2()
    private var torqueAccumulator = 0.0
    
    private var parts = mutableListOf<EntityPartI>()

    //TODO This assumes that (0.0) of each part is also its center of mass, and that the mass of each part is equal!
    override fun getCenterOfMass() : Coordinate<ShipSpace>{
        var cumulativeMassVector = Vector2()
        var cumulativeMassValue = 0.0
        getParts().forEach {
            cumulativeMassVector += it.getCenterOfMass().applyTransform(it.getTransform()).getVector()
            cumulativeMassValue += it.getMass()
        }

        val dividedMass = cumulativeMassVector / cumulativeMassValue

        return Coordinate(dividedMass)
    }

    fun getRenderables() : List<Graphics.Renderable> {
        println("AbstractKinematicEntity.getRenderables")
        return getParts().map { part ->
            val partToWorldTransform = combineTransforms(part.getTransform(), this.getTransform())

            //TODO Can we clean this up? This needs to be reused, and these magic '0.0' origins should maybe be derived somewhere...
            // Maybe we can package this data nicely
            val partCoordInWorldSpace = Coordinate<PartSpace>(Vector2()).applyTransform(partToWorldTransform)
            val partOrientationInWorldSpace = Orientation<PartSpace>(0.0).applyTransform(partToWorldTransform)
            val partZHeightInWorldSpace = ZHeight<PartSpace>(0.0).applyTransform(partToWorldTransform)

            println("partCoordInWorldSpace = ${partCoordInWorldSpace.getVector()}")
            println("partOrientationInWorldSpace = ${partOrientationInWorldSpace.getAngle()}")
            println("partZHeightInWorldSpace = ${partZHeightInWorldSpace.getZ()}")

            Graphics.Renderable(part.getModel(),
                partCoordInWorldSpace,
                partOrientationInWorldSpace,
                partZHeightInWorldSpace,
                part.getScale(),
                part.getColor(), part.getMetadata())
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

    final override fun getOrientation(): Orientation<WorldSpace> {
        return Orientation(rotation)
    }

    final override fun getTransform(): Transform<ShipSpace, WorldSpace> {
        return Transform(position, rotation, zpos)
    }

    final override fun getCoordinate(): Coordinate<WorldSpace> {
        return Coordinate(position)
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
        val block2 = EntityPartImpl()
        val gun = BasicGun()

        val color = Graphics.ColorData(1.0f, 1.0f, 1.0f, 1.0f)

        cockpit.setColor(color)
        cockpit.translate(Vector2(0.0, 0.0))

        thruster.setColor(color)
        thruster.translate(Vector2(-1.0, 0.0))

        block.setColor(color)
        block.translate(Vector2(1.0, 0.0))

        block2.setColor(color)
        block2.translate(Vector2(0.0, -1.0))

        gun.setColor(GREEN)
        gun.translate(Vector2(0.0, 1.0))

        addParts(listOf(
            thruster,
            cockpit,
            block,
            block2,
            gun))
    }

    fun getThrusters() : List<Thruster> {return getParts().filterIsInstance<Thruster>()}
    fun getTorquers() : List<Torquer> {return getParts().filterIsInstance<Torquer>()}
    fun getRadars() : List<Radar> {return getParts().filterIsInstance<Radar>()}
    fun getGuns() : List<Gun> {return getParts().filterIsInstance<Gun>()}

    override fun update(timeStep: Double) {
        getParts().filterIsInstance<Thruster>().forEach {
            if(it.getCurrentThrust().getMagnitude() > Double.MIN_VALUE){
                val partCoordsLocal: Coordinate<ShipSpace> = it.getCoordinate()
                val partCoordsWorld: Coordinate<WorldSpace> = partCoordsLocal.applyTransform(this.getTransform())
                val partOrientation : Orientation<WorldSpace> = it.getOrientation().applyTransform(this.getTransform())

                val force = Force(it.getCurrentThrust(), partCoordsLocal)
                this.applyForce(force);
                for(i in 0 until 100){
                    sendEffect(SimpleParticle(partCoordsWorld,
                        (it.getCurrentThrust() + this.getVelocity()).rotate(Math.random() * 0.1 * getRandomSign()) * (0.8 + Math.random()*0.2),
                        partOrientation,
                        Math.random() * 0.2 + 0.2,
                        100))
                }

            }
        }
        getParts().filterIsInstance<Torquer>().forEach {
            this.applyTorque(it.getTorque())
        }
        getParts().filterIsInstance<Gun>().forEach {
            val partCoordsLocal: Coordinate<ShipSpace> = it.getCoordinate()
            val partCoordsWorld: Coordinate<WorldSpace> = partCoordsLocal.applyTransform(this.getTransform())

            val gunOrientation = it.getFiringOrientation().applyTransform(it.getTransform()).applyTransform(this.getTransform())
            if(it.isFiring()){
                val projectile = it.createProjectile()
                //TODO introduce 'setPosition, setRotation etc that use SpacialConcepts instead of untyped vector2/double)
                projectile.translate(partCoordsWorld.getVector())
                projectile.rotate(gunOrientation.getAngle())
                projectile.setVelocity(this.getVelocity() + Vector2(gunOrientation.getAngle()) * 0.3)
                sendEntity(projectile)
            }
        }
    }

    override fun markedForRemoval(): Boolean {return false }
}