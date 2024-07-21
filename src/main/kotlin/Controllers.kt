import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet
import java.util.function.Predicate

//TODO Generalized PID utilities
class ControllerLayer : Layer{

    abstract class Controller<in E : PhysicsEntity>(){
        abstract fun update(entity : E)

        //TODO move this...
        fun emitThrustParticles(entity: E, thrust : Vector2){
            if(thrust.magnitude > 0){
                val adjustedThrust = thrust.copy().rotate((Math.random()-0.5)/3)
                effectsLayer.addEntity(TangibleEffectsEntity(entity.worldCenter.x, entity.worldCenter.y, Math.random(), listOf(
                    Component(Graphics.Model.SQUARE2, Graphics.Transform(Vector2(0.0, 0.0), 0f, 0.2f))
                ), dx = -adjustedThrust.x/5.0, dy = -adjustedThrust.y/5.0, drotation = (Math.random()-0.5)*10))
            }
        }
    }

    abstract class MultiController<in E : PhysicsEntity>(){

        abstract fun update(entities : List<E>)
    }

    class BasicMultiController<in E : PhysicsEntity>() : MultiController<E>(){

        val MIN_DIST = 7.0
        val MAX_DIST = 8.0

        override fun update(entities: List<E>) {
            val potentialTarget = physicsLayer.getEntities().firstOrNull { !entities.contains(it) }
            if(potentialTarget != null){
                //Attempt to surround the target

                val targetLoc = potentialTarget.worldCenter
                for (entity in entities) {
                    val vecToTarget = entity.worldCenter.to(targetLoc)
                    val currentDirection = entity.transform.rotation.toVector()

                    val angleDiff = vecToTarget.getAngleBetween(currentDirection)
                    val angularVelocity = entity.angularVelocity
                    val desiredVelocity = -angleDiff
                    val angularVelocityDiff = desiredVelocity - angularVelocity;
                    entity.applyTorque(-angleDiff*10 + angularVelocityDiff*10)
                    if(angleDiff < 0.01){
                        val dist = vecToTarget.normalize()
                        if(dist < MIN_DIST){
                            entity.applyForce(vecToTarget.multiply(-1.0))
                        }else if (dist > MAX_DIST){
                            entity.applyForce(vecToTarget)
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
            if(controllerEntityEntry.entities.stream().anyMatch(Predicate { t -> t.isMarkedForRemoval() })){
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

        if(input[KeyEvent.VK_SPACE]){
            x = -entity.linearVelocity.x
            y = -entity.linearVelocity.y
        }

        if(input[KeyEvent.VK_SHIFT]){
            x *= 2;
            y *= 2;
            r *= 2;
        }

        val desiredVelocity = Vector2(x, y).rotate(entity.transform.rotation);

        val thrust = desiredVelocity.product(10.0)

//        emitThrustParticles(entity, thrust)
        entity.applyForce(thrust)

        val rotate = r - entity.angularVelocity
        entity.applyTorque(rotate*10.0)
    }
}

class ChaseController : ControllerLayer.Controller<ShipEntity>(){

    private var lastTarget: PhysicsEntity? = null

    override fun update(entity: ShipEntity) {
        val currentTarget: PhysicsEntity;
        if(lastTarget == null){
            val potentialTarget = physicsLayer.getEntities().filter { it != entity }.firstOrNull()
            if(potentialTarget != null){
                currentTarget = potentialTarget
            }else{
                return
            }
        }else{
            currentTarget = lastTarget!!
        }

        val posDiff = entity.worldCenter.to(currentTarget.worldCenter)
        val thrust = posDiff.normalized
        emitThrustParticles(entity, thrust)
        entity.applyForce(thrust)
    }
}