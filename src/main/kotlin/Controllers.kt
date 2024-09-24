import EffectsUtils.Companion.emitThrustParticles
import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet
import kotlin.math.abs
import PhysicsLayer.PhysicsBodyData

class ControllerLayer : Layer{

    fun removeController(uuid: Int){
        controllerList.removeIf {
            it.input.removeIf { it2 -> it2.uuid == uuid } //Remove this entry from the input list
            it.input.size == 0 //If that was the last entry for this controller, remove the controller too :)
        }
    }

    private fun calcTorqueToTurnTo(desiredAngleVec: Vector2, angle: Double, angularVelocity: Double) : Double{
        val angleDiff = desiredAngleVec.getAngleBetween(angle)
        return (-angleDiff*8 - angularVelocity*4)
    }

    private fun doPositionalControl(data: PhysicsLayer.PhysicsBodyData, targetPos: Vector2, targetOrientation: Vector2, targetVelocity: Vector2 = Vector2()){
        val velocityDiff: Vector2 = data.velocity.to(targetVelocity)
        val vecToTarget = data.position.to(targetPos)

        val calcThrustToGetTo : (Vector2) -> Vector2 = fun(desiredPosition) : Vector2{
            val posDiff = desiredPosition.difference(data.position)
            return Vector2(posDiff.multiply(8.0)).add(velocityDiff.product(4.0))
        }

        val thrust : Vector2 = calcThrustToGetTo(targetPos)
        val torque : Double = calcTorqueToTurnTo(targetOrientation, data.angle, data.angularVelocity)

        physicsLayer.applyForce(data.uuid, thrust)
        physicsLayer.applyTorque(data.uuid, torque)
        emitThrustParticles(data, thrust)
    }

    abstract class BaseController<E : PhysicsBodyData>{
        abstract fun update(input: List<E>)
    }

    abstract class Controller<E : PhysicsBodyData> : BaseController<E>() {
        abstract override fun update(input : List<E>)
    }

    abstract class SingleController<E : PhysicsBodyData> : Controller<E>(){
        final override fun update(input: List<E>) {
            if(input.size != 1){
                throw IllegalArgumentException("A $javaClass should only ever be attached to one entity at a time ")
            }
            update(input.first())
        }

        abstract fun update(input: E);
    }

    inner class BubbleMultiController<E : PhysicsBodyData>(inline val centerGenerator: () -> Vector2 , val radius: Double, var bubbleVelocity: Vector2 = Vector2()) : Controller<E>(){

        val bubbleAnchorMap = mutableMapOf<Int, Vector2>()
        val inBubbleTrackerMap = mutableMapOf<Int, Boolean>()

        private fun randomizeAnchor(index: Int){
            bubbleAnchorMap[index] = Vector2(radius*0.5 + Math.random()*radius*0.2,0.0).rotate(Math.random()*2*Math.PI)
        }

        override fun update(data: List<E>) {
            if(bubbleAnchorMap.isEmpty()){
                for(datum in data){
                    val index = datum.uuid
                    inBubbleTrackerMap[index] = false
                    randomizeAnchor(index)
                }
            }
            for (datum in data) {
                if(datum.position != null) {
                    val index = datum.uuid
                    val vecToBubbleCenter = datum.position.to(centerGenerator())
                    if (abs(vecToBubbleCenter.magnitude) < radius) {
                        inBubbleTrackerMap[index] = true
                        doPositionalControl(
                            datum,
                            centerGenerator().sum(bubbleAnchorMap[index]),
                            Vector2(datum.angle),
                            bubbleVelocity
                        )
                    } else {
                        if (inBubbleTrackerMap[index]!!) {
                            inBubbleTrackerMap[index] = false
                            randomizeAnchor(index)
                        }
                        doPositionalControl(
                            datum,
                            centerGenerator().sum(bubbleAnchorMap[index]),
                            Vector2(datum.angle),
                            bubbleVelocity
                        )
                    }
                }
            }
        }

    }

    fun <E : PhysicsBodyData> addControllerEntry(group: MutableList<E>, controller: Controller<E>){
        controllerList.add(ControllerInputEntry(controller, group))
    }

    private data class ControllerInputEntry<E : PhysicsBodyData>(val controller: BaseController<E>, var input: MutableList<E>){
        fun update(){
            controller.update(input)
        }
    }


    private val controllerList = mutableListOf<ControllerInputEntry<*>>()

    override fun update() {
        for (controllerEntityEntry in controllerList) {
            controllerEntityEntry.update()
        }
    }

    fun populateModelMap(modelDataMap: HashMap<Graphics.Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        //Add renderables for player perspective?? Interesting idea
    }
}

class PlayerController(val input: BitSet) : ControllerLayer.SingleController<PhysicsBodyData>(){
    override fun update(entityData: PhysicsBodyData) {
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
            x = -entityData.velocity.x/10.0
            y = -entityData.velocity.y/10.0
            needsRotation = false
        }

        if(input[KeyEvent.VK_X]){
//            val newEntity = ProjectileEntity(entity.team)
//            val addedEntity = physicsLayer.addEntity(newEntity, entity.transform.rotationAngle, entity.worldCenter.sum(Vector2(entity.transform.rotation.toVector().product(entity.rotationDiscRadius+1))))
//            addedEntity.linearVelocity = entity.linearVelocity.copy()
        }

        val desiredVelocity = Vector2(x, y)
        if(needsRotation){
            desiredVelocity.rotate(entityData.angle)
        }

        val thrust = desiredVelocity.product(100.0)

        emitThrustParticles(entityData, thrust)
        physicsLayer.applyForce(entityData.uuid, thrust)

        val rotate = r*5 - entityData.angularVelocity
        physicsLayer.applyTorque(entityData.uuid, rotate*10.0)
    }
}