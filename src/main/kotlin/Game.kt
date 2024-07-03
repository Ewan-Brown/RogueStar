import Graphics.Model
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.AbstractPhysicsWorld
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
    val testPhysicsEntity = PhysicsEntity()
    testPhysicsEntity.setModels(listOf(Pair(Model.SQUARE1, Graphics.Transform(Vector2(0.0,0.0), 0.0f))))

    gui.setup()
    while(true){
        Thread.sleep(15)
        testEntity.position = testEntity.position.add(0.01,0.0)
        val modelDataMap = hashMapOf<Model, MutableList<Graphics.Transform>>()
        for (model in models) {
            modelDataMap[model] = mutableListOf()
        }
        var drawableThings : List<DrawableInstance> = testEntity.getDrawableInstances() + testPhysicsEntity.getDrawableInstances()
        for (drawable in drawableThings) {
            modelDataMap[drawable.model]!!.add(drawable.transform);
        }
        gui.updateDrawables(modelDataMap)
    }
}

private interface DrawableProvider{
    fun getDrawableInstances() : List<DrawableInstance>
}

private data class DrawableInstance(val model: Model, val transform: Graphics.Transform)

private class PhysicsEntity : AbstractPhysicsBody(), DrawableProvider {
    private var models: List<Pair<Model, Graphics.Transform>>? = null
    private val requiredModels : MutableList<Model> = mutableListOf()

    override fun getDrawableInstances(): List<DrawableInstance> {
        val result: MutableList<DrawableInstance> = ArrayList()

        val entityAngle = getTransform().rotationAngle.toFloat()
        val entityPos = this.worldCenter

        for (component in models!!) {
            val newPos = component.component2().position.copy().rotate(entityAngle.toDouble()).add(entityPos)
            val newAngle = entityAngle + component.component2().angle
            result.add(DrawableInstance(component.component1(),Graphics.Transform(newPos, newAngle)))
        }

        return result
    }

    //Why isn't this just part of the constructor
    fun setModels(models: List<Pair<Model,Graphics.Transform>>) {
        for (model in models) {
            model.second.position.subtract(getMass().center)
            if(!requiredModels.contains(model.first)){
                requiredModels.add(model.first)
            }
        }
        this.models = models
    }


}

private class PhysicsWorld : AbstractPhysicsWorld<PhysicsEntity, WorldCollisionData<PhysicsEntity>>(){
    override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, BodyFixture>>?): WorldCollisionData<PhysicsEntity>? {
        return null;
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
    override fun getDrawableInstances(): List<DrawableInstance> {
        return return mutableListOf(DrawableInstance(model, Graphics.Transform(position.copy(), angle)))
    }

}

private class EffectsWorld{
    val entities = listOf<EffectsEntity>();
}