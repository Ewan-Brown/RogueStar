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

class PhysicsLayer : Layer{
    private val physicsWorld = PhysicsWorld();
    init {
        physicsWorld.setGravity(0.0,0.0)

        val testPhysicsEntity1 = PhysicsEntity(listOf(Component(Model.SQUARE1, Graphics.Transform(Vector2(0.0,0.0), 0.0f))))
        testPhysicsEntity1.setMass(MassType.NORMAL)
        physicsWorld.addBody(testPhysicsEntity1)

        val testPhysicsEntity2 = PhysicsEntity(listOf(Component(Model.SQUARE2, Graphics.Transform(Vector2(0.0,0.0), 0.0f))))
        testPhysicsEntity2.setMass(MassType.NORMAL)
        physicsWorld.addBody(testPhysicsEntity2)

        testPhysicsEntity2.applyForce(Vector2(0.0,1.0))
        val sum = fun Int.(other: Int): Int = this + other

    }

    override fun update() {
        physicsWorld.update(1.0)
    }

    //TODO Can we delegate this or something
    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>) {
        physicsWorld.populateModelMap(modelDataMap);
    }

}

private class PhysicsWorld : AbstractPhysicsWorld<PhysicsEntity, WorldCollisionData<PhysicsEntity>>(){
    override fun processCollisions(iterator : Iterator<WorldCollisionData<PhysicsEntity>>) {
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

    fun createAndAddEntity(components : List<Component> ){
        val entity = PhysicsEntity(components)

    }
}

private class PhysicsEntity internal constructor(private val components: List<Component>) : AbstractPhysicsBody() {

    init {
        for (component in components) {
            val vertices = arrayOfNulls<Vector2>(component.model.points)
            for (i in vertices.indices) {
                vertices[i] = component.model.asVectorData[i].copy()
            }
            val v = Polygon(*vertices)
            v.translate(component.transform.position.copy())
            v.rotate(component.transform.angle.toDouble())
            val f = BodyFixture(v)
            this.addFixture(f)
        }
    }

    fun getComponents(): List<Component> {
        val result: MutableList<Component> = ArrayList()

        val entityAngle = getTransform().rotationAngle.toFloat()
        val entityPos = this.worldCenter

        for (component in components) {
            val newPos = component.transform.position.copy().rotate(entityAngle.toDouble()).add(entityPos)
            val newAngle = entityAngle + component.transform.angle
            result.add(Component(component.model,Graphics.Transform(newPos, newAngle)))
        }

        return result
    }
}