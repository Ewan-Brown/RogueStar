import Graphics.Model
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.*
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData
import java.util.function.Predicate

class PhysicsLayer : Layer {

    enum class CollisionCategory(val bits: Long) {
        CATEGORY_SHIP(0b0001),
        CATEGORY_PROJECTILE(0b0010),
        CATEGORY_SHIELD(0b0100)
    }

    private val physicsWorld = PhysicsWorld()
    private var time = 0.0

    init {
        physicsWorld.setGravity(0.0, 0.0)
    }

    fun getBodyData(): Map<Int, PhysicsBodyData> {
        return physicsWorld.bodies.associate { it.uuid to it.createBodyData() }
    }

    private fun getEntity(uuid: Int): PhysicsEntity? {
        return physicsWorld.bodies.firstOrNull { it -> it.uuid == uuid }
    }

    //TODO benchmark and see if a cachemap ;) is necessary
    public fun getEntityData(uuid: Int): PhysicsBodyData? {
        val body = getEntity(uuid)
        return body?.let { it.createBodyData() }
    }

    public fun applyTorque(uuid: Int, torque: Double) {
        getEntity(uuid)?.applyTorque(torque)
    }

    public fun applyForce(uuid: Int, force: Vector2) {
        getEntity(uuid)?.applyForce(force)
    }

    //onCollide() is called during physicsWorld.update(), meaning it always occurs before any body.update() is called.
    //therefore onCollide should simply flag things and store data, allowing .update() to clean up.
    fun update(controlActions: Map<Int, List<ControlAction>>) : List<EffectsRequest>{
        time++
        physicsWorld.update(1.0)
        var i = physicsWorld.bodies.size
        if(controlActions.size > i){
            error("More controlActions were supplied than there are entities in the PhysicsWorld!")
        }
        val effectsRequests = mutableListOf<EffectsRequest>()
        while(i > 0){
            i--
            val body = physicsWorld.bodies[i]
            effectsRequests.addAll(body.update(controlActions.getOrElse(i) { emptyList() }))
            if(body.isMarkedForRemoval()){
                physicsWorld.removeBody(body)
                i--
            }
        }
        return effectsRequests
    }

    //TODO Can we delegate this or something
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        physicsWorld.populateModelMap(modelDataMap)
    }

    enum class RequestType {
        SHIP, PROJECTILE
    }

    /**
     * Put in a request to add a new entity to the game. Returns an integer referring to the id of the entity if successful, otherwise null
     */
    public fun requestEntity(request: EntityRequest): Int? {
        val entity = when (request.type) {
            RequestType.SHIP -> {
                ShipEntity(request.scale, request.r, request.g, request.b, request.team)
            }

            RequestType.PROJECTILE -> {
                ProjectileEntity(request.team)
            }
        }
        addEntity(entity)
        return entity.uuid
    }

    class EntityRequest(
        val type: RequestType, val position: Vector2, val velocity: Vector2 = Vector2(),
        val angle: Double = 0.0, val angularVelocity: Double = 0.0,
        val scale: Double = 1.0, val r: Float, val g: Float, val b: Float, val team: Team
    ) {
//        protected abstract fun process() : PhysicsEntity
    }

    private fun <E : PhysicsEntity> addEntity(entity: E, angle: Double, pos: Vector2): E {
        entity.rotate(angle)
        entity.translate(pos)
        entity.setMass(MassType.NORMAL)
        entity.originalCenterOfMass = entity.mass.center
        physicsWorld.addBody(entity)
        return entity
    }

    private fun <E : PhysicsEntity> addEntity(entity: E): E {
        entity.setMass(MassType.NORMAL)
        entity.originalCenterOfMass = entity.mass.center
        physicsWorld.addBody(entity)
        return entity
    }

    fun removeEntity(uuid: Int) {
        physicsWorld.bodies.find { it.uuid == uuid }.let {
            if (it == null) throw Exception("Entity with uuid $uuid not found")
        }
    }

    class PartInfo(
        val renderableProducer: () -> RenderableComponent?,
        val componentDefinition: PhysicalComponentDefinition,
        var health: Int,
        var removed: Boolean = false
    ) {}

    private abstract class PhysicsEntity protected constructor(
        val originalComponentDefinitions: List<PhysicalComponentDefinition>,
        private val teamFilter: TeamFilter
    ) : AbstractPhysicsBody() {

        private companion object {
            private var UUID_COUNTER = 0
        }

        val team = teamFilter.team
        var originalCenterOfMass: Vector2? = null
        val missingParts = mutableListOf<PhysicalComponentDefinition>()

        val uuid = UUID_COUNTER++

        init {
            for (componentDefinition in originalComponentDefinitions) {
                this.addFixture(createFixture(componentDefinition))
            }
        }

        override fun removeFixture(fixture: BodyFixture?): Boolean {
            if (fixture != null) {
                missingParts.add((fixture.userData as PartInfo).componentDefinition)
            }
            return super.removeFixture(fixture)
        }

        fun createFixture(componentDefinition: PhysicalComponentDefinition): BodyFixture {
            val vertices = arrayOfNulls<Vector2>(componentDefinition.model.points)
            for (i in vertices.indices) {
                vertices[i] = componentDefinition.model.asVectorData[i].copy()
                    .multiply(componentDefinition.localTransform.scale.toDouble())
            }
            val polygon = Polygon(*vertices)
            polygon.translate(componentDefinition.localTransform.position.copy())
            polygon.rotate(componentDefinition.localTransform.rotation.toRadians())
            val fixture = BodyFixture(polygon)
            fixture.filter = teamFilter
            fixture.userData = PartInfo({
                RenderableComponent(
                    componentDefinition.model,
                    componentDefinition.localTransform,
                    componentDefinition.graphicalData
                )
            }, componentDefinition, 100)
            return fixture
        }

        fun getLocalRenderables(): List<RenderableComponent> {
            return getFixtures().mapNotNull { (it.userData as PartInfo).renderableProducer() }
        }

        //Need to update the 'center'
        fun recalculateComponents() {
            for (renderable in getLocalRenderables()) {
                renderable.transform.position.subtract(this.getMass().center)
            }
        }

        abstract fun onCollide(data: WorldCollisionData<PhysicsEntity>)

        abstract fun isMarkedForRemoval(): Boolean

        abstract fun update(actions: List<ControlAction>): List<EffectsRequest>

        fun getComponents(): List<RenderableComponent> {
            if (!isEnabled) {
                return listOf()
            } else {
                val result: MutableList<RenderableComponent> = ArrayList()

                val entityAngle = getTransform().rotationAngle.toFloat()
                val entityPos = this.worldCenter

                for (partInfo in getFixtures().map { (it.userData as PartInfo) }) {
                    val renderable = partInfo.renderableProducer()
                    if (renderable != null) {
                        val newPos = renderable.transform.position.copy().subtract(this.getMass().center).rotate(entityAngle.toDouble()).add(entityPos)
                        val newAngle =
                            Rotation(renderable.transform.rotation.toRadians() + this.transform.rotationAngle)
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
                                    partInfo.health / 100.0f
                                )
                            )
                        )
                    }
                }
                return result
            }
        }

        fun createBodyData(): PhysicsBodyData {
            return PhysicsBodyData(
                uuid,
                worldCenter,
                linearVelocity,
                transform.rotationAngle,
                angularVelocity,
                rotationDiscRadius,
                changeInPosition,
                changeInOrientation,
                team
            );
        }
    }

    private open class ShipEntity(scale: Double, red: Float, green: Float, blue: Float, team: Team) : PhysicsEntity(
        listOf(
            PhysicalComponentDefinition(
                Model.TRIANGLE,
                Transformation(Vector2(0.0, 0.0), 1.0 * scale, 0.0),
                GraphicalData(red, green, blue, 0.0f)
            ),
            PhysicalComponentDefinition(
                Model.SQUARE1,
                Transformation(Vector2(-1.0 * scale, 0.0), 1.0 * scale, 0.0),
                GraphicalData(1.0f, 1.0f, 1.0f, 0.0f)
            )
        ), TeamFilter(
            team = team, teamPredicate = { it != team }, category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits or CollisionCategory.CATEGORY_PROJECTILE.bits
        )
    ) {

        override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
            //Do nothing
        }

        override fun isMarkedForRemoval(): Boolean = false

        private var isFrontFlipped = false
        fun testFlipFrontPart(){
            if(!isFrontFlipped) {
                println("testFlipFrontPart")
                println("Raw")
                println("world: $worldCenter,local: $localCenter, mass: ${mass.center}")

                this.removeFixture(0)
                println("Removed Fixture")
                println("world: $worldCenter,local: $localCenter, mass: ${mass.center}")

                val oldCenter = this.getMass().center.copy()
                this.setMass(MassType.NORMAL)
                println("Reset mass")
                println("world: $worldCenter,local: $localCenter, mass: ${mass.center}")
                val newCenter = this.getMass().center.copy()
                this.localCenter
                val centerDiff = newCenter.subtract(oldCenter)
//                recalculateComponents()
//                println("Recalculated Components")
//                println("world: $worldCenter,local: $localCenter, mass: ${mass.center}")
                isFrontFlipped = !isFrontFlipped
            }else{
                println("world: $worldCenter,local: $localCenter, mass: ${mass.center}")
                //            if (missingParts.isNotEmpty()) {
//                val first = missingParts.first()
//                val diff = mass.center.difference(originalCenterOfMass)
//                println(diff)
//                val newComp = PhysicalComponentDefinition(
//                    first.model, Transformation(
//                        first.localTransform.position.sum(diff.product(first.localTransform.scale)),
//                        first.localTransform.scale,
//                        first.localTransform.rotation
//                    ), first.graphicalData
//                )
//                addFixture(createFixture(newComp))
//                missingParts.remove(first)
//            }
            }

        }

        override fun update(actions: List<ControlAction>): List<EffectsRequest> {
            val effectsList = mutableListOf<EffectsRequest>()
            for (action in actions) {
                when(action){
                    is ControlAction.ShootAction -> TODO()
                    is ControlAction.ThrustAction -> {
                        applyForce(action.thrust)
                        effectsList.add(EffectsRequest.ExhaustRequest(this.worldCenter, this.transform.rotationAngle, this.changeInPosition!!))
                    }
                    is ControlAction.TurnAction -> {
                        applyTorque(action.torque)
                    }
                    is ControlAction.TestAction -> {
                        testFlipFrontPart()
                    }
                }
            }
            return effectsList

        }
    }

    //TODO use the physics engine to create ephemeral colliding/form-changing entities that represent explosions' force etc.
//  Maybe could collect these explosions and pass them to shader to do cool stuff...
    private class ProjectileEntity(team: Team) : PhysicsEntity(
        listOf(
            PhysicalComponentDefinition(
                Model.TRIANGLE,
                Transformation(Vector2(0.0, 0.0), 0.2, 0.0),
                GraphicalData(1.0f, 0.0f, 0.0f, 0.0f)
            )
        ),
        TeamFilter(
            team = team,
            teamPredicate = { it != team },
            category = CollisionCategory.CATEGORY_PROJECTILE.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits
        )
    ) {
        var hasCollided = false

        init {
            this.linearVelocity.add(Vector2(this.transform.rotationAngle).multiply(50.0))
        }

        override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
//            val otherBody = if (data.body1 == this) data.body2 else data.body1
//            val otherFixture = if (data.body1 == this) data.fixture2 else data.fixture1
//            (otherFixture.userData as PartInfo).health -= 10
//            if ((otherFixture.userData as PartInfo).health <= 0 && !(otherFixture.userData as PartInfo).removed) {
//                (otherFixture.userData as PartInfo).removed = true
//                otherBody.removeFixture(otherFixture)
//                EffectsUtils.debris(otherBody.createBodyData(), otherFixture.userData as PartInfo)
//                if (otherBody.fixtures.size == 0) {
//                    physicsLayer.removeEntity(otherBody.uuid);
//                    controllerLayer.removeController(otherBody.uuid);
//                } else {
//                    val oldCenterOfMass = Vector2(otherBody.mass.center);
//                    otherBody.setMass(MassType.NORMAL)
//                    val newCenterOfMass = otherBody.mass.center
//
//                    val centerOfMassDifference = newCenterOfMass.difference(oldCenterOfMass)
//
//                    //FIXME the reference to the removed part is invalid because the local transform that it should have applied might need to adjust based on the COM, which changes when the part (and further parts) are removed?
//                    for (fixture in otherBody.fixtures) {
//                        val partInfo = (fixture.userData as PartInfo)
//                        partInfo.renderableProducer()?.let { oldRenderable ->
//                            val transform = oldRenderable.transform
//                            transform.position.subtract(centerOfMassDifference)
//                            fixture.userData = PartInfo({
//                                RenderableComponent(
//                                    oldRenderable.model,
//                                    transform,
//                                    oldRenderable.graphicalData
//                                )
//                            }, partInfo.componentDefinition, partInfo.health, partInfo.removed)
//                        }
//                    }
//                }
//            }
            hasCollided = true
            isEnabled = false
        }

        override fun isMarkedForRemoval(): Boolean = hasCollided

        override fun update(actions: List<ControlAction>): List<EffectsRequest> {
            this.applyForce(Vector2(transform.rotationAngle).product(2.0))
            return listOf()
        }
    }

    //physics DTOs
    open class PhysicsBodyData(
        val uuid: Int, val position: Vector2, val velocity: Vector2,
        val angle: Double, val angularVelocity: Double, val traceRadius: Double,
        val changeInPosition: Vector2, val changeInOrientation: Double, val team: Team
    ) {
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

    class TeamFilter(
        val team: Team = Team.TEAMLESS,
        val teamPredicate: Predicate<Team> = Predicate { true },
        val category: Long,
        val mask: Long
    ) : Filter {
        override fun isAllowed(filter: Filter?): Boolean {
            filter ?: throw NullPointerException("filter can never be null...")

            return if (filter is TeamFilter) {
                //Check that the category/mask matches both directions
                //AND if team logic matches
                (this.category and filter.mask) > 0 && (filter.category and this.mask) > 0 && teamPredicate.test(filter.team) && filter.teamPredicate.test(
                    team
                )
            } else {
                true
            }
        }
    }
}
