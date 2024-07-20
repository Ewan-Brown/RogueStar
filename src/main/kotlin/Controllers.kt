import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet

//For AI and player controls!
class ControllerLayer : Layer{

    abstract class Controller<in E : PhysicsEntity>(){
        abstract fun update(entity : E)
        fun emitThrustParticles(entity: E, thrust : Vector2){
            if(thrust.magnitude > 0){

                val adjustedThrust = thrust.copy().rotate((Math.random()-0.5)/3)
                effectsLayer.addEntity(TangibleEffectsEntity(entity.worldCenter.x, entity.worldCenter.y, Math.random(), listOf(
                    Component(Graphics.Model.SQUARE2, Graphics.Transform(Vector2(0.0, 0.0), 0f, 0.2f))
                ), dx = -adjustedThrust.x/5.0, dy = -adjustedThrust.y/5.0, drotation = (Math.random()-0.5)*10))
            }
        }
    }

    fun <E : PhysicsEntity> addControlledEntity(entity: E, controller: Controller<E>){
        val entry = ControllerEntityEntry(controller, entity)
        controllerList.add(entry)
    }

    private data class ControllerEntityEntry<E : PhysicsEntity>(val controller: Controller<E>,  var entity: E){
        fun update() {
            controller.update(entity)
        }
    }
    private val controllerList = mutableListOf<ControllerEntityEntry<*>>()
    private val entityRequestBuffer = mutableListOf<PhysicsEntity>()

    override fun update() {
        entityRequestBuffer.clear()
        controllerList.removeIf { t -> t.entity.isMarkedForRemoval() }
        for (controllerEntityEntry in controllerList) {
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
            y++;
        }
        if(input[KeyEvent.VK_S]){
            y--;
        }
        if(input[KeyEvent.VK_D]){
            x++;
        }
        if(input[KeyEvent.VK_A]){
            x--;
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

        val thrust = Vector2(x, y);
        if(!input[KeyEvent.VK_SHIFT]){
            thrust.rotate(entity.transform.rotation)
        }
        val rotate = r - entity.angularVelocity
        emitThrustParticles(entity, thrust)
        entity.applyForce(thrust)
        entity.applyTorque(rotate)

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