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
import org.dyn4j.collision.CategoryFilter
import org.dyn4j.dynamics.PhysicsBody
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class PhysicsLayer : Layer{
    enum class CollisionCategory(val bits: Long){
        CATEGORY_SHIP(      0b0001),
        CATEGORY_PROJECTILE(0b0010),
        CATEGORY_SHIELD(    0b0100)
    }
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

    //onCollide() is called during physicsWorld.update(), meaning it always occurs before any body.update() is called.
    //therefore onCollide should simply flag things and store data, allowing .update() to clean up.
    override fun update() {
        time++;
        physicsWorld.update(1.0)
        var i = physicsWorld.bodies.size;
        while(i > 0){
            i--;
            val body = physicsWorld.bodies[i]
            body.update()
            if(body.isMarkedForRemoval()){
                physicsWorld.removeBody(body)
                i--;
            }
        }
    }

    //TODO Can we delegate this or something
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>) {
        physicsWorld.populateModelMap(modelDataMap);
    }

    fun <E : PhysicsEntity> addEntity(entity: E, angle : Double, pos : Vector2) : E{
        entity.rotate(angle)
        entity.translate(pos)
        entity.setMass(MassType.NORMAL)
        entity.linearDamping = 0.1
        entity.angularDamping = 0.1
        entity.updateComponents()
        physicsWorld.addBody(entity)
        return entity
    }

    fun <E : PhysicsEntity> addEntity(entity: E) : E{
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
        super.processCollisions(iterator);
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
abstract class PhysicsEntity protected constructor(componentsAndCategories: List<Pair<Component, PhysicsLayer.CollisionCategory>>, val mask : Long) : AbstractPhysicsBody() {
    private companion object {
        private var UUID_COUNTER = 0;
    }
    constructor(comps: List<Component>, category: PhysicsLayer.CollisionCategory, mask: Long) : this(comps.map { Pair(it, category)}, mask) {
    }

    val uuid = UUID_COUNTER++;
    private val components: List<Component> = componentsAndCategories.map { it->it.first }

    init {
        for ((component, category) in componentsAndCategories) {
            val vertices = arrayOfNulls<Vector2>(component.model.points)
            for (i in vertices.indices) {
                vertices[i] = component.model.asVectorData[i].copy().multiply(component.transform.scale.toDouble())
            }
            val v = Polygon(*vertices)
            v.translate(component.transform.position.copy())
            v.rotate(component.transform.angle.toDouble())
            val f = BodyFixture(v)
            f.setFilter (CategoryFilter(category.bits, mask))
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

            result.add(Component(component.model,Transform(newPos, newAngle, component.transform.scale, component.transform.red, component.transform.green, component.transform.blue, component.transform.alpha)))
        }

        return result
    }
}


open class DumbEntity() : PhysicsEntity(listOf(
    Component(Model.TRIANGLE, Transform(Vector2(0.0, 0.0), 0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f))
), PhysicsLayer.CollisionCategory.CATEGORY_SHIP, PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits or PhysicsLayer.CollisionCategory.CATEGORY_PROJECTILE.bits) {
    override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {}

    override fun isMarkedForRemoval() : Boolean = false

    override fun update() {}
}

 class ProjectileEntity() : PhysicsEntity(listOf(
    Component(Model.TRIANGLE,
        Transform(Vector2(0.0, 0.0), 0f, 0.3f, 0.0f, 1.0f, 0.0f, 1.0f))),
     PhysicsLayer.CollisionCategory.CATEGORY_PROJECTILE,
     PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits) {
     var hasCollided = false

    override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
        hasCollided = true
    }

    override fun isMarkedForRemoval() : Boolean = hasCollided

    override fun update() {}
}