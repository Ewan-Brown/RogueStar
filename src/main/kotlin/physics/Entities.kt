package physics

import effects.Effect
import effects.EffectsConsumer
import graphics.Renderer.*
import graphics.HasNestedRenderables
import math.*
import math.Orientation
import kotlin.math.sin

data class CrudeBoundingBox(val center: Coordinates<EntityReferenceFrame>, val radius: Double)

interface CollisionData{
    fun getCrudeBoundingBox(): CrudeBoundingBox
}

interface KineticEntity : HasNestedRenderables<WorldReferenceFrame, EntityReferenceFrame>{
    fun getCenterOfMass() : Coordinates<EntityReferenceFrame>
    fun markedForRemoval() : Boolean
    fun getRotationalVelocity(): Double
    fun getVelocity(): Vector2
    fun setRotationalVelocity(rotVel: Double)
    fun setVelocity(vel: Vector2)
    fun rotate(rotation: Double)
    fun translate(translation: Vector2)
    fun getMass(): Double
    fun update(timeStep: Double)
    fun applyLocalForce(force: Force<EntityReferenceFrame>)
    fun applyWorldForce(force: Force<WorldReferenceFrame>)
    fun applyTorque(torque: Double)
    fun popNetForce(): Vector2
    fun popNetTorque(): Double
    fun getLastForces(): List<Force<EntityReferenceFrame>>
    fun sendEffect(effect: Effect)
    fun sendEntity(entity: KineticEntity)
    fun setEntityConsumer(consumer: EntityConsumer)
    fun setEffectConsumer(consumer: EffectsConsumer)
    fun setPose(pose: Pose<WorldReferenceFrame>)
    fun getCollisionTargetData() : CollisionData?
    fun getCollisionTriggerData() : CollisionTrigger?
}

//interface ProjectileEntity : KineticEntity {
//    fun getProjectileInteractionDescriptor() : ProjectileInteraction
//}

open class ComplexEntity(): KineticEntity{

    private var effectsConsumer: EffectsConsumer? = null
    private var entityConsumer: EntityConsumer? = null

    private var coordinates: Coordinates<WorldReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<WorldReferenceFrame> = Orientation(0.0)
    private var zheight: ZHeight<WorldReferenceFrame> = ZHeight(0.0)

    private var vel = Vector2()
    private var rotVelocity = 0.0

    //For Debug purposes
    private var lastForces = listOf<Force<EntityReferenceFrame>>()
    private var currentForces = mutableListOf<Force<EntityReferenceFrame>>()

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

    override fun getCenterOfMass() : Coordinates<EntityReferenceFrame>{
        var cumulativeMassVector = Vector2()
        var cumulativeMassValue = 0.0

        val componentsWithMass = getModules().toMutableList() + getHull().toMutableList() + stations.toMutableList()

        for (component in componentsWithMass) {
            cumulativeMassVector += component.centerOfMass.applyTransform(getTransformLocalToParentFrame(component)).getVector() * component.getMass()
            cumulativeMassValue += component.getMass()
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
    override fun markedForRemoval() : Boolean {
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

    override fun getMass(): Double {
        return getModules().sumOf { it.getMass() } + getHull().sumOf { it.getMass() }
    }

    override fun update(timeStep: Double) {
        for(system in systems){
            system.update(timeStep, this)
        }
    }

    // Just to double check https://www.physics.uoguelph.ca/torque-and-rotational-motion-tutorial
    override fun applyLocalForce(force: Force<EntityReferenceFrame>) {
        forceAccumulator += force.vector
        currentForces.add(force)

        val comToForce: Vector2 = force.origin - getCenterOfMass()
        val r = comToForce.getMagnitude();
        val theta = force.vector.getAngleTo(comToForce)
        val torque = r * force.vector.getMagnitude() * sin(theta)
        applyTorque(torque)
    }

    override fun applyWorldForce(force: Force<WorldReferenceFrame>) {
        val localForce = force.applyTransform(getTransformParentToLocalFrame(this))
        applyLocalForce(localForce)
    }

    override fun applyTorque(torque: Double) {
        torqueAccumulator += torque
    }

    override fun setPose(pose: Pose<WorldReferenceFrame>){
        coordinates = pose.coordinate
        orientation = pose.orientation
        zheight = pose.zHeight
    }

    override fun getCollisionTargetData(): CollisionData? {
        return object : CollisionData{
            override fun getCrudeBoundingBox() = CrudeBoundingBox(getCenterOfMass(), 1.0)
        }
    }

    override fun getCollisionTriggerData(): CollisionTrigger? {
        return null
    }

    override fun popNetForce(): Vector2 {
        val netForce = forceAccumulator
        forceAccumulator = Vector2(0.0, 0.0)
        lastForces = currentForces
        currentForces = mutableListOf<Force<EntityReferenceFrame>>()
        return netForce
    }

    override fun popNetTorque(): Double {
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

    fun addHullToHullMap(map: Map<EntityHull, List<EntityHull>>){
        hullToHullMap.putAll(map)
    }

    fun addHullToModuleMap(map: Map<EntityHull, List<EntityModule>>){
        hullToModuleMap.putAll(map)
    }

    fun addHullToStationMap(map: Map<EntityHull, List<EntityStation>>){
        hullToStationMap.putAll(map)
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

    fun getSystems() : List<EntitySystem>{
        return systems;
    }

    override fun getLastForces(): List<Force<EntityReferenceFrame>> {
        return lastForces
    }

    override fun sendEffect(effect: Effect){
        if(effectsConsumer != null){
            effectsConsumer!!.addEffect(effect)
        }else{
            throw NullPointerException("EffectsConsumer not set!")
        }
    }

    override fun sendEntity(entity: KineticEntity){
        if(entityConsumer != null){
            entityConsumer!!.addEntity(entity)
        }else{
            throw NullPointerException("EntityConsumer not set!")
        }
    }

    override fun setEntityConsumer(consumer: EntityConsumer) {
        this.entityConsumer = consumer
    }

    override fun setEffectConsumer(consumer: EffectsConsumer) {
        this.effectsConsumer = consumer
    }

    override fun getZHeight(): ZHeight<WorldReferenceFrame> {
        return zheight
    }

    override fun getOrientation(): Orientation<WorldReferenceFrame> {
        return orientation
    }

    override fun getCoordinates(): Coordinates<WorldReferenceFrame> {
        return coordinates
    }
}