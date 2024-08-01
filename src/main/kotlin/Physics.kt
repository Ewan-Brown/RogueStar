import Graphics.Transform
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData
import Graphics.Model
import org.dyn4j.dynamics.PhysicsBody
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PhysicsLayer : Layer{
    private val physicsWorld = PhysicsWorld();
    private var time = 0.0;
    init {
        physicsWorld.setGravity(0.0,0.0)
    }

    fun getEntities() : List<Pair<Int, PhysicsBodyData>>{
        return physicsWorld.bodies.map { it -> it.uuid to PhysicsBodyData(it) }
    }

    //TODO benchmark and see if a cachemap ;) is necessary
    fun getEntity(uuid : Int) : PhysicsBodyData? {
        val body = physicsWorld.bodies.firstOrNull { it.uuid == uuid }
        return body?.let { PhysicsBodyData(it) }
    }

    override fun update() {
        time++;
        physicsWorld.update(1.0)
    }

    //TODO Can we delegate this or something
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>) {
        physicsWorld.populateModelMap(modelDataMap);
    }

    fun <E : PhysicsEntity> addEntity(entity: E, angle : Double, pos : Vector2) : E{
        entity.translateToOrigin()
        entity.translate(pos)
        entity.rotate(angle)
        entity.setMass(MassType.NORMAL)
        entity.linearDamping = 0.1
        entity.angularDamping = 0.1
        entity.updateComponents()
        physicsWorld.addBody(entity)
        return entity
    }

    fun removeEntity(entity: PhysicsEntity){
        physicsWorld.removeBody(entity)
    }

}

data class PhysicsBodyData(val position: Vector2?, val velocity: Vector2?, val angle: Double, val traceRadius: Double){
    constructor(body: PhysicsBody) : this(body.worldCenter, body.linearVelocity, body.transform.rotationAngle, body.rotationDiscRadius);
}

private class PhysicsWorld : AbstractPhysicsWorld<PhysicsEntity, WorldCollisionData<PhysicsEntity>>(){

    override fun processCollisions(iterator : Iterator<WorldCollisionData<PhysicsEntity>>) {
        iterator.forEach {
            it.pair.first.body.onCollide(it)
            it.pair.second.body.onCollide(it)
        }
        //TODO Do something with collisions, if we'd like
    }

    override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, BodyFixture>>?): WorldCollisionData<PhysicsEntity> {
        return WorldCollisionData(pair);
    }

    fun populateModelMap(map : HashMap<Model, MutableList<Transform>>){
        for (body in this.bodies) {
            for (component in body.getComponents()) {
                map[component.model]!!.add(component.transform)
            }
        }
    }

//    fun getPhysicsBodies() : List<PhysicsBodyData>{
//        return getBodies()
//    }
}

/**
 * Represents the physical thing, which may be swapped among controllers freely!
 */
abstract class PhysicsEntity (private val components: List<Component>) : AbstractPhysicsBody() {
    private companion object {
        private var UUID_COUNTER = 0;
    }
    val uuid = UUID_COUNTER++;
    init {
        for (component in components) {
            val vertices = arrayOfNulls<Vector2>(component.model.points)
            for (i in vertices.indices) {
                vertices[i] = component.model.asVectorData[i].copy().multiply(component.transform.scale.toDouble())
            }
            val v = Polygon(*vertices)
            v.translate(component.transform.position.copy())
            v.rotate(component.transform.angle.toDouble())
            val f = BodyFixture(v)
            this.addFixture(f)
        }
    }

    //Need to update the 'center'
    fun updateComponents(){
        for (component in components) {
            component.transform.position.subtract(this.getMass().center)
        }
    }

    abstract fun onCollide(data: WorldCollisionData<PhysicsEntity>);

    abstract fun isMarkedForRemoval() : Boolean;

    abstract fun update();

    fun getComponents(): List<Component> {
        val result: MutableList<Component> = ArrayList()

        val entityAngle = getTransform().rotationAngle.toFloat()
        val entityPos = this.worldCenter

        for (component in components) {
            val newPos = component.transform.position.copy().rotate(entityAngle.toDouble()).add(entityPos)
            val newAngle = entityAngle + component.transform.angle
            result.add(Component(component.model,Graphics.Transform(newPos, newAngle, component.transform.scale)))
        }

        return result
    }
}