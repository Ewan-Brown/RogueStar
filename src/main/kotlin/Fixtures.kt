import Graphics.*
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Convex
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.WorldCollisionData

/************************
 * Fixture related code
 *************************/

/**
 * Needed a custom extension of this to easily react for fixture<->fixture collision events. Dyn4J is focused on Body<->Body collisions - this just makes life easier for me.
 */
open class BasicFixture(shape: Convex): BodyFixture(shape) {
    private var health = 100;
    fun onCollide(data: WorldCollisionData<BasicFixture, PhysicsEntity>) {
        health -= 1;
    }

    fun getHealth(): Int = health
    fun isMarkedForRemoval(): Boolean = health <= 0

    /**
     * Just for testing at the moment
     */
    fun kill(){health = 0}
}

class ThrusterFixture(shape: Convex): BasicFixture(shape) {
    private var direction = 0.0
}

class GunFixture(shape: Convex): BasicFixture(shape) {
}

class CockpitFixture(shape: Convex): BasicFixture(shape) {
}

/**
 * A slot for a fixture of a given type. Think carefully about what fields should go in slot vs fixture.
 */
sealed class FixtureSlot<T : BasicFixture>(val model: Model, val localTransform: Transformation){
    abstract fun createFixture(): T
    fun onDestruction(){}
}

fun <F : FixtureSlot<*>> copyFixtureSlot(a: F) : F {
    return when(a){
        is BasicFixtureSlot -> BasicFixtureSlot(a.model, a.localTransform) as F
        is ThrusterFixtureSlot -> ThrusterFixtureSlot(a.model, a.localTransform) as F
        is CockpitFixtureSlot -> CockpitFixtureSlot(a.model, a.localTransform) as F
        is GunFixtureSlot -> GunFixtureSlot(a.model, a.localTransform) as F
        else -> {throw IllegalArgumentException()}
    }
}

class BasicFixtureSlot(model: Model, localTransform: Transformation) : FixtureSlot<BasicFixture>(model, localTransform){
    override fun createFixture(): BasicFixture {
        val vertices = arrayOfNulls<Vector2>(model.points)
        for (i in vertices.indices) {
            vertices[i] = model.asVectorData[i].copy()
                .multiply(localTransform.scale)
        }
        val polygon = Polygon(*vertices)
        polygon.rotate(localTransform.rotation.toRadians())
        polygon.translate(Vector2(localTransform.translation.x, localTransform.translation.y)) //Dyn4J is 2D :P
        val fixture = BasicFixture(polygon)
        fixture.filter = TeamFilter(
            category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits
        )
        return fixture
    }

}

class ThrusterFixtureSlot(model: Model, transform : Transformation) : FixtureSlot<ThrusterFixture>(model, transform) {
    override fun createFixture(): ThrusterFixture {
        val vertices = arrayOfNulls<Vector2>(model.points)
        for (i in vertices.indices) {
            vertices[i] = model.asVectorData[i].copy()
                .multiply(localTransform.scale)
        }
        val polygon = Polygon(*vertices)
        polygon.rotate(localTransform.rotation.toRadians())
        polygon.translate(Vector2(localTransform.translation.x, localTransform.translation.y)) //Dyn4J is 2D :P
        val fixture = ThrusterFixture(polygon)
        fixture.filter = TeamFilter(
            category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits
        )
        return fixture
    }
}

class GunFixtureSlot(model: Model, transform : Transformation) : FixtureSlot<GunFixture>(model, transform) {
    override fun createFixture(): GunFixture {
        val vertices = arrayOfNulls<Vector2>(model.points)
        for (i in vertices.indices) {
            vertices[i] = model.asVectorData[i].copy()
                .multiply(localTransform.scale)
        }
        val polygon = Polygon(*vertices)
        polygon.rotate(localTransform.rotation.toRadians())
        polygon.translate(Vector2(localTransform.translation.x, localTransform.translation.y)) //Dyn4J is 2D :P
        val fixture = GunFixture(polygon)
        fixture.filter = TeamFilter(
            category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits
        )
        return fixture
    }
}


class CockpitFixtureSlot(model: Model, transform : Transformation) : FixtureSlot<CockpitFixture>(model, transform) {
    override fun createFixture(): CockpitFixture {
        val vertices = arrayOfNulls<Vector2>(model.points)
        for (i in vertices.indices) {
            vertices[i] = model.asVectorData[i].copy()
                .multiply(localTransform.scale)
        }
        val polygon = Polygon(*vertices)
        polygon.rotate(localTransform.rotation.toRadians())
        polygon.translate(Vector2(localTransform.translation.x, localTransform.translation.y)) //Dyn4J is 2D :P
        val fixture = CockpitFixture(polygon)
        fixture.filter = TeamFilter(
            category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits
        )
        return fixture
    }
}