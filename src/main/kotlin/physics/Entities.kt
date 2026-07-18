package physics

import effects.Effect
import effects.EffectsConsumer
import graphics.Renderer.*
import graphics.HasNestedRenderables
import math.*
import math.Orientation
import kotlin.math.sin

class Entity(): HasNestedRenderables<WorldReferenceFrame, EntityReferenceFrame>{

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
     * Hull - Physical - the structure. Stations, Pawns reside in hull. Modules can connect to hull
     * Modules - Physical - anything functional for 1the entity. Turrets, Thrusters, Radar... Anything that might act or provide
     *   -> mostly just to hold state, should have minimal logic
     * Stations - Pseudophysical - A way for pawns to interact with systems. Exists within hull, basically just checkpoints
     *   -> should only provide actions around declarative statements
     *   -> no state. just a real-world adapter for pawn->module interactions
     * Systems - Non Physical - The connection between state, stations and modules.
     *   ->  Holds all the imperative logic and execution of decisions
    **/
    private val hulls = mutableListOf<EntityHull>()
    private val modules = mutableListOf<EntityModule>()
    private val systems = mutableListOf<EntitySystem>()
    private val stations = mutableListOf<EntityStation>()
    private val pawns = mutableListOf<Pawn>()

    private val hullToHullMap = mutableMapOf<EntityHull, List<EntityHull>>() // Two-way
    private val hullToModuleMap = mutableMapOf<EntityHull, List<EntityModule>>()
    private val hullToStationMap = mutableMapOf<EntityHull, List<EntityStation>>()

    fun getCenterOfMass() : Coordinates<EntityReferenceFrame>{
        var cumulativeMassVector = Vector2()
        var cumulativeMassValue = 0.0

        val thingsWithMass = getModules().toMutableList() + getHull().toMutableList()

        for (module in thingsWithMass) {
            cumulativeMassVector += module.centerOfMass.applyTransform(getTransformLocalToParentFrame(module)).getVector() * module.getMass()
            cumulativeMassValue += module.getMass()
        }

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

    fun getRotationalVelocity(): Double {
        return rotVelocity
    }

    fun getVelocity(): Vector2 {
        return vel
    }

    fun setRotationalVelocity(rotVel: Double) {
        rotVelocity = rotVel
    }

    fun setVelocity(vel: Vector2) {
        this.vel = vel
    }

    fun rotate(rotation: Double) {
        this.orientation += rotation
    }

    fun translate(translation: Vector2) {
        this.coordinates += translation
    }

    //TODO Flesh this out
    fun getMass(): Double {
        return getModules().sumOf { it.getMass() } + getHull().sumOf { it.getMass() }
    }

    fun update(timeStep: Double) {
        for(system in systems){
            system.update(timeStep, this)
        }
    }

    // Just to double check https://www.physics.uoguelph.ca/torque-and-rotational-motion-tutorial
    fun applyForce(force: Force) {
        forceAccumulator += force.vector
        currentForces.add(force)

        val comToForce: Vector2 = force.origin - getCenterOfMass()
        val r = comToForce.getMagnitude();
        val theta = force.vector.getAngleTo(comToForce)
        val torque = r * force.vector.getMagnitude() * sin(theta)
        applyTorque(torque)
    }

    fun applyTorque(torque: Double) {
        torqueAccumulator += torque
    }

    fun checkAndResetNetForce(): Vector2 {
        val netForce = forceAccumulator
        forceAccumulator = Vector2(0.0, 0.0)
        lastForces = currentForces
        currentForces = mutableListOf<Force>()
        return netForce
    }

    fun checkAndResetNetTorque(): Double {
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

    fun addStation(station: EntityStation){
        stations.add(station)
    }

    fun addSystem(system: EntitySystem){
        systems.add(system)
    }

    fun getLastForces(): List<Force> {
        return lastForces
    }

    fun sendEffect(effect: Effect){
        if(effectsConsumer != null){
            effectsConsumer!!.addEffect(effect)
        }else{
            throw NullPointerException("EffectsConsumer not set!")
        }
    }
    fun sendEntity(entity: Entity){
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

    fun getPawnsInside(): List<Pawn> {
        return pawns
    }

    fun getHull(): List<EntityHull> {
        return hulls
    }

    fun getModules(): List<EntityModule> {
        return modules
    }

    fun setPose(pose: Pose<WorldReferenceFrame>){
        coordinates = pose.coordinate
        orientation = pose.orientation
        zheight = pose.zHeight
    }

    fun getSystems() : List<EntitySystem>{
        return systems;
    }
}