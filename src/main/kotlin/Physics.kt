import Graphics.Model
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.*
import org.dyn4j.geometry.*
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData
import java.util.function.Predicate

private data class ComponentDefinition(val model : Model, val localTransform: Transformation, val graphicalData: GraphicalData)

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
    //therefore maybe onCollide should simply flag things and store data, allowing .update() to clean up.
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
            effectsRequests.addAll(body.updateFixtures())
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
                val pair = createShip(request.scale, request.r, request.g, request.b, request.team);
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
        private val internalComponents: List<Component>,
        private val teamFilter: TeamFilter
    ) : AbstractPhysicsBody<CustomFixture>() {

        private companion object {
            private var UUID_COUNTER = 0
        }

        val team = teamFilter.team

        val uuid = UUID_COUNTER++

        init {
            for (component in internalComponents){
                reviveComponent(component)
            }
        }

        fun testFunc(){
            for (internalComponent in internalComponents.filter { it.getRespectiveFixture() == null }) {
                reviveComponent(internalComponent)
            }
        }

        private fun killComponent(component: Component){
            if(!internalComponents.contains(component)){
                throw IllegalArgumentException("tried to kill a component that is not under this entity")
            }else{
               if(component.getRespectiveFixture() == null){
                   throw IllegalStateException("tried to kill a component that is already dead under this entity")
               }else{
                   println("Removing")
                   removeFixture(component.getRespectiveFixture())
                   component.killFixture()
               }
            }
        }

        private fun reviveComponent(component: Component){
            if(!internalComponents.contains(component)){
                throw IllegalArgumentException("tried to kill a component that is not under this entity")
            }else{
                if(component.getRespectiveFixture() != null){
                    throw IllegalStateException("tried to revive a component that is already alive")
                }else{
                    val newFixture = component.reviveFixture()
                    addFixture(newFixture)
                }
            }
        }

        open class Component(val definition: ComponentDefinition, private val filter: TeamFilter, val adjacentComponents: List<Component>){
            private var respectiveFixture: CustomFixture? = null
            fun killFixture(){ respectiveFixture = null}
            fun reviveFixture(): CustomFixture  {
                val newFixture = createFixture(definition)
                respectiveFixture = newFixture
                return newFixture
            }
            fun getRespectiveFixture(): CustomFixture? { return respectiveFixture }
            fun generateRenderable(): RenderableComponent? {
                return if (respectiveFixture == null){
                    null
                }else{
                    RenderableComponent(definition.model, definition.localTransform, definition.graphicalData)
                }
            }
            private fun createFixture(componentDefinition: ComponentDefinition): CustomFixture {
                val vertices = arrayOfNulls<Vector2>(componentDefinition.model.points)
                for (i in vertices.indices) {
                    vertices[i] = componentDefinition.model.asVectorData[i].copy()
                        .multiply(componentDefinition.localTransform.scale.toDouble())
                }
                val polygon = Polygon(*vertices)
                polygon.translate(componentDefinition.localTransform.position.copy())
                polygon.rotate(componentDefinition.localTransform.rotation.toRadians())
                val fixture = CustomFixture(polygon)
                fixture.filter = filter
                return fixture
            }
        }

        class ThrusterComponent(definition: ComponentDefinition, filter:TeamFilter, val localExhaustDirection: Rotation, adjacentComponents: List<Component>) : Component(definition, filter, adjacentComponents)

        abstract fun isMarkedForRemoval(): Boolean

        fun updateFixtures() : List<EffectsRequest>{
            var didLoseParts = false;
            for (internalComponent in internalComponents) {
                internalComponent.getRespectiveFixture()?.let {
                    if (it.isMarkedForRemoval()){
                        it.onDestruction()
                        killComponent(internalComponent)
                        didLoseParts = true
                    }
                }
            }
            if(didLoseParts) setMass(MassType.NORMAL)
            return listOf()
        }
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
                                    comp.getRespectiveFixture()!!.getHealth().toFloat() / 100.0f
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
        private fun createShip(scale: Double, red: Float, green: Float, blue: Float, team: Team) : Pair<List<PhysicsEntity.Component>, List<PhysicsEntity.Component>>{
            val body = mutableListOf<PhysicsEntity.Component>()
            val thruster = mutableListOf<PhysicsEntity.Component>()
            body.add(
                PhysicsEntity.Component(
                    ComponentDefinition(
                        Model.SQUARE1,
                        Transformation(Vector2(0.0), scale),
                        GraphicalData(red, green, blue, 0.0f)),
                    TeamFilter(team, {it.UUID != team.UUID},
                        category = CollisionCategory.CATEGORY_SHIP.bits,
                        mask = CollisionCategory.CATEGORY_SHIP.bits),
                    listOf())
            )
            thruster.add(
                PhysicsEntity.Component(
                    ComponentDefinition(
                        Model.SQUARE1,
                        Transformation(Vector2(0.0), scale),
                        GraphicalData(red, green, blue, 0.0f)),
                    TeamFilter(team, {it.UUID != team.UUID},
                        category = CollisionCategory.CATEGORY_SHIP.bits,
                        mask = CollisionCategory.CATEGORY_SHIP.bits),
                    listOf())
            )
//            IntRange(0, 10).forEach { a ->
//                IntRange(0, 10).forEach { b ->
//                    if(a == 0){
//                        val comp = PhysicsEntity.Component(
//                            ComponentDefinition(
//                                Model.SQUARE1,
//                                Transformation(Vector2(a.toDouble()*0.2, b.toDouble()*0.2), scale * 0.2, 0.0),
//                                GraphicalData(red, green/2.0f, blue/2.0f, 0.0f)
//                            ), TeamFilter(team, {it.UUID != team.UUID},
//                                category = CollisionCategory.CATEGORY_SHIP.bits,
//                                mask = CollisionCategory.CATEGORY_SHIP.bits))
//                        body.add(comp)
//                        thruster.add(comp)
//                    }else{
//                        val comp = PhysicsEntity.Component(
//                            ComponentDefinition(
//                                Model.SQUARE1,
//                                Transformation(Vector2(a.toDouble()*0.2, b.toDouble()*0.2), scale * 0.2, 0.0),
//                                GraphicalData(red, green, blue, 0.0f)
//                            ), TeamFilter(team, {it.UUID != team.UUID},
//                                category = CollisionCategory.CATEGORY_SHIP.bits,
//                                mask = CollisionCategory.CATEGORY_SHIP.bits))
//                        body.add(comp)
//                    }
//                }
//            }

            return Pair(body, thruster)
        }
    }

    private open class ShipEntity(scale: Double, red: Float, green: Float, blue: Float, team: Team, comp: List<Component>, val thrusterComponents: List<Component>) : PhysicsEntity(
        comp, TeamFilter(
            team = team, teamPredicate = { it != team }, category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits or CollisionCategory.CATEGORY_PROJECTILE.bits
        )
    ) {

        init {
            if(thrusterComponents.isEmpty()){
                throw Exception("Attempted to create a ship with no thruster components! Not acceptable right now.")
            }
        }
        override fun isMarkedForRemoval(): Boolean = false

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

//    //physics DTOs
    open class PhysicsBodyData(
        val uuid: Int, val position: Vector2, val velocity: Vector2,
        val angle: Double, val angularVelocity: Double, val traceRadius: Double,
        val changeInPosition: Vector2, val changeInOrientation: Double, val team: Team
    ) {
    }

    private class PhysicsWorld : AbstractPhysicsWorld<CustomFixture, PhysicsEntity, WorldCollisionData<CustomFixture, PhysicsEntity>>() {

        override fun processCollisions(iterator: Iterator<WorldCollisionData<CustomFixture, PhysicsEntity>>) {
            super.processCollisions(iterator)
            //Note that this list is ephemeral and should not be accessed or referenced outside this very small scope.
            contactCollisions.forEach {
                it.pair.first.fixture.onCollide(it)
                it.pair.second.fixture.onCollide(it)
            }

        }

        override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, CustomFixture>>?): WorldCollisionData<CustomFixture, PhysicsEntity> {
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

    private class CustomFixture(shape: Convex?): BodyFixture(shape) {
        private var health = 100;
        fun onCollide(data: WorldCollisionData<CustomFixture, PhysicsEntity>) {
            health -= 10;
        }
        fun getHealth(): Int = health
        fun isMarkedForRemoval(): Boolean = health <= 0
        fun onDestruction() : List<EffectsRequest> {return listOf()}
    }
}

