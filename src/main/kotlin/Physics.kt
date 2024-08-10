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
import org.dyn4j.geometry.Rotation
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
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        physicsWorld.populateModelMap(modelDataMap);
    }

    fun <E : PhysicsEntity> addEntity(entity: E, angle : Double, pos : Vector2) : E{
        entity.rotate(angle)
        entity.translate(pos)
        entity.setMass(MassType.NORMAL)
//        entity.linearDamping = 0.1
//        entity.angularDamping = 0.1
        entity.recalculateComponents()
        physicsWorld.addBody(entity)
        return entity
    }

    fun <E : PhysicsEntity> addEntity(entity: E) : E{
        entity.setMass(MassType.NORMAL)
//        entity.linearDamping = 0.1
//        entity.angularDamping = 0.1
        entity.recalculateComponents()
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

private class PhysicsWorld : AbstractPhysicsWorld<PhysicsEntity, WorldCollisionData<PhysicsEntity>>() {

    override fun processCollisions(iterator: Iterator<WorldCollisionData<PhysicsEntity>>) {
        super.processCollisions(iterator)
        //Note that this list is ephemeral and should not be accessed or referenced outside this very small scope.
        contactCollisions.forEach {
            it.pair.first.body.onCollide(it)
            it.pair.second.body.onCollide(it)
        }

    }

    override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, BodyFixture>>?): WorldCollisionData<PhysicsEntity> {
        return WorldCollisionData(pair);
    }

    fun populateModelMap(map: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        for (body in this.bodies) {
            for (component in body.getComponents()) {
                map[component.model]!!.add(Pair<Transformation, GraphicalData>(component.transform, component.graphicalData))
            }
        }
    }
}

abstract class PhysicsEntity protected constructor(compDefinitions: List<PhysicalComponentDefinition>) : AbstractPhysicsBody() {
    private companion object {
        private var UUID_COUNTER = 0;
    }

    val uuid = UUID_COUNTER++;
    private val renderables: List<RenderableComponent> = compDefinitions.map {
        RenderableComponent(it.model, it.localTransform, it.graphicalData)
    }

    init {
        for (componentDefinition in compDefinitions) {
            val vertices = arrayOfNulls<Vector2>(componentDefinition.model.points)
            for (i in vertices.indices) {
                vertices[i] = componentDefinition.model.asVectorData[i].copy().multiply(componentDefinition.localTransform.scale.toDouble())
            }
            val v = Polygon(*vertices)
            v.translate(componentDefinition.localTransform.position.copy())
            v.rotate(componentDefinition.localTransform.rotation.toRadians())
            val f = BodyFixture(v)
            f.setFilter (CategoryFilter(componentDefinition.category, componentDefinition.mask))
            this.addFixture(f)
        }
    }

    //Need to update the 'center'
    fun recalculateComponents(){
        for (renderable in renderables) {
            renderable.transform.position.subtract(this.getMass().center)
        }
    }

    abstract fun onCollide(data: WorldCollisionData<PhysicsEntity>);

    abstract fun isMarkedForRemoval() : Boolean;

    abstract fun update();

    fun getComponents(): List<RenderableComponent> {
        val result: MutableList<RenderableComponent> = ArrayList()

        val entityAngle = getTransform().rotationAngle.toFloat()
        val entityPos = this.worldCenter

        for (component in renderables) {
            val newPos = component.transform.position.copy().rotate(entityAngle.toDouble()).add(entityPos)
            val newAngle = Rotation(component.transform.rotation.toRadians() + this.transform.rotationAngle)
            val scale = component.transform.scale

            result.add(RenderableComponent(component.model, Transformation(newPos, scale, newAngle), component.graphicalData));
        }

        return result
    }
}


open class DumbEntity() : PhysicsEntity(listOf(
    PhysicalComponentDefinition(
        Model.TRIANGLE,
        Transformation(Vector2(0.0, 0.0), 1.0, 0.0),
        GraphicalData(0.0f, 0.5f, 0.5f, 0.0f),
    PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits,
    PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits or PhysicsLayer.CollisionCategory.CATEGORY_PROJECTILE.bits),
    PhysicalComponentDefinition(
        Model.SQUARE1,
        Transformation(Vector2(-1.0, 0.0), 1.0, 0.0),
        GraphicalData(1.0f, 1.0f, 1.0f, 0.0f),
        PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits,
        PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits or PhysicsLayer.CollisionCategory.CATEGORY_PROJECTILE.bits)) ) {
    override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {}

    override fun isMarkedForRemoval() : Boolean = false

    override fun update() {}
}

//TODO use the physics engine to create ephemeral colliding/form-changing entities that represent explosions' force etc.
//  Maybe could collect these explosions and pass them to shader to do cool stuff...
// TODO Does this need a controller attached to it, if it is homing it _definitely_ should
class ProjectileEntity() : PhysicsEntity(listOf(
    PhysicalComponentDefinition(
        Model.TRIANGLE,
        Transformation(Vector2(0.0, 0.0), 0.2, 0.0),
        GraphicalData(1.0f, 0.0f, 0.0f, 0.0f),
        PhysicsLayer.CollisionCategory.CATEGORY_PROJECTILE.bits,
        PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits)) ) {
     var hasCollided = false

    init {
        this.linearVelocity.add(Vector2(this.transform.rotationAngle).multiply(50.0))
    }

    override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
        data.body2.applyImpulse(Vector2(this.transform.rotationAngle).product(2.0))
        data.body2.applyTorque((Math.random()-0.5) * 1000)
        hasCollided = true
    }

    override fun isMarkedForRemoval() : Boolean = hasCollided

    override fun update() {
        this.applyForce(Vector2(transform.rotationAngle).product(2.0))
    }
}