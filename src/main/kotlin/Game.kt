import Graphics.DrawableThing
import Graphics.Model
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData


fun main() {
    val physicsWorld = PhysicsWorld();
    val effectsWorld = EffectsWorld();
    val gui = Graphics();
    val testEntity = SimpleEffectsEntity(Vector2(0.0,0.0), model = Model.TRIANGLE)
    val drawableThings = listOf(testEntity);
    gui.updateDrawables(drawableThings)
    gui.setup()
    while(true){
        Thread.sleep(15)
        testEntity.position = testEntity.position.add(0.01,0.0)
    }
}

class PhysicsEntity : AbstractPhysicsBody() {

}

class PhysicsWorld : AbstractPhysicsWorld<PhysicsEntity, WorldCollisionData<PhysicsEntity>>(){
    override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, BodyFixture>>?): WorldCollisionData<PhysicsEntity>? {
        return null;
    }
}

abstract class EffectsEntity : DrawableThing{}

class SimpleEffectsEntity(
    var position: Vector2,
    var velocity: Vector2 = Vector2(0.0, 0.0),
    var angle: Float = 0.0f,
    var angularVelocity: Float = 0.0f,
    val model: Model,
) : EffectsEntity(){

    override fun getTransformedComponents(): MutableList<Pair<Graphics.Model, Graphics.Transform>> {
        return mutableListOf(Pair(model, Graphics.Transform(position, angle)))
    }

    override fun getRequiredModels(): MutableList<Graphics.Model> {
        return mutableListOf(model)
    }

}

class EffectsWorld{
    val entities = listOf<EffectsEntity>();

}