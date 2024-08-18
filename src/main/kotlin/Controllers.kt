import EffectsUtils.Companion.emitThrustParticles
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

//TODO Generalized PID utilities
class ControllerLayer : Layer{

    fun calcTorqueToTurnTo(desiredAngleVec: Vector2, entity: PhysicsEntity) : Double{
        val angleDiff = desiredAngleVec.getAngleBetween(entity.transform.rotationAngle)
        return (-angleDiff*8 - entity.angularVelocity*4) * entity.mass.mass;
    }

    fun doPositionalControl(entity: PhysicsEntity, targetPos: Vector2, targetOrientation: Vector2 = entity.worldCenter.to(targetPos), targetVelocity: Vector2 = Vector2()){
        val velocityVector = entity.linearVelocity
        val velocityDiff = velocityVector.to(targetVelocity)
        val vecToTarget = entity.worldCenter.to(targetPos)

        val calcThrustToGetTo : (Vector2) -> Vector2 = fun(desiredPosition) : Vector2{
            val posDiff = desiredPosition.difference(entity.worldCenter)
            return Vector2(posDiff.multiply(2.0)).add(velocityDiff.product(4.0));
        }

        val thrust : Vector2 = calcThrustToGetTo(targetPos)
        val torque : Double = calcTorqueToTurnTo(targetOrientation, entity)


        entity.applyTorque(torque)
        entity.applyForce(thrust)
        emitThrustParticles(entity, thrust)
    }

    abstract class Controller<in E : PhysicsEntity> {
        abstract fun update(entity : E)
    }

    abstract class MultiController<in E : PhysicsEntity> {
        abstract fun update(entities : List<E>)

    }

    inner class BubbleMultiController<in E : PhysicsEntity>(val bubbleCenter: Vector2, val radius: Double, var bubbleVelocity: Vector2 = Vector2()) : MultiController<E>(){

        override fun update(entities: List<E>) {
            for (entity in entities) {
                val vecToBubbleCenter = entity.worldCenter.to(bubbleCenter)
                if(abs(vecToBubbleCenter.magnitude) < radius){
                    doPositionalControl(entity, entity.worldCenter, Vector2(entity.transform.rotationAngle), bubbleVelocity)
                }else{
                    doPositionalControl(entity, bubbleCenter, Vector2(entity.transform.rotationAngle), bubbleVelocity)
                }
            }
        }

    }

    //TODO This should really take into account the target velocity, and needs to be a bit snappier at short distance
    inner class EncircleMultiController<in E : PhysicsEntity>() : MultiController<E>(){

        //TODO This should be immutable
        private var angleMap: MutableMap<Int, Double>? = null
        override fun update(entities: List<E>) {
            val target = physicsLayer.getBodyData().firstOrNull { it.first == 0 }
            if(target != null) {
                val angleSeparation = Math.PI * 2 / entities.size;

                //Check if the angleMap either
                // is null
                // is not of same size as passed entities
                // does not have matching elements to entities
                if (angleMap == null || entities.size != angleMap!!.size || !entities.map { it -> it.uuid }
                        .all { angleMap!!.containsKey(it) }) {
                    angleMap = mutableMapOf();
                    var angle = 0.0
                    for (entity in entities.sortedBy { it.uuid }) {
                        angleMap!![entity.uuid] = angle;
                        angle += angleSeparation
                    }
                } else {
                    for(entity in entities){
                        val angle = angleMap!![entity.uuid] ?: throw NullPointerException("angle")
                        val targetPos: Vector2 = Vector2(target.second.position).add(Vector2(angle).product(10.0))
                        val vecToTarget = entity.worldCenter.to(targetPos)
                        val vecToTargetHost = entity.worldCenter.to(target.second.position!!)
                        val velocityVector = entity.linearVelocity.to(target.second.velocity!!)

                        if(vecToTarget.magnitude < 0.2 && velocityVector.magnitude < 0.2){
                            doPositionalControl(entity, targetPos, vecToTargetHost)
                        } else{
                            doPositionalControl(entity, targetPos)
                        }

                    }
                }
            }

        }
    }

    class GridController<in E : PhysicsEntity>(var groupOrientation: Rotation = Rotation(), var groupCenter: Vector2 = Vector2()) : MultiController<E>() {
        override fun update(entities: List<E>) {
            val spacing = entities.stream().mapToDouble { it.rotationDiscRadius}.max().asDouble * 3
            val calculatePos: (Int) -> (Vector2) = {
                val index = it
                val count = entities.size
                val countSqrt = floor(sqrt(count.toDouble()))
                val targetPos = Vector2(index % countSqrt, floor(index / countSqrt)).multiply(spacing)
                targetPos.rotate(groupOrientation).add(groupCenter)
                targetPos
            }
            for ((index, entity) in entities.withIndex()) {
                val pos = calculatePos(index)
                val posDiff = pos.difference(entity.worldCenter)
                entity.applyForce(posDiff.multiply(0.3))
                if(posDiff.magnitude < 0.1){
                    entity.applyForce(entity.linearVelocity.multiply(-0.3))
                }
            }
        }
    }

    class LineController<in E : PhysicsEntity>(var p0: Vector2, var p1: Vector2) : MultiController<E>() {
        override fun update(entities: List<E>) {
            val spacing = p0.to(p1).divide(entities.size.toDouble())
            val calculatePos: (Int) -> (Vector2) = {
                val index = it
                val targetPos = p0.sum(spacing.product(index.toDouble()))
                targetPos
            }
            for ((index, entity) in entities.withIndex()) {
                val pos = calculatePos(index)
                val posDiff = pos.difference(entity.worldCenter)
                entity.applyForce(posDiff.multiply(0.3))
                if(posDiff.magnitude < 0.03){
                    entity.applyForce(entity.linearVelocity.multiply(-0.1))
                }
            }
        }
    }

    fun <E : PhysicsEntity> addControlledEntity(entity: E, controller: Controller<E>){
        val entry = ControllerEntityEntry(controller, entity)
        controllerList.add(entry)
    }

    fun <E : PhysicsEntity> addMultiControlledEntities(entities: List<E>, controller: MultiController<E>){
        val entry = MultiControllerEntityEntry(controller, entities)
        multiControllerList.add(entry)
    }

    private data class ControllerEntityEntry<E : PhysicsEntity>(val controller: Controller<E>,  var entity: E){
        fun update() {
            controller.update(entity)
        }
    }

    //TODO This 'entry' thing is fine but why do we need two types for single vs multi?
    private data class MultiControllerEntityEntry<E : PhysicsEntity>(val controller: MultiController<E>,  var entities: List<E>){
        //TODO Add a clean exit/recovery case for when one or more entities inevitably die off
        fun update() {
            controller.update(entities)
        }
    }

    private val controllerList = mutableListOf<ControllerEntityEntry<*>>()
    private val multiControllerList = mutableListOf<MultiControllerEntityEntry<*>>()
    private val entityRequestBuffer = mutableListOf<PhysicsEntity>()

    override fun update() {
        entityRequestBuffer.clear()
        controllerList.removeIf { t -> t.entity.isMarkedForRemoval() }
        for (controllerEntityEntry in controllerList) {
            controllerEntityEntry.update()
        }
        for (controllerEntityEntry in multiControllerList) {
            if(controllerEntityEntry.entities.stream().anyMatch(PhysicsEntity::isMarkedForRemoval)){
                error("Entity under a multi controller is marked for removal... Controller should deal with this?")
            }
            controllerEntityEntry.update()
        }
        for (physicsEntity in entityRequestBuffer) {
            physicsLayer.addEntity(physicsEntity)
        }
    }

    fun getNewEntityRequests() : List<PhysicsEntity>{
        return entityRequestBuffer
    }

    fun populateModelMap(modelDataMap: HashMap<Graphics.Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        //Add renderables for player perspective?? Interesting idea
    }
}

class PlayerController(val input: BitSet) : ControllerLayer.Controller<ShipEntity>(){
    override fun update(entity: ShipEntity) {
        var x = 0.0;
        var y = 0.0;
        var r = 0.0;

        if(input[KeyEvent.VK_W]){
            x++;
        }
        if(input[KeyEvent.VK_S]){
            x--;
        }
        if(input[KeyEvent.VK_D]){
            y++;
        }
        if(input[KeyEvent.VK_A]){
            y--;
        }

        if(input[KeyEvent.VK_Q]){
            r++;
        }

        if(input[KeyEvent.VK_E]){
            r--;
        }

        var needsRotation = true;

        if(input[KeyEvent.VK_SPACE]){
            x = -entity.linearVelocity.x/10.0
            y = -entity.linearVelocity.y/10.0
            needsRotation = false;
        }

        if(input[KeyEvent.VK_X]){
            //TODO Maybe make this a little more abstracted, I don't like having to directly affect kinematics from this layer when we can avoid it...
            val newEntity = ProjectileEntity(entity.team);
            val addedEntity = physicsLayer.addEntity(newEntity, entity.transform.rotationAngle, entity.worldCenter.sum(Vector2(entity.transform.rotation.toVector().product(entity.rotationDiscRadius+1))))
            addedEntity.linearVelocity = entity.linearVelocity.copy()
        }

        val desiredVelocity = Vector2(x, y)
        if(needsRotation){
            desiredVelocity.rotate(entity.transform.rotation)
        }

        val thrust = desiredVelocity.product(100.0)

        emitThrustParticles(entity, thrust)
        entity.applyForce(thrust)

        val rotate = r*5 - entity.angularVelocity
        entity.applyTorque(rotate*30.0)
    }
}

class ChaseController : ControllerLayer.Controller<ShipEntity>(){

    private var lastTarget: Int? = null

    override fun update(entity: ShipEntity) {
        val currentTarget: Int;
        if(lastTarget == null){
            val potentialTarget = physicsLayer.getBodyData().filter { it.first != entity.uuid}.firstOrNull()
            if(potentialTarget != null){
                currentTarget = potentialTarget.first
            }else{
                return
            }
        }else{
            currentTarget = lastTarget!!
        }

        if(currentTarget != null) {
            val bodyData = physicsLayer.getEntity(currentTarget);
            val posDiff = entity.worldCenter.to(bodyData?.position)
            val thrust = posDiff.normalized
            emitThrustParticles(entity, thrust)
            entity.applyForce(thrust)
        }
    }
}