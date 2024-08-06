import EffectsUtils.Companion.emitThrustParticles
import org.dyn4j.geometry.Vector2
import java.awt.event.KeyEvent
import java.util.BitSet
import kotlin.math.abs

//TODO Generalized PID utilities
class ControllerLayer : Layer{

    abstract class Controller<in E : PhysicsEntity> {
        abstract fun update(entity : E)
    }

    abstract class MultiController<in E : PhysicsEntity> {
        abstract fun update(entities : List<E>)

    }

    class EncircleMultiController<in E : PhysicsEntity>() : MultiController<E>(){

        //TODO This should be immutable
        private var angleMap: MutableMap<Int, Double>? = null

        override fun update(entities: List<E>) {
            val target = physicsLayer.getEntities().firstOrNull { it.first == 0 }
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
                    for(e in entities){
                        val angle = angleMap!![e.uuid] ?: throw NullPointerException("angle")
                        val targetPos: Vector2 = Vector2(target.second.position).add(Vector2(angle).product(10.0))
                        val vecToTarget: Vector2 = e.worldCenter.to(targetPos)
                        val vecToTargetHost: Vector2 = e.worldCenter.to(target.second.position)

                        val velocityVector = e.linearVelocity;
                        val angleVector = Vector2(e.transform.rotationAngle)

                        val FORWARD_ANGLE_THRESHOLD = Math.PI/8;


                        val orientationToTargetDiff = angleVector.getAngleBetween(vecToTarget)


                        val calcTorqueToTurnTo : (Vector2) -> Double = fun(desiredAngleVec) : Double{
                            val angularVelocity = e.angularVelocity
                            val angleDiff = desiredAngleVec.getAngleBetween(e.transform.rotationAngle)
                            val desiredVelocity = -angleDiff
                            val velocityOffset = desiredVelocity - angularVelocity;
                            return -angleDiff + velocityOffset * 2
                        }

                        val thrust : Vector2
                        var torque : Double
                        if(vecToTarget.magnitude < 1){
                            thrust = e.linearVelocity.product(-5.0)
                            torque = calcTorqueToTurnTo(vecToTargetHost)*2
                        }else {

                            if (abs(orientationToTargetDiff) > FORWARD_ANGLE_THRESHOLD) {
                                torque = calcTorqueToTurnTo(vecToTarget)*2
                                thrust = vecToTarget.normalized.product(2.5)

                            } else {
                                torque = calcTorqueToTurnTo(vecToTarget)
                                thrust = vecToTarget.normalized.product(5.0)
                            }
                        }
                        e.applyTorque(torque)
                        e.applyForce(thrust)
                        emitThrustParticles(e, thrust)

                    }
                }
            }

        }
    }

    class ChaseMultiController<in E : PhysicsEntity>() : MultiController<E>(){

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

    internal fun addEntityRequest(entity : PhysicsEntity){
        entityRequestBuffer.add(entity)
    }

    fun populateModelMap(modelDataMap: HashMap<Graphics.Model, MutableList<Graphics.Transform>>) {
        //Add renderables for player perspective?? Interesting idea
    }
}

class PlayerController(val input: BitSet) : ControllerLayer.Controller<DumbEntity>(){
    override fun update(entity: DumbEntity) {
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
            val newEntity = ProjectileEntity();
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

class ChaseController : ControllerLayer.Controller<DumbEntity>(){

    private var lastTarget: Int? = null

    override fun update(entity: DumbEntity) {
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
            val posDiff = entity.worldCenter.to(bodyData?.position)
            val thrust = posDiff.normalized
            emitThrustParticles(entity, thrust)
            entity.applyForce(thrust)
        }
    }
}