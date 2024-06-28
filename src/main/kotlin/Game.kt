import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.World
import org.dyn4j.world.WorldCollisionData

class GameWorld{
    val physicsWorld = PhysicsWorld();
    val effectsWorld = EffectsWorld();
}

class PhysicsEntity : AbstractPhysicsBody() {

}

class PhysicsWorld : AbstractPhysicsWorld<PhysicsEntity, WorldCollisionData<PhysicsEntity>>(){
    override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, BodyFixture>>?): WorldCollisionData<PhysicsEntity>? {
        return null;
    }
}

class EffectsEntity{

}

class EffectsWorld{

}