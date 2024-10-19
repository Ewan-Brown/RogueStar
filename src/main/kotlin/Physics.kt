import Graphics.Model
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.collision.Filter
import org.dyn4j.collision.Fixture
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
                val pair = createShip(request.scale, request.r, request.g, request.b);
                ShipEntity(request.scale, request.r, request.g, request.b, request.team, pair.first, pair.second)
            }

            RequestType.PROJECTILE -> {
//                ProjectileEntity(request.team)
                TODO()
            }
        }
        addEntity(entity, request.angle, request.position)
        return entity.uuid
    }

    class EntityRequest(
        val type: RequestType, val position: Vector2, val velocity: Vector2 = Vector2(),
        val angle: Double = 0.0, val angularVelocity: Double = 0.0,
        val scale: Double = 1.0, val r: Float, val g: Float, val b: Float, val team: Team
    )

    private fun <E : PhysicsEntity> addEntity(entity: E, angle: Double, pos: Vector2): E {
        entity.rotate(angle)
        entity.translate(pos)
        entity.setMass(MassType.NORMAL)
        physicsWorld.addBody(entity)
        return entity
    }

    private fun <E : PhysicsEntity> addEntity(entity: E): E {
        entity.setMass(MassType.NORMAL)
        physicsWorld.addBody(entity)
        return entity
    }

    fun removeEntity(uuid: Int) {
        physicsWorld.bodies.find { it.uuid == uuid }.let {
            if (it == null) throw Exception("Entity with uuid $uuid not found")
        }
    }

    private abstract class PhysicsEntity protected constructor(
        private val internalComponents: List<EntityComponent>,
        private val teamFilter: TeamFilter
    ) : AbstractPhysicsBody() {

        private companion object {
            private var UUID_COUNTER = 0
        }

        val team = teamFilter.team

        val uuid = UUID_COUNTER++

        init {
            for (component in internalComponents){
                val fixture = createFixture(component.definition)
                this.addFixture(fixture)
                component.setRespectiveFixture(fixture)
            }
        }

        open class EntityComponent(val definition: PhysicalComponentDefinition){
            fun getHealth() : Int = 100
            private var respectiveFixture: Fixture? = null
            fun setRespectiveFixture(fix: Fixture) { respectiveFixture = fix }
            fun removeRespectiveFixture() { respectiveFixture = null }
            fun getRespectiveFixture(): Fixture? { return respectiveFixture }
            fun generateRenderable(): RenderableComponent? {
                return if (respectiveFixture == null){
                    null
                }else{
                    RenderableComponent(definition.model, definition.localTransform, definition.graphicalData)
                }
            }
        }

        class ThrusterComponent(definition: PhysicalComponentDefinition, val localExhaustDirection: Rotation) : EntityComponent(definition)


        override fun removeFixture(fixture: BodyFixture?): Boolean {
            internalComponents.find { it.getRespectiveFixture() == fixture }?.let {
                it.removeRespectiveFixture()
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
            return fixture
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

                for (comp in internalComponents){
                    val renderable = comp.generateRenderable()
                    if(renderable != null) {
                        val newPos = renderable.transform.position.copy().subtract(this.getMass().center)
                            .rotate(entityAngle.toDouble()).add(entityPos)
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
                                    comp.getHealth().toFloat() / 100.0f
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

    companion object{
        private fun createShip(scale: Double, red: Float, green: Float, blue: Float) : Pair<List<PhysicsEntity.EntityComponent>, List<PhysicsEntity.EntityComponent>>{
            val body = mutableListOf<PhysicsEntity.EntityComponent>()
            val thruster = mutableListOf<PhysicsEntity.EntityComponent>()
            IntRange(0, 10).forEach { a ->
                IntRange(0, 10).forEach { b ->
                    if(a == 0){
                        val comp = PhysicsEntity.EntityComponent(
                            PhysicalComponentDefinition(
                                Model.SQUARE1,
                                Transformation(Vector2(a.toDouble()*0.2, b.toDouble()*0.2), scale * 0.2, 0.0),
                                GraphicalData(red, green/2.0f, blue/2.0f, 0.0f)
                            )
                        )
                        body.add(comp)
                        thruster.add(comp)
                    }else{
                        val comp = PhysicsEntity.EntityComponent(
                            PhysicalComponentDefinition(
                                Model.SQUARE1,
                                Transformation(Vector2(a.toDouble()*0.2, b.toDouble()*0.2), scale * 0.2, 0.0),
                                GraphicalData(red, green, blue, 0.0f)
                            )
                        )
                        body.add(comp)
                    }
                }
            }

            return Pair(body, thruster)
        }
    }

    private open class ShipEntity(scale: Double, red: Float, green: Float, blue: Float, team: Team, comp: List<EntityComponent>, val thrusterComponents: List<EntityComponent>) : PhysicsEntity(
        comp, TeamFilter(
            team = team, teamPredicate = { it != team }, category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits or CollisionCategory.CATEGORY_PROJECTILE.bits
        )
    ) {

        override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
            //Do nothing
            val thisOnesFixture = if(data.pair.first.body == this) data.pair.first.fixture else data.pair.second.fixture
            removeFixture(thisOnesFixture)
            setMass(MassType.NORMAL)
        }

        override fun isMarkedForRemoval(): Boolean = false

        fun testFunc(){
            println("testFunc()!")
        }

        override fun update(actions: List<ControlAction>): List<EffectsRequest> {
            val effectsList = mutableListOf<EffectsRequest>()
            for (action in actions) {
                when(action){
                    is ControlAction.ShootAction -> TODO()
                    is ControlAction.ThrustAction -> {
                        //Count thrusters
                        val thrusterCount = thrusterComponents.count { return@count it.getRespectiveFixture() != null }
                        applyForce(action.thrust.product(thrusterCount.toDouble() / thrusterComponents.size.toDouble()))
                        effectsList.add(EffectsRequest.ExhaustRequest(this.worldCenter, this.transform.rotationAngle, this.changeInPosition!!))
                    }
                    is ControlAction.TurnAction -> {
                        applyTorque(action.torque)
                    }
                    is ControlAction.TestAction -> {
                        testFunc()
                    }
                }
            }
            return effectsList

        }
    }

    //TODO use the physics engine to create ephemeral colliding/form-changing entities that represent explosions' force etc.
//  Maybe could collect these explosions and pass them to shader to do cool stuff...
//    private class ProjectileEntity(team: Team) : PhysicsEntity(
//        listOf(
//            PhysicalComponentDefinition(
//                Model.TRIANGLE,
//                Transformation(Vector2(0.0, 0.0), 0.2, 0.0),
//                GraphicalData(1.0f, 0.0f, 0.0f, 0.0f)
//            )
//        ),
//        TeamFilter(
//            team = team,
//            teamPredicate = { it != team },
//            category = CollisionCategory.CATEGORY_PROJECTILE.bits,
//            mask = CollisionCategory.CATEGORY_SHIP.bits
//        )
//    ) {
//        var hasCollided = false
//
//        init {
//            this.linearVelocity.add(Vector2(this.transform.rotationAngle).multiply(50.0))
//        }
//
//        override fun onCollide(data: WorldCollisionData<PhysicsEntity>) {
//            //Cause damage to the other part!
//            hasCollided = true
//            isEnabled = false
//        }
//
//        override fun isMarkedForRemoval(): Boolean = hasCollided
//
//        override fun update(actions: List<ControlAction>): List<EffectsRequest> {
//            this.applyForce(Vector2(transform.rotationAngle).product(2.0))
//            return listOf()
//        }
//    }
//
//    //physics DTOs
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
