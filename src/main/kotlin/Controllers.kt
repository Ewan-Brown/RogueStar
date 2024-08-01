import EffectsUtils.Companion.emitThrustParticles
import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet
import java.util.function.Predicate

//TODO Generalized PID utilities
class ControllerLayer : Layer{

    abstract class Controller<in E : PhysicsEntity> {
        abstract fun update(entity : E)
    }

    abstract class MultiController<in E : PhysicsEntity> {

        abstract fun update(entities : List<E>)

        //TODO move this...

    }

    class BasicMultiController<in E : PhysicsEntity>() : MultiController<E>(){

        val MIN_DIST = 7.0
        val MAX_DIST = 8.0

        override fun update(entities: List<E>) {
            val result = physicsLayer.getEntities().firstOrNull { it.first == 0 }
            if(result != null){

                val uuid = result.first;
                val data = result.second;

                //Attempt to surround the target
                val targetLoc = data.position
                for (entity in entities) {
                    val vecToTarget = entity.worldCenter.to(targetLoc)
                    val currentDirection = entity.transform.rotation.toVector()

                    val angleDiff = vecToTarget.getAngleBetween(currentDirection)
                    val angularVelocity = entity.angularVelocity
                    val desiredVelocity = -angleDiff
                    val angularVelocityDiff = desiredVelocity - angularVelocity;
                    entity.applyTorque(-angleDiff*20 + angularVelocityDiff*10)
                    if(angleDiff < 0.01){
                        val dist = vecToTarget.normalize()
                        if(dist < MIN_DIST){
                            val thrust = vecToTarget.multiply(-20.0)
                            emitThrustParticles(entity, thrust)
                            entity.applyForce(thrust)
                        }else if (dist > MAX_DIST){
                            val thrust = vecToTarget.multiply(10.0)
                            emitThrustParticles(entity, thrust)
                            entity.applyForce(thrust)
                        }else{

//                            entity.applyForce(entity.linearVelocity.multiply(-0.3))
                            val orientationVector = entity.transform.rotation.toVector()
                            val velocity = entity.linearVelocity
                            val dotProduct = velocity.dot(orientationVector)

                            val sideslip = velocity.difference(orientationVector.product(dotProduct))
                            sideslip.normalize()
//                            entity.applyForce(sideslip)
                        }
                    }
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
    }

    fun getNewEntityRequests() : List<PhysicsEntity>{
        return entityRequestBuffer
    }

    fun populateModelMap(modelDataMap: HashMap<Graphics.Model, MutableList<Graphics.Transform>>) {
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
            x = -entity.changeInPosition.x
            y = -entity.changeInPosition.y
            needsRotation = false;
        }

        if(input[KeyEvent.VK_SHIFT]){
            x *= 2;
            y *= 2;
            r *= 2;
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
            val potentialTarget = physicsLayer.getEntities().filter { it.first != entity.uuid}.firstOrNull()
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
            val posDiff = entity.worldCenter.to(bodyData?.worldCenter)
            val thrust = posDiff.normalized
            emitThrustParticles(entity, thrust)
            entity.applyForce(thrust)
        }
    }
}