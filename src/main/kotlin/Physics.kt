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
    }

    override fun update() {
        physicsWorld.update(1.0)
    }

    //TODO Can we delegate this or something
    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>) {
        physicsWorld.populateModelMap(modelDataMap);
    }

    fun addEntity(components: List<Component>, angle : Double, pos : Vector2) : AbstractPhysicsBody{
        val newEntity = PhysicsEntity(components)
        newEntity.translateToOrigin()
        newEntity.translate(pos)
        newEntity.rotate(angle)
        newEntity.setMass(MassType.NORMAL)
        physicsWorld.addBody(newEntity)
        return newEntity
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

private class PhysicsEntity (private val components: List<Component>) : AbstractPhysicsBody() {

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