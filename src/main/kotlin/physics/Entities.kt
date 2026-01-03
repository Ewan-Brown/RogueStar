package physics

import EffectsConsumer
import EntityConsumer
import controllers.ControllerTarget
import effects.Effect
import effects.SimpleParticle
import graphics.BLUE
import graphics.GREEN
import graphics.Graphics
import math.*
import math.Orientation
import kotlin.math.sin

//TODO Why is this an interface? is-a vs has-a...
interface KinematicEntityI: InReferenceFrame<WorldReferenceFrame>, HasReferenceFrame<EntityReferenceFrame>{
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
    fun getPawnsInside() : List<AbstractPawn>

    fun getPartContainedBy(coordinate : Coordinates<EntityReferenceFrame>) : EntityPartI?
}

abstract class AbstractKinematicEntity() : KinematicEntityI{

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
    
    private var parts = mutableListOf<EntityPartI>()
    private var pawns = mutableListOf<AbstractPawn>()

    //TODO This assumes that (0.0) of each part is also its center of mass, and that the mass of each part is equal!
    override fun getCenterOfMass() : Coordinates<EntityReferenceFrame>{
        var cumulativeMassVector = Vector2()
        var cumulativeMassValue = 0.0
        getParts().forEach {
            cumulativeMassVector += it.getCenterOfMass().applyTransform(getTransformLocalToParentFrame(it)).getVector()
            cumulativeMassValue += it.getMass()
        }

        val dividedMass = cumulativeMassVector / cumulativeMassValue

        return Coordinates(dividedMass)
    }

    //TODO This feels messy.
    fun getRenderables() : List<Graphics.Renderable> {

        val shipToWorld = getTransformLocalToParentFrame(this)

        val partRenderables : List<Graphics.Renderable> = getParts().map { part ->

            val partToShip = getTransformLocalToParentFrame(part)

            val partToWorldTransform = combineTransforms(partToShip, shipToWorld)

            //TODO Can we clean this up? This needs to be reused, and these magic '0.0' origins should maybe be derived somewhere...
            // Come to think of it, each part may or may not have multiple renderables... think of a cannon with hull and barrel components, separate
            // What we see below is the implicit single renderable who's coordinates are 0/0/0...
            // See Pawn implementation for good example! Lots of duplicated code here
            val partCoordInWorldSpace = Coordinates<PartReferenceFrame>(Vector2()).applyTransform(partToWorldTransform)
            val partOrientationInWorldSpace = Orientation<PartReferenceFrame>(0.0).applyTransform(partToWorldTransform)
            val partZHeightInWorldSpace = ZHeight<PartReferenceFrame>(0.0).applyTransform(partToWorldTransform)

            Graphics.Renderable(part.getModel(),
                partCoordInWorldSpace,
                partOrientationInWorldSpace,
                partZHeightInWorldSpace,
                part.getScale(),
                part.getColor(), part.getMetadata())
        }

        val pawnRenderables : List<Graphics.Renderable> = getPawnsInside().flatMap { pawn ->

            val pawnToShip = getTransformLocalToParentFrame(pawn)
            val pawnToWorldTransform: Transform<PawnReferenceFrame, WorldReferenceFrame> = combineTransforms(pawnToShip, shipToWorld)

            pawn.getRenderables().map { pawnRenderable ->

                val pawnRenderableCoordInWorldSpace = pawnRenderable.getCoordinates().applyTransform(pawnToWorldTransform)
                val pawnRenderableOrientationInWorldSpace = pawnRenderable.getOrientation().applyTransform(pawnToWorldTransform)
                val pawnRenderableZHeightInWorldSpace = pawnRenderable.getZHeight().applyTransform(pawnToWorldTransform)

                Graphics.Renderable(pawnRenderable.getModel(),

                    pawnRenderableCoordInWorldSpace,
                    pawnRenderableOrientationInWorldSpace,
                    pawnRenderableZHeightInWorldSpace,
                    pawnRenderable.getScale(),
                    pawnRenderable.getColor(), pawnRenderable.getMetadata())
            }        }

        return partRenderables + pawnRenderables
    }

    fun getParts(): List<EntityPartI> {
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

    fun addPart(part: EntityPartI) {
        parts.add(part)
    }

    fun addParts(parts: List<EntityPartImpl>) {
        for(part in parts){
            addPart(part)
        }
    }

    fun addPawn(pawn: AbstractPawn) {
        pawns.add(pawn)
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
    protected fun sendEntity(entity: AbstractKinematicEntity){
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

    override fun getPawnsInside(): List<AbstractPawn> {
        return pawns
    }

    override fun getPartContainedBy(coordinate: Coordinates<EntityReferenceFrame>): EntityPartI? {
        return getParts().firstOrNull { part ->
            val modelVectors = part.getModel().asVectors()
            val transformedVectors = modelVectors.map { vector ->
                var transformedVector = (vector * part.getScale())
                val transformedCoordinatesInPartFrame = Coordinates<PartReferenceFrame>(transformedVector)
                val partToShip = getTransformLocalToParentFrame(part)
                val transformedCoordinatesInEntityFrame = transformedCoordinatesInPartFrame.applyTransform(partToShip)
                transformedCoordinatesInEntityFrame.getVector()
            }
            doesPolygonContainPoint(transformedVectors, coordinate.getVector())
        }
    }
}

open class DumbEntity() : AbstractKinematicEntity() {
    init {
        addPart(EntityPartImpl())
    }
    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }

}

class SimpleShip() : ControllableEntity(){
    init {
        val thruster = BasicThruster()
        val cockpit = Cockpit()
        val block = BasicThruster()
        val block2 = EntityPartImpl()
        val gun = BasicGun()

        val pawn = SimplePawn()

        val color = Graphics.ColorData(1.0f, 1.0f, 1.0f, 1.0f)

        cockpit.setColor(color)
        cockpit.translate(Vector2(0.0, 0.0))

        thruster.setColor(BLUE)
        thruster.translate(Vector2(-1.0, 0.0))

        block.setColor(BLUE)
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

        addPawn(pawn)
    }
}

abstract class ControllableEntity() : AbstractKinematicEntity(), ControllerTarget {

    fun getThrusters() : List<Thruster> {return getParts().filterIsInstance<Thruster>()}
    fun getTorquers() : List<Torquer> {return getParts().filterIsInstance<Torquer>()}
    fun getRadars() : List<Radar> {return getParts().filterIsInstance<Radar>()}
    fun getGuns() : List<Gun> {return getParts().filterIsInstance<Gun>()}

    override fun update(timeStep: Double) {

        getParts().filterIsInstance<Thruster>().forEach {
            if(it.getCurrentThrust().getMagnitude() > Double.MIN_VALUE){
                val partCoordsLocal: Coordinates<EntityReferenceFrame> = it.getCoordinates()

                val partCoordsWorld: Coordinates<WorldReferenceFrame> = partCoordsLocal.applyTransform(getTransformLocalToParentFrame(this))
                val partOrientation : Orientation<WorldReferenceFrame> = it.getOrientation().applyTransform(getTransformLocalToParentFrame(this))

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
            val partCoordsLocal: Coordinates<EntityReferenceFrame> = it.getCoordinates()
            val partCoordsWorld: Coordinates<WorldReferenceFrame> = partCoordsLocal.applyTransform(getTransformLocalToParentFrame(this))

            val gunOrientation = it.getFiringOrientation().applyTransform(getTransformLocalToParentFrame(it)).applyTransform(getTransformLocalToParentFrame(this))
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
    override fun markedForControllerRemoval(): Boolean {return markedForRemoval()}
}