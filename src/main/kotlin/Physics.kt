import Graphics.Model
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData
import java.util.function.Predicate
import kotlin.math.cos

class PhysicsLayer : Layer{

    enum class CollisionCategory(val bits: Long){
        CATEGORY_SHIP(      0b0001),
        CATEGORY_PROJECTILE(0b0010),
        CATEGORY_SHIELD(    0b0100)
    }

    private val physicsWorld = PhysicsWorld()
    private var time = 0.0
    init {
        physicsWorld.setGravity(0.0,0.0)
    }

    fun getBodyData() : List<Pair<Int, PhysicsBodyData>>{
        return physicsWorld.bodies.map {
            it.uuid to PhysicsBodyData(it)
        }
    }

    //TODO benchmark and see if a cachemap ;) is necessary
    fun getEntity(uuid : Int) : PhysicsBodyData? {
        val body = physicsWorld.bodies.firstOrNull { it.uuid == uuid }
        return body?.let { PhysicsBodyData(it) }
    }

    //onCollide() is called during physicsWorld.update(), meaning it always occurs before any body.update() is called.
    //therefore onCollide should simply flag things and store data, allowing .update() to clean up.
    override fun update() {
        time++
        physicsWorld.update(1.0)
        var i = physicsWorld.bodies.size
        while(i > 0){
            i--
            val body = physicsWorld.bodies[i]
            body.update()
            if(body.isMarkedForRemoval()){
                physicsWorld.removeBody(body)
                i--
            }
        }
    }

    //TODO Can we delegate this or something
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        physicsWorld.populateModelMap(modelDataMap)
    }

    fun <E : PhysicsEntity> addEntity(entity: E, angle : Double, pos : Vector2) : E{
        entity.rotate(angle)
        entity.translate(pos)
        entity.setMass(MassType.NORMAL)
        entity.recalculateComponents()
        physicsWorld.addBody(entity)
        return entity
    }

    fun <E : PhysicsEntity> addEntity(entity: E) : E{
        entity.setMass(MassType.NORMAL)
        entity.recalculateComponents()
        physicsWorld.addBody(entity)
        return entity
    }

    fun removeEntity(entity: PhysicsEntity){
        physicsWorld.removeBody(entity)
    }

}

//physics DTOs
open class PhysicsBodyData(val position: Vector2?, val velocity: Vector2?, val angle: Double, val traceRadius: Double, val team: Team){
    constructor(body: PhysicsEntity) : this(body.worldCenter, body.linearVelocity, body.transform.rotationAngle, body.rotationDiscRadius, body.team)
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
        return WorldCollisionData(pair)
    }

    fun populateModelMap(map: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        for (body in this.bodies) {
            for (component in body.getComponents()) {
                map[component.model]!!.add(Pair(component.transform, component.graphicalData))
            }
        }
    }
}


class TeamFilter(val team : Team = Team.TEAMLESS, val teamPredicate : Predicate<Team> = Predicate { true }, val category : Long, val mask : Long) : Filter{
    override fun isAllowed(filter: Filter?): Boolean {
        filter ?: throw NullPointerException("filter can never be null...")

        return if(filter is TeamFilter){
            //Check that the category/mask matches both directions
            //AND if team logic matches
            (this.category and filter.mask) > 0 && (filter.category and this.mask) > 0 && teamPredicate.test(filter.team) && filter.teamPredicate.test(team)
        }else{
            true
        }
    }
}

abstract class PhysicsEntity protected constructor(compDefinitions: List<PhysicalComponentDefinition>, private val teamFilter: TeamFilter) : AbstractPhysicsBody() {
    val team = teamFilter.team
    private companion object {
        private var UUID_COUNTER = 0
    }

    private val missingParts = mutableListOf<PhysicalComponentDefinition>()
    class PartInfo(val renderableProducer: () -> RenderableComponent?, val componentDefinition: PhysicalComponentDefinition, var health: Int, var removed: Boolean = false){}

    val uuid = UUID_COUNTER++

    init {
        for (componentDefinition in compDefinitions) {
            this.addFixture(createFixture(componentDefinition))
        }
    }

    override fun removeFixture(fixture: BodyFixture?): Boolean {
        if(fixture != null){
            missingParts.add((fixture.userData as PartInfo).componentDefinition)
        }
        return super.removeFixture(fixture)
    }

    fun createFixture(componentDefinition: PhysicalComponentDefinition) : BodyFixture{
        val vertices = arrayOfNulls<Vector2>(componentDefinition.model.points)
        for (i in vertices.indices) {
            vertices[i] = componentDefinition.model.asVectorData[i].copy().multiply(componentDefinition.localTransform.scale.toDouble())
        }
        val v = Polygon(*vertices)
        v.translate(componentDefinition.localTransform.position.copy())
        v.rotate(componentDefinition.localTransform.rotation.toRadians())
        val f = BodyFixture(v)
        f.createMass()
        f.filter = teamFilter
        f.userData = PartInfo ({
            RenderableComponent(
                componentDefinition.model,
                componentDefinition.localTransform,
                componentDefinition.graphicalData
            )
        }, componentDefinition, 100)
        return f
    }

    fun getLocalRenderables(): List<RenderableComponent> {
        return getFixtures().mapNotNull { (it.userData as PartInfo).renderableProducer() }
    }

    //Need to update the 'center'
    fun recalculateComponents(){
        for (renderable in getLocalRenderables()) {
            renderable.transform.position.subtract(this.getMass().center)
        }
    }

    abstract fun onCollide(data: WorldCollisionData<PhysicsEntity>)

    abstract fun isMarkedForRemoval() : Boolean

    abstract fun update()

    fun getComponents(): List<RenderableComponent> {
        if(!isEnabled){
            return listOf()
        }else {
            val result: MutableList<RenderableComponent> = ArrayList()

            val entityAngle = getTransform().rotationAngle.toFloat()
            val entityPos = this.worldCenter

            for (partInfo in getFixtures().map { (it.userData as PartInfo)}) {
                val renderable = partInfo.renderableProducer()
                if(renderable != null) {
                    val newPos = renderable.transform.position.copy().rotate(entityAngle.toDouble()).add(entityPos)
                    val newAngle = Rotation(renderable.transform.rotation.toRadians() + this.transform.rotationAngle)
                    val scale = renderable.transform.scale

                    //TODO We should probably have two different types -
                    //  'renderable data that is static and attached to fixture'
                    //  and 'class that represents ephemeral data describing a fixture's rendering'
                    result.add(
                        RenderableComponent(
                            renderable.model,
                            Transformation(newPos, scale, newAngle),
                            GraphicalData(
                                renderable.graphicalData.red,
                                renderable.graphicalData.green,
                                renderable.graphicalData.blue,
                                renderable.graphicalData.z,
                                partInfo.health/100.0f
                                )
                        )
                    )
                }
            }
            return result
        }
    }
}

open class ShipEntity(scale : Double, red : Float, green : Float, blue : Float, team : Team) : PhysicsEntity(listOf(
    PhysicalComponentDefinition(
        Model.TRIANGLE,
        Transformation(Vector2(0.0, 0.0), 1.0*scale, 0.0),
        GraphicalData(red, green, blue, 0.0f)),
    PhysicalComponentDefinition(
        Model.SQUARE1,
        Transformation(Vector2(-1.0*scale, 0.0), 1.0*scale, 0.0),
        GraphicalData(1.0f, 1.0f, 1.0f, 0.0f))), TeamFilter(team = team, teamPredicate = { it != team}, category = PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits,
    mask = PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits or PhysicsLayer.CollisionCategory.CATEGORY_PROJECTILE.bits)) {

    override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
        val isOtherBody = data.body1 == this
    }

    override fun isMarkedForRemoval() : Boolean = false

    override fun update() {}
}

//TODO use the physics engine to create ephemeral colliding/form-changing entities that represent explosions' force etc.
//  Maybe could collect these explosions and pass them to shader to do cool stuff...
class ProjectileEntity(team : Team) : PhysicsEntity(listOf(
    PhysicalComponentDefinition(
        Model.TRIANGLE,
        Transformation(Vector2(0.0, 0.0), 0.2, 0.0),
        GraphicalData(1.0f, 0.0f, 0.0f, 0.0f))),
    TeamFilter(team = team, teamPredicate = { it != team}, category = PhysicsLayer.CollisionCategory.CATEGORY_PROJECTILE.bits, mask = PhysicsLayer.CollisionCategory.CATEGORY_SHIP.bits)) {
    var hasCollided = false

    init {
        this.linearVelocity.add(Vector2(this.transform.rotationAngle).multiply(50.0))
    }

    override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
        val otherBody = if(data.body1 == this) data.body2 else data.body1
        val otherFixture = if(data.body1 == this) data.fixture2 else data.fixture1

        (otherFixture.userData as PartInfo).health -= 10000
        if((otherFixture.userData as PartInfo).health <= 0 && !(otherFixture.userData as PartInfo).removed){
            (otherFixture.userData as PartInfo).removed = true
            otherBody.removeFixture(otherFixture)
            EffectsUtils.debris(otherBody, otherFixture)
            if(otherBody.fixtures.size == 0){
                physicsLayer.removeEntity(otherBody);
                controllerLayer.removeController(otherBody);
            }else{
                otherBody.setMass(MassType.NORMAL)
                otherBody.recalculateComponents()
            }
        }
        hasCollided = true
        isEnabled = false
    }

    override fun isMarkedForRemoval() : Boolean = hasCollided

    override fun update() {
        this.applyForce(Vector2(transform.rotationAngle).product(2.0))
    }
}