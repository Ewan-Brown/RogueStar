package physics

import EffectsConsumer
import EntityConsumer
import effects.Effect
import graphics.Graphics.*
import graphics.HasNestedRenderables
import math.*
import math.Orientation
import javax.swing.text.html.parser.Entity
import kotlin.math.sin

interface EntityI: HasNestedRenderables<WorldReferenceFrame, EntityReferenceFrame> {
    fun getVelocity() : Vector2
    fun getRotationalVelocity() : Double

    fun setVelocity(vel: Vector2)
    fun setRotationalVelocity(rotVel: Double)

    fun translate(translation: Vector2)
    fun rotate(rotation: Double)

    fun getCenterOfMass() : Coordinates<EntityReferenceFrame>
    fun getMass() : Double
    fun update(timeStep: Double)

    fun applyForce(force: Force)
    fun applyTorque(torque: Double)
    fun checkNetForce() : Vector2
    fun checkNetTorque() : Double

    fun getLastForces(): List<Force>
    fun getPawnsInside() : List<Pawn>
    fun getHull() : List<EntityHull>
    fun getModules() : List<EntityModule>
}


interface EntityComponent : HasNestedRenderables<EntityReferenceFrame, ComponentReferenceFrame>{
    fun getMass(): Double
    fun isCollideable() : Boolean
    fun getCenterOfMass() : Coordinates<ComponentReferenceFrame>
    fun getCollisionBoundary() : List<Coordinates<ComponentReferenceFrame>>
}

abstract class AbstractComponent(val boundary: List<Vector2>) : EntityComponent{

    private val coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private val orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private val ZHeight: ZHeight<EntityReferenceFrame> = ZHeight(0.0)

    override fun isCollideable(): Boolean {
        return true
    }

    override fun getCollisionBoundary(): List<Coordinates<ComponentReferenceFrame>> {
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

}

abstract class EntityHull(boundary: List<Vector2>, coordinates: Coordinates<EntityReferenceFrame>, orientation: Orientation<EntityReferenceFrame>,
                          ZHeight: ZHeight<EntityReferenceFrame>) : AbstractComponent(boundary)
abstract class EntityModule(boundary: List<Vector2>, coordinates: Coordinates<EntityReferenceFrame>, orientation: Orientation<EntityReferenceFrame>,
                            ZHeight: ZHeight<EntityReferenceFrame>) : AbstractComponent(boundary)

abstract class AbstractEntity() : EntityI{

    private var effectsConsumer: EffectsConsumer? = null
    private var entityConsumer: EntityConsumer? = null

    private var position: Coordinates<WorldReferenceFrame> = Coordinates(Vector2())
    private var zpos: ZHeight<WorldReferenceFrame> = ZHeight(0.0)
    private var rotation: Orientation<WorldReferenceFrame> = Orientation(0.0)

    private var vel = Vector2()
    private var rotVelocity = 0.0

    private var lastForces = listOf<Force>()
    private var currentForces = mutableListOf<Force>()

    private var forceAccumulator = Vector2()
    private var torqueAccumulator = 0.0
    
    private var pawns = mutableListOf<Pawn>()
    private var hulls = mutableListOf<EntityHull>()
    private var modules = mutableListOf<EntityModule>()

    //TODO This assumes that (0.0) of each part is also its center of mass, and that the mass of each part is equal!
    override fun getCenterOfMass() : Coordinates<EntityReferenceFrame>{
        var cumulativeMassVector = Vector2()
        var cumulativeMassValue = 0.0
//        getParts().forEach {
//            cumulativeMassVector += it.getCenterOfMass().applyTransform(getTransformLocalToParentFrame(it)).getVector()
//            cumulativeMassValue += it.getMass()
//        }

        val dividedMass = cumulativeMassVector / cumulativeMassValue

        return Coordinates(dividedMass)
    }

    override fun getImmediateRenderables(): List<IntermediaryRenderable<EntityReferenceFrame>> {
        return emptyList()
    }

    override fun getChildren(): List<HasNestedRenderables<EntityReferenceFrame, *>> {
        return getPawnsInside() + getHull() + getModules()
    }

    abstract fun markedForRemoval() : Boolean

    //TODO Make this stuff reusable, for things like pawns etc.

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

    fun addPawn(pawn: Pawn) {
        pawns.add(pawn)
    }

    fun addModule(module: EntityModule) {
        modules.add(module)
    }

    fun addHull(hull: EntityHull) {
        hulls.add(hull)
    }

    override fun getLastForces(): List<Force> {
        return lastForces
    }

    fun setEffectsConsumer(effectsConsumer: EffectsConsumer){this.effectsConsumer = effectsConsumer}
    fun setEntityConsumer(entityConsumer: EntityConsumer){this.entityConsumer = entityConsumer}

    protected fun sendEffect(effect: Effect){
        if(effectsConsumer != null){
            effectsConsumer!!.addEffect(effect)
        }else{
            throw NullPointerException("EffectsConsumer not set!")
        }
    }
    protected fun sendEntity(entity: AbstractEntity){
        if(entityConsumer != null){
            entityConsumer!!.addEntity(entity)
        }else{
            throw NullPointerException("EntityConsumer not set!")
        }
    }

    override fun getZHeight(): ZHeight<WorldReferenceFrame> {
        return zpos
    }

    final override fun getOrientation(): Orientation<WorldReferenceFrame> {
        return rotation
    }

    final override fun getCoordinates(): Coordinates<WorldReferenceFrame> {
        return position
    }

    override fun getPawnsInside(): List<Pawn> {
        return pawns
    }

    override fun getHull(): List<EntityHull> {
        return hulls
    }

    override fun getModules(): List<EntityModule> {
        return modules
    }
}

open class DumbEntity() : AbstractEntity() {

    init {

        val hull = DummyHull()
        this.addHull(hull)
    }

    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }

}
//
//class SimpleShip() : ControllableEntity(){
//
//    val navStation: Station
//    val weaponStation: Station
//
//    init {
//        val thruster = BasicThruster()
//        val cockpit = Cockpit()
//        val thruster2 = BasicThruster()
//        val hull = EntityPartImpl()
//        val gun = BasicGun()
//        val pawn = DumbPawn()
//        val pawn2 = DumbPawn()
//
//        val color = Graphics.ColorData(1.0f, 1.0f, 1.0f, 1.0f)
//
//        cockpit.setColor(PURPLE)
//        cockpit.translate(Vector2(0.0, 0.0))
//
//        thruster.setColor(BLUE)
//        thruster.translate(Vector2(-1.0, 0.0))
//
//        thruster2.setColor(BLUE)
//        thruster2.translate(Vector2(1.0, 0.0))
//
//        hull.setColor(color)
//        hull.translate(Vector2(0.0, -1.0))
//
//        gun.setColor(GREEN)
//        gun.translate(Vector2(0.0, 1.0))
//
//        navStation = Station(Coordinates(Vector2(0.0, 0.0)), Orientation(0.0), ZHeight(0.0))
//        weaponStation = Station(Coordinates(Vector2(0.0, 1.0)), Orientation(0.0), ZHeight(0.0))
//
//        addParts(listOf(
//            thruster,
//            cockpit,
//            thruster2,
//            hull,
//            gun))
//
//        pawn.translate(Vector2(0.0, -1.0))
//        pawn2.translate(Vector2(0.0, -1.0))
//
//        addPawn(pawn)
//        addPawn(pawn2)
//
//    }
//
//    fun getThrusters() : List<Thruster> {return getParts().filterIsInstance<Thruster>()}
//    fun getTorquers() : List<Torquer> {return getParts().filterIsInstance<Torquer>()}
//    fun getRadars() : List<Radar> {return getParts().filterIsInstance<Radar>()}
//    fun getGuns() : List<Gun> {return getParts().filterIsInstance<Gun>()}
//}
//
//open class ControllableEntity() : AbstractEntity() {
//
//    override fun update(timeStep: Double) {
//
//        getParts().filterIsInstance<Thruster>().forEach {
//            if(it.getCurrentThrust().getMagnitude() > Double.MIN_VALUE){
//                val partCoordsLocal: Coordinates<EntityReferenceFrame> = it.getCoordinates()
//
//                val partCoordsWorld: Coordinates<WorldReferenceFrame> = partCoordsLocal.applyTransform(getTransformLocalToParentFrame(this))
//                val partOrientation : Orientation<WorldReferenceFrame> = it.getOrientation().applyTransform(getTransformLocalToParentFrame(this))
//
//                val force = Force(it.getCurrentThrust(), partCoordsLocal)
//                this.applyForce(force);
//                for(i in 0 until 100){
//                    sendEffect(SimpleParticle(partCoordsWorld,
//                        (it.getCurrentThrust() + this.getVelocity()).rotate(Math.random() * 0.1 * getRandomSign()) * (0.8 + Math.random()*0.2),
//                        partOrientation,
//                        Math.random() * 0.2 + 0.2,
//                        100))
//                }
//
//            }
//        }
//        getParts().filterIsInstance<Torquer>().forEach {
//            this.applyTorque(it.getTorque())
//        }
//        getParts().filterIsInstance<Gun>().forEach {
//            val partCoordsLocal: Coordinates<EntityReferenceFrame> = it.getCoordinates()
//            val partCoordsWorld: Coordinates<WorldReferenceFrame> = partCoordsLocal.applyTransform(getTransformLocalToParentFrame(this))
//
//            val gunOrientation = it.getFiringOrientation().applyTransform(getTransformLocalToParentFrame(it)).applyTransform(getTransformLocalToParentFrame(this))
//            if(it.isFiring()){
//                val projectile = it.createProjectile()
//                projectile.translate(partCoordsWorld.getVector())
//                projectile.rotate(gunOrientation.getAngle())
//                projectile.setVelocity(this.getVelocity() + Vector2(gunOrientation.getAngle()) * 0.3)
//                sendEntity(projectile)
//            }
//        }
//    }
//
//    override fun markedForRemoval(): Boolean {return false }
//}

//class Station(private val pose: Pose<EntityReferenceFrame>) : InReferenceFrame<EntityReferenceFrame> {
//    override fun getPose(): Pose<EntityReferenceFrame> {
//        return pose
//    }
//}

