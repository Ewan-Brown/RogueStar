import Graphics.Model
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.World
import org.dyn4j.world.WorldCollisionData


/**
 * Test
 */
fun main() {
    val physicsWorld = PhysicsWorld();
    val effectsWorld = EffectsWorld();
    val models = listOf(Model.TRIANGLE, Model.SQUARE1)
    val gui = Graphics(models)

    val testEntity = SimpleEffectsEntity(Vector2(0.0,0.0), model = Model.TRIANGLE)

    physicsWorld.setGravity(0.0,0.0)

    val testPhysicsEntity1 = PhysicsEntity(listOf(Component(Model.SQUARE1, Graphics.Transform(Vector2(0.0,0.0), 0.0f))))
    testPhysicsEntity1.setMass(MassType.NORMAL)
    physicsWorld.addBody(testPhysicsEntity1)

    val testPhysicsEntity2 = PhysicsEntity(listOf(Component(Model.SQUARE1, Graphics.Transform(Vector2(0.0,0.0), 0.0f))))
    testPhysicsEntity2.setMass(MassType.NORMAL)
    physicsWorld.addBody(testPhysicsEntity2)


    gui.setup()
    while(true){
        Thread.sleep(15)

        testEntity.position = testEntity.position.add(0.01,0.0)
        testEntity.angle += 0.01f;
        testPhysicsEntity1.applyForce(Vector2(1.0,1.0))

        val modelDataMap = hashMapOf<Model, MutableList<Graphics.Transform>>()
        for (model in models) {
            modelDataMap[model] = mutableListOf()
        }
        var drawableThings : List<Component> = testEntity.getDrawableInstances() + testPhysicsEntity1.getDrawableInstances() + testPhysicsEntity2.getDrawableInstances()
        for (drawable in drawableThings) {
            modelDataMap[drawable.model]!!.add(drawable.transform);
        }
        gui.updateDrawables(modelDataMap)
        physicsWorld.update(1.0)
    }
}

private interface DrawableProvider{
    fun getDrawableInstances() : List<Component>
}

private data class Component(val model: Model, val transform: Graphics.Transform)

private class PhysicsEntity : AbstractPhysicsBody, DrawableProvider {

    val components: List<Component>

    constructor(components: List<Component>) : super() {
        this.components = components

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

    override fun getDrawableInstances(): List<Component> {
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

private class PhysicsWorld : AbstractPhysicsWorld<PhysicsEntity, WorldCollisionData<PhysicsEntity>>(){
    override fun processCollisions(iterator : Iterator<WorldCollisionData<PhysicsEntity>>) {
        //TODO Do something with collisions, if we'd like
    }
    override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, BodyFixture>>?): WorldCollisionData<PhysicsEntity>? {
        return WorldCollisionData(pair);
    }
}

private abstract class EffectsEntity : DrawableProvider{}

private class SimpleEffectsEntity(
    var position: Vector2,
    var velocity: Vector2 = Vector2(0.0, 0.0),
    var angle: Float = 0.0f,
    var angularVelocity: Float = 0.0f,
    val model: Model,
) : EffectsEntity(){
    override fun getDrawableInstances(): List<Component> {
        return return mutableListOf(Component(model, Graphics.Transform(position.copy(), angle)))
    }

}

private class EffectsWorld{
    val entities = listOf<EffectsEntity>();
}