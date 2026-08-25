package physics

import effects.Effect
import effects.EffectsConsumer
import graphics.HasNestedRenderables
import graphics.Renderer
import graphics.Renderer.IntermediaryRenderable
import math.Coordinates
import math.EntityReferenceFrame
import math.Orientation
import math.Pose
import math.Vector2
import math.WorldReferenceFrame
import math.ZHeight
import math.getTransformLocalToParentFrame
import math.getTransformParentToLocalFrame
import models.Model
import kotlin.math.sin
import kotlin.times

sealed interface ProjectileInteraction
data class PointProjectileInteraction(val point: Coordinates<EntityReferenceFrame>) : ProjectileInteraction
data class LineProjectileInteraction(val point1: Coordinates<EntityReferenceFrame>, val point2: Coordinates<EntityReferenceFrame>) : ProjectileInteraction
data class RadiusProjectileInteraction(val point: Coordinates<EntityReferenceFrame>, val radius: Double) : ProjectileInteraction

class BulletProjectile(private val mass: Double, size: Double): ProjectileEntity{

    private var effectsConsumer: EffectsConsumer? = null
    private var entityConsumer: EntityConsumer? = null

    private var coordinates: Coordinates<WorldReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<WorldReferenceFrame> = Orientation(0.0)
    private var zheight: ZHeight<WorldReferenceFrame> = ZHeight(0.0)

    private var velocity = Vector2()
    private var rotVelocity = 0.0

    private var lastForces = listOf<Force<EntityReferenceFrame>>()
    private var currentForces = mutableListOf<Force<EntityReferenceFrame>>()

    private var forceAccumulator = Vector2()
    private var torqueAccumulator = 0.0

    override fun getImmediateRenderables(): List<IntermediaryRenderable<EntityReferenceFrame>> {
        return listOf(IntermediaryRenderable<EntityReferenceFrame>(
            model = Model.SQUARE,
            pose = Pose(),
            scale = 1.0,
            colorData = Renderer.ColorData(1.0f, 0.0f, 0.0f, 1.0f),
            metaData = Renderer.MetaData()
        ))
    }

    override fun getChildren(): List<HasNestedRenderables<EntityReferenceFrame, *>> {
        return emptyList()
    }

    override fun getCenterOfMass(): Coordinates<EntityReferenceFrame> {
        return Coordinates(Vector2())
    }

    override fun markedForRemoval() : Boolean {
        return false
    }

    override fun getRotationalVelocity(): Double {
        return rotVelocity
    }

    override fun getVelocity(): Vector2 {
        return velocity
    }

    override fun setRotationalVelocity(rotVel: Double) {
        rotVelocity = rotVel
    }

    override fun setVelocity(vel: Vector2) {
        this.velocity = vel
    }

    override fun rotate(rotation: Double) {
        this.orientation += rotation
    }

    override fun translate(translation: Vector2) {
        this.coordinates += translation
    }

    override fun getMass(): Double {
        return mass
    }

    override fun update(timeStep: Double) {

    }

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

    // Just to double check https://www.physics.uoguelph.ca/torque-and-rotational-motion-tutorial
//    fun applyForce(force: Force) {
//        forceAccumulator += force.vector
//        currentForces.add(force)
//
//        val comToForce: Vector2 = force.origin - getCenterOfMass()
//        val r = comToForce.getMagnitude();
//        val theta = force.vector.getAngleTo(comToForce)
//        val torque = r * force.vector.getMagnitude() * sin(theta)
//        applyTorque(torque)
//    }

    override fun applyTorque(torque: Double) {
        torqueAccumulator += torque
    }

    override fun setPose(pose: Pose<WorldReferenceFrame>){
        coordinates = pose.coordinate
        orientation = pose.orientation
        zheight = pose.zHeight
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

    override fun getProjectileInteractionDescriptor(): ProjectileInteraction {
        return PointProjectileInteraction(Coordinates<EntityReferenceFrame>(Vector2()))
    }
}

class LaserProjectile(trailLength: Double) : ProjectileEntity{

    private var coordinates: Coordinates<WorldReferenceFrame> = Coordinates(Vector2())
    private var zheight: ZHeight<WorldReferenceFrame> = ZHeight(0.0)
    private var velocity = Vector2()
    private var lastPos: Coordinates<WorldReferenceFrame>? = null

    private var effectsConsumer: EffectsConsumer? = null
    private var entityConsumer: EntityConsumer? = null

    override fun getProjectileInteractionDescriptor(): ProjectileInteraction {
        return LineProjectileInteraction(Coordinates<EntityReferenceFrame>(Vector2()) - velocity, Coordinates(Vector2()))
    }

    override fun getCenterOfMass(): Coordinates<EntityReferenceFrame> {
        return Coordinates<EntityReferenceFrame>(Vector2())
    }

    override fun markedForRemoval(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRotationalVelocity(): Double {
        return 0.0
    }

    override fun getVelocity(): Vector2 {
        return velocity
    }

    override fun setRotationalVelocity(rotVel: Double) {

    }

    override fun setVelocity(vel: Vector2) {
        velocity = vel
    }

    override fun rotate(rotation: Double) {
        return
    }

    override fun translate(translation: Vector2) {
        coordinates += translation
    }

    override fun getMass(): Double {
        return 0.0
    }

    override fun update(timeStep: Double) {

    }


    override fun applyTorque(torque: Double) {

    }

    override fun popNetForce(): Vector2 {
        return Vector2()
    }

    override fun popNetTorque(): Double {
        return 0.0
    }

    override fun getLastForces(): List<Force<EntityReferenceFrame>> {
        return emptyList()
    }

    override fun sendEffect(effect: Effect) {
        effectsConsumer!!.addEffect(effect)
    }

    override fun sendEntity(entity: KineticEntity) {
        entityConsumer!!.addEntity(entity)
    }

    override fun setEntityConsumer(consumer: EntityConsumer) {
        entityConsumer = consumer
    }

    override fun setEffectConsumer(consumer: EffectsConsumer) {
        effectsConsumer = consumer
    }

    override fun setPose(pose: Pose<WorldReferenceFrame>) {

    }

    override fun getImmediateRenderables(): List<IntermediaryRenderable<EntityReferenceFrame>> {
        TODO("Not yet implemented rendering for laser")
    }

    override fun getCoordinates(): Coordinates<WorldReferenceFrame> {
        return coordinates
    }

    override fun getOrientation(): Orientation<WorldReferenceFrame> {
        return Orientation(0.0) //TODO Is this ok?
    }

    override fun getZHeight(): ZHeight<WorldReferenceFrame> {
        return zheight
    }


    override fun applyLocalForce(force: Force<EntityReferenceFrame>) {
//        forceAccumulator += force.vector
//        currentForces.add(force)
//
//        val comToForce: Vector2 = force.origin - getCenterOfMass()
//        val r = comToForce.getMagnitude();
//        val theta = force.vector.getAngleTo(comToForce)
//        val torque = r * force.vector.getMagnitude() * sin(theta)
//        applyTorque(torque)
    }

    override fun applyWorldForce(force: Force<WorldReferenceFrame>) {
//        val localForce = force.applyTransform(getTransformParentToLocalFrame(this))
//        applyLocalForce(localForce)
    }

}