import EffectsUtils.Companion.emitThrustParticles
import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet
import kotlin.math.abs

//TODO Generalized PID utilities
class ControllerLayer : Layer{

    fun removeController(entity: PhysicsEntity){
        controllerList.removeIf{ it -> it.input.any { it == entity}}
    }

    fun calcTorqueToTurnTo(desiredAngleVec: Vector2, entity: PhysicsEntity) : Double{
        val angleDiff = desiredAngleVec.getAngleBetween(entity.transform.rotationAngle)
        return (-angleDiff*8 - entity.angularVelocity*4) * entity.mass.mass
    }

    fun doPositionalControl(entity: PhysicsEntity, targetPos: Vector2, targetOrientation: Vector2 = entity.worldCenter.to(targetPos), targetVelocity: Vector2 = Vector2()){
        val velocityVector = entity.linearVelocity
        val velocityDiff = velocityVector.to(targetVelocity)
        val vecToTarget = entity.worldCenter.to(targetPos)

        val calcThrustToGetTo : (Vector2) -> Vector2 = fun(desiredPosition) : Vector2{
            val posDiff = desiredPosition.difference(entity.worldCenter)
            return Vector2(posDiff.multiply(8.0)).add(velocityDiff.product(4.0))
        }

        val thrust : Vector2 = calcThrustToGetTo(targetPos)
        val torque : Double = calcTorqueToTurnTo(targetOrientation, entity)


        entity.applyTorque(torque)
        entity.applyForce(thrust)
        emitThrustParticles(entity, thrust)
    }

    abstract class BaseController<E : PhysicsEntity>{
        abstract fun update(input: List<E>)
    }

    abstract class Controller<E : PhysicsEntity> : BaseController<E>() {
        abstract override fun update(input : List<E>)
    }

    abstract class SingleController<E : PhysicsEntity> : Controller<E>(){
        final override fun update(input: List<E>) {
            if(input.size != 1){
                throw IllegalArgumentException("A $javaClass should only ever be attached to one entity at a time ")
            }
            update(input.first())
        }

        abstract fun update(input: E);
    }

    inner class BubbleMultiController<E : PhysicsEntity>(inline val centerGenerator: () -> Vector2 , val radius: Double, var bubbleVelocity: Vector2 = Vector2()) : Controller<E>(){

        val bubbleAnchorMap = mutableMapOf<Int, Vector2>()
        val inBubbleTrackerMap = mutableMapOf<Int, Boolean>()

        private fun randomizeAnchor(index: Int){
            bubbleAnchorMap[index] = Vector2(radius*0.5 + Math.random()*radius*0.2,0.0).rotate(Math.random()*2*Math.PI)
        }

        override fun update(entities: List<E>) {
            if(bubbleAnchorMap.isEmpty()){
                for(entity in entities){
                    val index = entity.uuid
                    inBubbleTrackerMap[index] = false
                    randomizeAnchor(index)
                }
            }
            for (entity in entities) {
                val index = entity.uuid
                val vecToBubbleCenter = entity.worldCenter.to(centerGenerator())
                if(abs(vecToBubbleCenter.magnitude) < radius){
                    inBubbleTrackerMap[index] = true
                    doPositionalControl(entity, centerGenerator().sum(bubbleAnchorMap[index]), Vector2(entity.transform.rotationAngle), bubbleVelocity)
                }else{
                    if(inBubbleTrackerMap[index]!!){
                        inBubbleTrackerMap[index] = false
                        randomizeAnchor(index)
                    }
                    doPositionalControl(entity, centerGenerator().sum(bubbleAnchorMap[index]), Vector2(entity.transform.rotationAngle), bubbleVelocity)
                }
            }
        }

    }

    fun <E : PhysicsEntity> addControllerEntry(group: List<E>, controller: Controller<E>){
        controllerList.add(ControllerInputEntry(controller, group))
    }

    private data class ControllerInputEntry<E : PhysicsEntity>(val controller: BaseController<E>, var input: List<E>){
        fun update(){
            controller.update(input)
        }
    }


    private val controllerList = mutableListOf<ControllerInputEntry<*>>()
    private val entityRequestBuffer = mutableListOf<PhysicsEntity>()

    override fun update() {
        entityRequestBuffer.clear()
        for (controllerEntityEntry in controllerList) {
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

class PlayerController(val input: BitSet) : ControllerLayer.SingleController<ShipEntity>(){
    override fun update(entity: ShipEntity) {
        var x = 0.0
        var y = 0.0
        var r = 0.0

        if(input[KeyEvent.VK_W]){
            x++
        }
        if(input[KeyEvent.VK_S]){
            x--
        }
        if(input[KeyEvent.VK_A]){
            y++
        }
        if(input[KeyEvent.VK_D]){
            y--
        }

        if(input[KeyEvent.VK_Q]){
            r++
        }

        if(input[KeyEvent.VK_E]){
            r--
        }

        var needsRotation = true

        if(input[KeyEvent.VK_SPACE]){
            x = -entity.linearVelocity.x/10.0
            y = -entity.linearVelocity.y/10.0
            needsRotation = false
        }

        if(input[KeyEvent.VK_X]){
            //TODO Maybe make this a little more abstracted, I don't like having to directly affect kinematics from this layer when we can avoid it...
            val newEntity = ProjectileEntity(entity.team)
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
        entity.applyTorque(rotate*10.0)
    }
}