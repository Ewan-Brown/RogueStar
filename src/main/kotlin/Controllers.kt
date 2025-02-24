import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet
import kotlin.math.abs
import WorldLayer.PhysicsBodyData

data class ControllerInput(val map: Map<Int, PhysicsBodyData>)
data class ControllerOutput(val map: Map<Int, List<ControlAction>>)

class ControllerLayer : Layer<ControllerInput, ControllerOutput>{

    fun removeController(uuid: Int){
        controllerList.removeIf {
            it.input.removeIf { it2 -> it2 == uuid } //Remove this entry from the input list
            it.input.size == 0 //If that was the last entry for this controller, remove the controller too :)
        }
    }

    companion object {
        private fun calcTorqueToTurnTo(desiredAngleVec: Vector2, angle: Double, angularVelocity: Double) : Double{
            val angleDiff = desiredAngleVec.getAngleBetween(angle)
            return (-angleDiff*8 - angularVelocity*4)
        }

        private fun calculatePositionalControl(data: WorldLayer.PhysicsBodyData, targetPos: Vector2, targetOrientation: Vector2, targetVelocity: Vector2 = Vector2()) : Pair<Vector2, Double>{
            val velocityDiff: Vector2 = data.velocity.to(targetVelocity)
            val vecToTarget = data.position.to(targetPos)

            val calcThrustToGetTo : (Vector2) -> Vector2 = fun(desiredPosition) : Vector2{
                val posDiff = desiredPosition.difference(data.position)
                return posDiff * 8.0 + velocityDiff * 4.0
            }

            val thrust : Vector2 = calcThrustToGetTo(targetPos)
            val torque : Double = calcTorqueToTurnTo(targetOrientation, data.angle, data.angularVelocity)

            return Pair(thrust, torque)
        }
    }



    abstract class BaseController<E : PhysicsBodyData>{
        abstract fun update(input: List<E>, data: Map<Int, PhysicsBodyData>) : Map<Int, List<ControlAction>>
    }

    abstract class GroupController<E : PhysicsBodyData> : BaseController<E>() {
        abstract override fun update(input : List<E>, data: Map<Int, PhysicsBodyData>) : Map<Int, List<ControlAction>>
    }

    abstract class SingleController<E : PhysicsBodyData> : BaseController<E>(){
        final override fun update(input: List<E>, data: Map<Int, PhysicsBodyData>) : Map<Int, List<ControlAction>> {
            if(input.size > 1){
                throw IllegalArgumentException("A $javaClass should only ever be attached to one entity at a time ")
            }
            val uuid = input.first().uuid
            return mapOf(Pair(uuid, update(input.first(), data)))
        }

        abstract fun update(input: E, data: Map<Int, PhysicsBodyData>) : List<ControlAction>;
    }

    class BubbleMultiController<E : PhysicsBodyData>(val targetUUID: Int , val radius: Double) : GroupController<E>(){

        val bubbleAnchorMap = mutableMapOf<Int, Vector2>()
        val inBubbleTrackerMap = mutableMapOf<Int, Boolean>()

        private fun randomizeAnchor(index: Int){
            bubbleAnchorMap[index] = Vector2(radius*0.5 + Math.random()*radius*0.2,0.0).rotate(Math.random()*2*Math.PI)
        }

        override fun update(input: List<E>, data: Map<Int, PhysicsBodyData>) : Map<Int, List<ControlAction>> {
            val controlMap = mutableMapOf<Int, List<ControlAction>>()
            var bubbleVelocity: Vector2 = Vector2()
            if(bubbleAnchorMap.isEmpty()){
                for(datum in input){
                    val index = datum.uuid
                    inBubbleTrackerMap[index] = false
                    randomizeAnchor(index)
                }
            }
            for (datum in input) {
                if(datum.position != null) {
                    val index = datum.uuid
                    val target = data[targetUUID]!!.position
                    val vecToBubbleCenter = target - datum.position

                    if (abs(vecToBubbleCenter.magnitude) < radius) {
                        inBubbleTrackerMap[index] = true
                    } else {
                        if (inBubbleTrackerMap[index]!!) {
                            inBubbleTrackerMap[index] = false
                            randomizeAnchor(index)
                        }
                    }
                    val pairResult = calculatePositionalControl(
                        datum,
                        target.sum(bubbleAnchorMap[index]),
                        Vector2(datum.angle),
                        bubbleVelocity
                    )
                    controlMap[datum.uuid] = listOf(ControlAction.ThrustAction(pairResult.first), ControlAction.TurnAction(pairResult.second))
                }
            }
            return controlMap
        }
    }

    fun <E : PhysicsBodyData> addControllerEntry(controller: GroupController<E>, group: MutableList<Int>, ){
        controllerList.add(ControllerInputEntry(controller, group))
    }

    fun <E : PhysicsBodyData> addControllerEntry(controller: SingleController<E>, entity: Int, ){
        controllerList.add(ControllerInputEntry(controller, mutableListOf(entity)))
    }

    private data class ControllerInputEntry<E : PhysicsBodyData>(val controller: BaseController<E>, val input: MutableList<Int>){
        fun update(input: List<PhysicsBodyData>, data: Map<Int, PhysicsBodyData>) : Map<Int, List<ControlAction>>{
            return controller.update(input as List<E>, data) //TODO We can do better than this
        }
    }

    private val controllerList = mutableListOf<ControllerInputEntry<*>>()

    override fun update(input: ControllerInput) : ControllerOutput {
        val entityDataMap = input.map
        val amalgamatedMap = mutableMapOf<Int, List<ControlAction>>()
        for (controllerEntityEntry in controllerList) {
            val input = controllerEntityEntry.input.mapNotNull { entityDataMap[it] } //TODO Deal with disappeared entities?
            val map = controllerEntityEntry.update(input, entityDataMap)
            amalgamatedMap.putAll(map)
        }
        return ControllerOutput(amalgamatedMap)
    }

    fun populateModelMap(modelDataMap: HashMap<Graphics.Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        //Add renderables for player perspective?? Interesting idea
    }
}

class PlayerController(val input: BitSet) : ControllerLayer.SingleController<PhysicsBodyData>(){
    override fun update(entityData: PhysicsBodyData, data: Map<Int, PhysicsBodyData>): List<ControlAction> {
        val actions = mutableListOf<ControlAction>()
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

        var direction = Vector2(x, y)

        if(input[KeyEvent.VK_Q]){
            r++
        }

        if(input[KeyEvent.VK_E]){
            r--
        }

        var needsRotation = true

        if(input[KeyEvent.VK_SPACE]){
            direction = entityData.changeInPosition.flip() * 3.0
            needsRotation = false
        }

        if(input[KeyEvent.VK_X]){
            actions.add(ControlAction.TestAction)
        }

        if(needsRotation){
            direction.rotate(entityData.angle)
        }

        val thrust = direction.product(100.0)
        val rotate = r*5 - entityData.angularVelocity


        actions.add(ControlAction.TurnAction(rotate))
        actions.add(ControlAction.ThrustAction(thrust))
        return actions
    }
}


sealed class ControlAction(){
    data class ThrustAction(val thrust: Vector2)  : ControlAction()
    data class TurnAction(val torque: Double)  : ControlAction()
    data object ShootAction : ControlAction()
    data object TestAction : ControlAction()
}