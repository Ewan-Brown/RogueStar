import BasicFixtureSlot.*
import CockpitFixtureSlot.*
import Graphics.*
import GunFixtureSlot.*
import RifleFixtureSlot.*
import RifleFixtureSlot.RifleFixture
import ThrusterFixtureSlot.*
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Convex
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.WorldCollisionData

/************************
 * Fixture related code
 *************************/

/**
 * Needed a custom extension of this to easily react for fixture<->fixture collision events. Dyn4J is focused on Body<->Body collisions - this just makes life easier for me.
 */

/**
 * A slot for a fixture of a given type. Think carefully about what fields should go in slot vs fixture.
 *
 * At the moment (7/13/2025) - I think there needs to be 1-to-1 SLOT - FIXTURE class relationships
 */
sealed class AbstractFixtureSlot<T : BasicFixture>(val model: Model, val localTransform: Transformation, val collisionCategory: CollisionCategory, val collisionMask: Long, val fixtureProducer: (Polygon) -> T) {

    fun generateFixture(teamProducer: () -> Team?): T{
        val vertices = arrayOfNulls<Vector2>(model.points)
        for (i in vertices.indices) {
            vertices[i] = model.asVectorData[i].copy()
                .multiply(localTransform.scale)
        }
        val polygon = Polygon(*vertices)
        polygon.rotate(localTransform.rotation.toRadians())
        polygon.translate(Vector2(localTransform.translation.x, localTransform.translation.y)) //Dyn4J is 2D :P
        val fixture = fixtureProducer(polygon)
        fixture.filter = TeamFilter(teamProducer,
            category = collisionCategory.bits,
            mask = collisionMask
        )
        return fixture
    }
    fun onDestruction(){}
}

fun <F : AbstractFixtureSlot<*>> copyFixtureSlot(a: F) : F {
    return when(a){
        is BasicFixtureSlot -> BasicFixtureSlot(a.model, a.localTransform, a.collisionCategory, a.collisionMask) as F
        is ThrusterFixtureSlot -> ThrusterFixtureSlot(a.model, a.localTransform) as F
        is CockpitFixtureSlot -> CockpitFixtureSlot(a.model, a.localTransform) as F
        is RifleFixtureSlot -> RifleFixtureSlot(a.model, a.localTransform, a.projectileCreator) as F
        else -> {throw IllegalArgumentException()}
    }
}

class BasicFixtureSlot(model: Model, localTransform: Transformation, collisionCategory: CollisionCategory, collisionMask: Long)
    : AbstractFixtureSlot<BasicFixture>(model, localTransform, collisionCategory, collisionMask, { p -> BasicFixture(p)}) {
    open class BasicFixture(shape: Convex): BodyFixture(shape) {
        private var health = 100;
        fun onCollide(data: WorldCollisionData<BasicFixture, AbstractPhysicsEntity>) {
            health -= 1;
        }

        fun getHealth(): Int = health
        fun isMarkedForRemoval(): Boolean = health <= 0

        /**
         * Just for testing at the moment
         */
        fun kill(){health = 0}
    }
}

class ThrusterFixtureSlot(model: Model, transform : Transformation)
    : AbstractFixtureSlot<ThrusterFixture>(model, transform, CollisionCategory.CATEGORY_SHIP, CollisionCategory.CATEGORY_SHIP.bits, { p -> ThrusterFixture(p)}) {
    class ThrusterFixture(shape: Convex): BasicFixture(shape) {
        private var orientation = 0.0
    }
}

abstract class GunFixtureSlot<T : GunFixture>(model: Model, transform : Transformation,
                     val projectileCreator: (T) -> AbstractPhysicsEntity, producer: (Polygon) -> GunFixture)
    : AbstractFixtureSlot<GunFixture>(model, transform, CollisionCategory.CATEGORY_SHIP, CollisionCategory.CATEGORY_SHIP.bits, producer) {

    abstract class GunFixture(shape: Convex): BasicFixture(shape)

    abstract fun generateProjectile(t: T): Pair<AbstractPhysicsEntity, Transformation>

}

class RifleFixtureSlot(model: Model, transform : Transformation, projectileCreator: (RifleFixture) -> AbstractPhysicsEntity)
    : GunFixtureSlot<RifleFixture>(model, transform, projectileCreator, {p -> RifleFixture(p)}) {

    //Assumes everything is oriented correctly..
    val bulletSpawnOffset: Vector2 = Vector2(0.0, 0.0)

    class RifleFixture(shape: Convex): GunFixture(shape){
        val rotation : Double = 0.0
    }


    // The projectile's desired transformation relative to the fixture's coordinate system
    fun getProjectileTransformation(t: RifleFixture): Transformation {
        return Transformation(bulletSpawnOffset.toVec3(), 1.0, t.rotation)
    }

    override fun generateProjectile(t: RifleFixture): Pair<AbstractPhysicsEntity, Transformation> {
        val projectile = projectileCreator(t);
        val transform = getProjectileTransformation(t);
        return Pair(projectile, transform)
    }

}

class CockpitFixtureSlot(model: Model, transform : Transformation) : AbstractFixtureSlot<CockpitFixture>(model, transform, CollisionCategory.CATEGORY_SHIP, CollisionCategory.CATEGORY_SHIP.bits, { p -> CockpitFixture(p)}) {
    class CockpitFixture(shape: Convex): BasicFixture(shape) {}
}