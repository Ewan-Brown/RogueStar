package physics

import EffectsConsumer
import EntityConsumer
import effects.Effect
import graphics.Graphics.*
import graphics.HasNestedRenderables
import math.*
import math.Orientation
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


class AbstractEntity() : EntityI{

    var effectsConsumer: EffectsConsumer? = null
    var entityConsumer: EntityConsumer? = null

    private var coordinates: Coordinates<WorldReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<WorldReferenceFrame> = Orientation(0.0)
    private var zheight: ZHeight<WorldReferenceFrame> = ZHeight(0.0)

    private var vel = Vector2()
    private var rotVelocity = 0.0

    private var lastForces = listOf<Force>()
    private var currentForces = mutableListOf<Force>()

    private var forceAccumulator = Vector2()
    private var torqueAccumulator = 0.0

    /**
     * Everything that 'makes up' a ship. Sum of its parts.
     * Hull - the structure. Stations, Pawns reside in hull. Modules can connect to hull
     * Modules - anything functional for the entity. Turrets, Thrusters, Radar... Anything that might act or provide
     *   -> mostly just to hold state, should have minimal logic
     * Stations - A way for pawns to interact with systems. Exists within hull, basically just checkpoints
     *   -> should only provide actions around declarative statements
     *   -> no state. just a real-world adapter for pawn->module interactions
     * Systems - The connection between state, stations and modules.
     *   ->  Holds all the imperative logic and execution of decisions
    **/
    private var hulls = mutableListOf<EntityHull>()
    private var modules = mutableListOf<EntityModule>()
    private var systems = mutableListOf<EntitySystem>()
    private var stations = mutableListOf<EntityStation>()
    private var pawns = mutableListOf<Pawn>()

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

    //TODO Flesh this out
    fun markedForRemoval() : Boolean {
        return false
    }

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
        this.orientation += rotation
    }

    override fun translate(translation: Vector2) {
        this.coordinates += translation
    }

    //TODO Flesh this out
    override fun getMass(): Double {
        return 1.0
    }

    override fun update(timeStep: Double) {

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

    fun sendEffect(effect: Effect){
        if(effectsConsumer != null){
            effectsConsumer!!.addEffect(effect)
        }else{
            throw NullPointerException("EffectsConsumer not set!")
        }
    }
    fun sendEntity(entity: AbstractEntity){
        if(entityConsumer != null){
            entityConsumer!!.addEntity(entity)
        }else{
            throw NullPointerException("EntityConsumer not set!")
        }
    }

    override fun getZHeight(): ZHeight<WorldReferenceFrame> {
        return zheight
    }

    final override fun getOrientation(): Orientation<WorldReferenceFrame> {
        return orientation
    }

    final override fun getCoordinates(): Coordinates<WorldReferenceFrame> {
        return coordinates
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

    fun setPose(pose: Pose<WorldReferenceFrame>){
        coordinates = pose.coordinate
        orientation = pose.orientation
        zheight = pose.zHeight
    }
}