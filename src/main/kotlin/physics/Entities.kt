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
    fun getRenderableComponents() : List<Graphics.Renderable>
}

interface KinematicPart{
    fun getPolygon() : List<Vector2>
}

interface KinematicEntityI : EntityI{
    fun getVelocity() : Vector2
    fun getRotationalVelocity() : Double

    fun setVelocity(vel: Vector2)
    fun setRotationalVelocity(rotVel: Double)

    fun translate(translation: Vector3)
    fun rotate(rotation: Double)

    fun getCenterOfMass() : Vector2
    fun getMass() : Double
    fun update(timeStep: Double)

    fun getKinematicParts() : List<KinematicPart>
}

//TODO Add a way for entity to reference PhysicsLayerI to add new entities or create effects etc.
abstract class KinematicEntityImpl(transform: Transformation3, kinematicData: KinematicData) : KinematicEntityI{

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

open class DumbEntity(transform: Transformation3, kinematicData: KinematicData) : KinematicEntityImpl(transform, kinematicData) {
    private val parts: List<EntityPart> = listOf(
        DumbPart(Transformation3(Vector3(0.0, 0.0, 0.0), 0.0, 1.0),1.0),
        DumbPart(Transformation3(Vector3(1.0, 1.0, 0.0), 0.0, 1.0),1.0)
    )
    override fun getEntityParts(): List<EntityPart> { return parts }
    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }
}


class ControllableEntity(transform: Transformation3, kinematicData: KinematicData) : KinematicEntityImpl(transform, kinematicData) {

    private val parts: List<EntityPart> = listOf(
        SuperPart(Transformation3(Vector3(0.0, 0.0, 0.0), 0.0, 1.0),1.0),
//        SuperPart(Transformation3(Vector3(1.0, 0.0, 0.0), 0.0, 1.0),1.0),
//        SuperPart(Transformation3(Vector3(-1.0, 0.0, 0.0), 0.0, 1.0),1.0)
    )

    override fun getEntityParts(): List<EntityPart> { return parts }

    public fun getThrusters() : List<Thruster> {return parts.filterIsInstance<Thruster>()}
    public fun getTorquers() : List<Torquer> {return parts.filterIsInstance<Torquer>()}
    public fun getRadars() : List<Radar> {return parts.filterIsInstance<Radar>()}

    override fun update(timeStep: Double) {
        var appliedThrust: Vector2 = Vector2(0.0, 0.0)
        var appliedTorque: Double = 0.0
        getEntityParts().filterIsInstance<Thruster>().forEach {
            appliedThrust += it.getCurrentThrust()
            val offsetVector = (it as EntityPart).getLocalTransform() //TODO This is awkward.
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