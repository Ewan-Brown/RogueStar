import Graphics.Model
import PhysicsLayer.PhysicsEntity.*
import Graphics
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.*
import org.dyn4j.geometry.*
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData
import java.util.*
import java.util.function.Predicate
import kotlin.collections.HashMap
import kotlin.math.PI

//data class ComponentDefinition(val model : Model, val localTransform: Transformation)

data class PhysicsInput(val map : Map<Int, List<ControlAction>>)
data class PhysicsOutput(val requests: List<EffectsRequest>)

class PhysicsLayer(val models: List<Model>) : Layer<PhysicsInput, PhysicsOutput> {
    val graphicsService = GraphicsService()
    /**
     * Output of EntityDesigner, serialized to file and consumed by loadEntities().
     */
    public class EntityBlueprint(){
        var components: List<ComponentBlueprint> = listOf()
        var connections: Map<Int, List<Int>> = mapOf()
        constructor(comp: List<ComponentBlueprint>, conn: Map<Int, List<Int>>) : this(){
            components = comp
            connections = conn
        }
    }

    val simpleLoader: (EntityBlueprint, PhysicsWorld) -> ShipEntity = {
        entityBlueprint: EntityBlueprint, worldRef: PhysicsWorld ->
        val components = entityBlueprint.components
        val connections = entityBlueprint.connections
        val fixtureSlotMapping: Map<ComponentBlueprint, FixtureSlot<*>> = components.associateWith {
            val model = models[it.shape]
            val transform = Transformation(Vector3(it.position.x / 30.0, it.position.y / 30.0, 0.0), it.scale, it.rotation * PI / 2.0) //TODO Clean up this

            //TODO Add component implementations for each type!
            when(it.type){
                Type.THRUSTER -> ThrusterFixtureSlot(model, transform)
                Type.ROOT ,
                Type.GUN ,
                Type.BODY -> BasicFixtureSlot(model, transform)
            }

//            val model = models[it.shape]
//            val transform = Transformation(it.position / 30.0, it.scale, it.rotation * PI / 2.0)
//            val def = ComponentDefinition(model, transform)
//            def.
//            Component(
//                def,
//                TeamFilter(
//                    category = CollisionCategory.CATEGORY_SHIP.bits,
//                    mask = CollisionCategory.CATEGORY_SHIP.bits
//                )
//            )
        }

        fun getMatchingComponent(id: Int) = fixtureSlotMapping[components[id]]
        fun transform(ids: List<Int>) = ids.map { id -> getMatchingComponent(id)!! }

        val thrusters : List<ThrusterFixtureSlot> = fixtureSlotMapping.values.filterIsInstance<ThrusterFixtureSlot>()
        val root = fixtureSlotMapping.filter { it.key.type == Type.ROOT }.map { it.value }.first()
        val trueConnectionMap = connections.entries.associate { entry: Map.Entry<Int, List<Int>> ->
            getMatchingComponent(entry.key)!! to entry.value.map { getMatchingComponent(it)!! }.toList()
        }
        val s = ShipDetails(fixtureSlotMapping.values.toList(), thrusters, trueConnectionMap, root)
        ShipEntity(Team.TEAMLESS, s, worldRef)
    }
    //TODO Refactor this?
    enum class Blueprint{
        BULLET,
        DEFAULT_SHIP
    }

    val blueprintGenerator: (Blueprint) -> EntityFactory<*> = {
        when(it){
            Blueprint.BULLET -> bulletFactory;
            Blueprint.DEFAULT_SHIP -> defaultShipFactory;
        }
    }

    private val bulletFactory = EntityFactory(models, "bullet", PhysicsEntity::class.java, { simpleLoader(it, physicsWorld)}, mutableListOf());
    private val defaultShipFactory = EntityFactory(models, "ship_default", ShipEntity::class.java, { simpleLoader(it, physicsWorld)}, mutableListOf());

    public class EntityFactory<T : PhysicsEntity>(val models: List<Graphics.Model>, val internalName: String, val clazz : Class<T>, private val generator : (EntityBlueprint) -> T, val blueprintData: MutableList<EntityBlueprint>) {
        fun generate() : T{
            val ret = generator.invoke(blueprintData.first())
            ret.setMass(MassType.NORMAL)
            return ret //TODO Make loading from multiple resources choices possible?
        }
    }

    //Just a list of the ship factories available, useful for testing.
    private val shipFactories = mutableListOf<() -> PhysicsEntity>()

    public fun loadEntities(){
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addDeserializer(Vector2::class.java, VectorDeserializer())
        module.addDeserializer(ComponentBlueprint::class.java, ComponentDeserializer())
        mapper.registerModules(module)

        //Identify that all core entities are present, and create factories for each of them, then validate the data fits the factory.
        for (blueprint in Blueprint.entries.map{blueprintGenerator.invoke(it)}) {
            //Find matching resource file
            val matchingFile = this.javaClass.getResourceAsStream("/entities/entity_"+blueprint.internalName+".json")
            //TODO support having multiple to choose from for same resource

            val blueprintData = mapper.readValue(matchingFile, EntityBlueprint::class.java)
            blueprint.blueprintData.add(blueprintData)

            //Test generation of this entity
            blueprint.generate()
            blueprint.generate()
            blueprint.generate()
        }
        shipFactories.add ( blueprintGenerator.invoke(Blueprint.DEFAULT_SHIP)::generate )
    }

    class ComponentDeserializer() : StdDeserializer<ComponentBlueprint>(ComponentBlueprint::class.java){

        override fun deserialize(parser: JsonParser, p1: DeserializationContext): ComponentBlueprint {
            val node: JsonNode = parser.codec.readTree(parser)
            val shape = node.get("shape").asInt()
            val scale = node.get("scale").asDouble()
            val x = node.get("position").get("x").asDouble()
            val y = node.get("position").get("y").asDouble()
            val rotation = node.get("rotation").asInt()
            val type = node.get("type").asText()!!

            val position = Vector2(x, y)
            return ComponentBlueprint(shape, scale, position, rotation, Type.valueOf(type))
        }
    }

    private enum class CollisionCategory(val bits: Long) {
        CATEGORY_SHIP(0b0001),
        CATEGORY_PROJECTILE(0b0010),
        CATEGORY_SHIELD(0b0100)
    }

    private val physicsWorld = PhysicsWorld(blueprintGenerator)
    private var time = 0.0
    val thisVariable = "test"

    init {
        physicsWorld.setGravity(0.0, 0.0)
    }

    fun getBodyData(): Map<Int, PhysicsBodyData> {
        return physicsWorld.bodies.associate { it.uuid to it.createBodyData() }
    }

    private fun getEntity(uuid: Int): PhysicsEntity? {
        return physicsWorld.bodies.firstOrNull { it -> it.uuid == uuid }
    }

    fun getEntityData(uuid: Int): PhysicsBodyData? {
        val body = getEntity(uuid)
        return body?.let { it.createBodyData() }
    }

    //TODO this comment needsa second thought
    //onCollide() is called during physicsWorld.update(), meaning it always occurs before any body.update() is called.
    //therefore maybe onCollide should simply flag things and store data, allowing .update() to clean up.
    override fun update(input: PhysicsInput) : PhysicsOutput{
        val controlActions = input.map;
        time++
        physicsWorld.update(1.0)
        var i = physicsWorld.bodies.size
        if(controlActions.size > i){
            error("More controlActions were supplied than there are entities in the PhysicsWorld!")
        }
        while(i > 0){
            i--
            val body = physicsWorld.bodies[i]
            body.update(controlActions.getOrElse(body.uuid) { emptyList() })
            if(body.isMarkedForRemoval()){
                physicsWorld.removeBody(body)
                i--
            }
        }
        physicsWorld.processEntityBuffer()
        val effects = physicsWorld.effectsBuffer
        physicsWorld.effectsBuffer = mutableListOf()

        return PhysicsOutput(effects)
    }


    //TODO Can we delegate this or something
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.RenderableEntity>>) {
        physicsWorld.populateModelMap(modelDataMap)
    }

    /**
     * Process a request to add a new entity to the game. Returns an integer referring to the id of the entity if successful, otherwise null
     *
     */
    fun requestEntity(request: EntityRequest): Int {
        val entity = when (request.type) {
            RequestType.RANDOM_SHIP ->{
                addEntity(shipFactories.random().invoke(), request.angle, request.position)
            }
            RequestType.BULLET -> TODO()
        }
        return entity.uuid
    }

    /**
     * The input DTO for this layer - represents an outer request to create an entity.
     * Generally only used at game start or when an entity is immaculately conceived.
     * Otherwise, entities should be created here in Physics via other entities controls.
     */
    public enum class RequestType {
        RANDOM_SHIP,
        BULLET,
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

    fun removeEntity(uuid: Int) {
        physicsWorld.bodies.find { it.uuid == uuid }.let {
            if (it == null) throw Exception("Entity with uuid $uuid not found")
        }
    }

    public abstract class PhysicsEntity protected constructor(
        internalFixtureSlots: List<FixtureSlot<*>>,
        val root: FixtureSlot<*> = internalFixtureSlots[0],
        val teamFilter: TeamFilter,
        private val connectionMap: Map<FixtureSlot<*>, List<FixtureSlot<*>>>,
        val worldReference: PhysicsWorld
    ) : AbstractPhysicsBody<BasicFixture>() {

        private companion object {
            private var UUID_COUNTER = 0
        }

        val team = teamFilter.team
        val uuid = UUID_COUNTER++

        /**
         * This serves two purposes - to keep a list of all the components (fixture slots) as well as a reference to each of the components' fixtures -
         * if a reference is null it means the fixture is destroyed or removed
         * TODO Why not just have Component.fixture as a field?
         */
        val fixtureSlotFixtureMap: MutableMap<FixtureSlot<*>, BasicFixture?> = internalFixtureSlots.associateWith { null }.toMutableMap()

        init {
            for(value in internalFixtureSlots){
                reviveComponent(value)
            }
        }

        /**
         * Function used purely for manually testing proof of concept or debugging.
         */
        open fun testFunc(){
            val bullet = worldReference.blueprintGenerator(Blueprint.BULLET).generate()
            for (entry in fixtureSlotFixtureMap.entries) {
                entry.key
            }
            bullet.translate(-bullet.worldCenter.x, -bullet.worldCenter.y)
            worldReference.entityBuffer.add(bullet);
        }

        /**
         * Will remove a given component, and if not disabled, will do 'split check' which checks if this results in a fragment of the entity being lost due to lack of physical connection
         * The reason for the flag is to allow for when we are removing components *due* to an initial destruction, where we don't need to trigger this because we're already processing it.
         * */
        private fun processComponentDestruction(fixtureSlot: FixtureSlot<*>, trueDestruction: Boolean = true){
            if(!fixtureSlotFixtureMap.contains(fixtureSlot)){
                throw IllegalArgumentException("tried to kill a component that is not under this entity")
            }else{
               if(fixtureSlotFixtureMap[fixtureSlot] == null){
                   throw IllegalStateException("tried to kill a component that is already dead under this entity - $fixtureSlot")
               }else {
                   removeFixture(fixtureSlotFixtureMap[fixtureSlot])
                   fixtureSlotFixtureMap[fixtureSlot] = null
                   //Split check!
                   if (trueDestruction) {
                       val branchRoots = connectionMap[fixtureSlot]
                       val nodesAlreadyCounted = mutableListOf<FixtureSlot<*>>()
                       val branches: List<List<FixtureSlot<*>>> = branchRoots!!.mapNotNull {
                           if (fixtureSlotFixtureMap[it] == null){
                               return@mapNotNull null
                           }
                           if (nodesAlreadyCounted.contains(it)) {
                               return@mapNotNull null
                           } else {
                               val accumulator = mutableListOf<FixtureSlot<*>>()
                               val toIgnore = fixtureSlot
                               val nodesToExplore = Stack<FixtureSlot<*>>()
                               nodesToExplore.push(it)
                               while (nodesToExplore.isNotEmpty()) {
                                   val node = nodesToExplore.pop()
                                   if (node != toIgnore && !accumulator.contains(node) && fixtureSlotFixtureMap[node] != null) {
                                       accumulator.add(node)
                                       nodesAlreadyCounted.add(node)
                                       nodesToExplore.addAll(connectionMap[node]!!)
                                   }
                               }
                               return@mapNotNull accumulator
                           }
                       }
                       for (branch in branches) {
                           if (branch.contains(root)) {
                               //This branch will remain a part of this entity - all other branches will be fragmented off!
                           } else {
                               //Remove fragmented branch from this entity
                               for (branchComponent in branch) {
                                   processComponentDestruction(branchComponent, false) //false == non-recursive
                               }
                               //Generate a new connection map for this new entity
                               val newConnections = mutableMapOf<FixtureSlot<*>, List<FixtureSlot<*>>>()
                               val tempComponentMap = branch.associateWith { it -> copyFixtureSlot(it)}
//                               Iterate across the connections of each component in this branch, creating a structural copy with the new components
                               for(connectionEntry in connectionMap.filterKeys { branch.contains(it) }) { //Iterate over each branch element
                                   newConnections[connectionEntry.key] = connectionEntry.value.filter { branch.contains(it) }.map { tempComponentMap[it]!! }
                               }
                               val newEntity = ShipEntity(this.team, ShipDetails(newConnections.keys.toList(), listOf(), newConnections, newConnections.keys.toList()[0]), worldReference)
                               newEntity.translate(this.localCenter.product(-1.0))
                               newEntity.rotate(this.transform.rotationAngle)
                               newEntity.translate(this.worldCenter)
                               worldReference.entityBuffer.add(newEntity)
                           }
                       }
                   }
               }
            }
        }

        /**
         * Assuming this component's fixture is 'dead' this will regenerate it and add it back to the body.
         */
        fun reviveComponent(fixtureSlot: FixtureSlot<*>){
            if(!fixtureSlotFixtureMap.contains(fixtureSlot)){
                throw IllegalArgumentException("tried to kill a component that is not under this entity")
            }else{
                if(fixtureSlotFixtureMap[fixtureSlot] != null){
                    throw IllegalStateException("tried to revive a component that is already alive")
                }else{
                    val fixture = fixtureSlot.createFixture()
                    fixtureSlotFixtureMap[fixtureSlot] = fixture
                    addFixture(fixture)
                }
            }
        }

        data class RenderData(val localPosition: Transformation)

        /**
         * A slot for a fixture of a given type. Think carefully about what fields should go in slot vs fixture.
         */
        sealed class FixtureSlot<T : BasicFixture>(val model: Model, val localTransform: Transformation){
            abstract fun createFixture(): T
            fun onDestruction(){}
            abstract fun getRenderData() : RenderData
        }

        fun <F : FixtureSlot<*>> copyFixtureSlot(a: F) : F {
            return when(a){
                is BasicFixtureSlot -> BasicFixtureSlot(a.model, a.localTransform) as F
                is ThrusterFixtureSlot -> ThrusterFixtureSlot(a.model, a.localTransform) as F
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

            override fun getRenderData(): RenderData {
                TODO("Not yet implemented")
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

            override fun getRenderData(): RenderData {
                TODO("Not yet implemented")
            }
        }

        abstract fun isMarkedForRemoval(): Boolean

        //Check if any parts needs to be removed, and then calculate new center of mass.
        private fun updateFixtures(){
            var didLoseParts = false;
            val effectsList = mutableListOf<EffectsRequest>()
            val entityList = mutableListOf<PhysicsEntity>();
            for (entry in fixtureSlotFixtureMap) {
                entry.value?.let{
                    if (it.isMarkedForRemoval()){
                        entry.key.onDestruction()
                        processComponentDestruction(entry.key)
                        didLoseParts = true
                    }
                }
            }
            if(didLoseParts) setMass(MassType.NORMAL)
        }

        fun update(actions: List<ControlAction>){
            updateFixtures();
            processControlActions(actions);
        }

        abstract fun processControlActions(actions: List<ControlAction>)

        fun getRenderableComponents(): List<Graphics.RenderableEntity> {
            if (!isEnabled) {
                return listOf()
            } else {
                val result: MutableList<Graphics.RenderableEntity> = ArrayList()

                val entityAngle = getTransform().rotationAngle.toFloat()
                val entityPos = this.worldCenter

                for (comp in fixtureSlotFixtureMap.entries){
                    val renderable = transformLocalRenderableToGlobal(worldReference.graphicsService, comp.key)
                    if(renderable != null) {
                        result.add(renderable)
                    }
                }
                return result
            }
        }

        /**
         * Take a component (which has coordinates in local space) and transform it to global space, then wrap it with graphical details
         */
        fun transformLocalRenderableToGlobal(graphicsService: GraphicsService, fixtureSlot: FixtureSlot<*>) : Graphics.RenderableEntity?{
            return graphicsService.componentToRenderable(this, fixtureSlot);
            return null
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

    public data class ShipDetails(val fixtureSlots: List<FixtureSlot<*>>, val thrusters: List<ThrusterFixtureSlot>, val connectionMap: Map<FixtureSlot<*>, List<FixtureSlot<*>>>, val cockpit: FixtureSlot<*>)

    open class ShipEntity(team: Team, shipDetails: ShipDetails, worldReference: PhysicsWorld) : PhysicsEntity(
        shipDetails.fixtureSlots, shipDetails.cockpit, TeamFilter(
            team = team, doesCollide = { it != team }, category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits or CollisionCategory.CATEGORY_PROJECTILE.bits
        ), shipDetails.connectionMap, worldReference
    ) {

        val thrusterComponents = shipDetails.thrusters
        override fun isMarkedForRemoval(): Boolean = false

        override fun testFunc(){
            super.testFunc()
        }

        override fun processControlActions(actions: List<ControlAction>) {

            for (action in actions) {
                when(action){
                    is ControlAction.ShootAction -> TODO()
                    is ControlAction.ThrustAction -> {
                        val thrusterCount = thrusterComponents.count { return@count fixtureSlotFixtureMap[it] != null }
                        applyForce(action.thrust.product(thrusterCount.toDouble() / thrusterComponents.size.toDouble()))
                        for (thrusterComponent in thrusterComponents) {
                            transformLocalRenderableToGlobal(worldReference.graphicsService, thrusterComponent)?.transform?.let{
                                worldReference.effectsBuffer.add(EffectsRequest.ExhaustRequest(it.translation, it.rotation.toRadians(), Vector2()))
                            }
                        }
                    }
                    is ControlAction.TurnAction -> {
                        applyTorque(action.torque * this.getMass().mass)
                    }
                    is ControlAction.TestAction -> {
                        testFunc()
                    }
                }
            }
        }
    }

    //physics DTO as layer output
    open class PhysicsBodyData(
        val uuid: Int, val position: Vector2, val velocity: Vector2,
        val angle: Double, val angularVelocity: Double, val traceRadius: Double,
        val changeInPosition: Vector2, val changeInOrientation: Double, val team: Team
    )
    /**
     * Extension of Dyn4J world
     */
    class PhysicsWorld(val blueprintGenerator: (Blueprint) -> EntityFactory<*>) : AbstractPhysicsWorld<BasicFixture, PhysicsEntity, WorldCollisionData<BasicFixture, PhysicsEntity>>() {

        val graphicsService = GraphicsService()
        val entityBuffer = mutableListOf<PhysicsEntity>()
        var effectsBuffer = mutableListOf<EffectsRequest>()

        override fun processCollisions(iterator: Iterator<WorldCollisionData<BasicFixture, PhysicsEntity>>) {
            super.processCollisions(iterator)
            contactCollisions.forEach {
                it.pair.first.fixture.onCollide(it)
                it.pair.second.fixture.onCollide(it)
            }
        }

        /**
         * Entites should never be added via addBody, they should go through the buffer first. this lets us handle anything that needs to be handled.
         */
        fun processEntityBuffer(){
            for (physicsEntity in entityBuffer) {
                addBody(physicsEntity)
            }
            entityBuffer.clear()
        }

        override fun createCollisionData(pair: CollisionPair<CollisionItem<PhysicsEntity, BasicFixture>>?): WorldCollisionData<BasicFixture, PhysicsEntity> {
            return WorldCollisionData(pair)
        }

        fun populateModelMap(map: HashMap<Model, MutableList<Graphics.RenderableEntity>>) {
            for (body in this.bodies) {
                for (component in body.getRenderableComponents()) {
//                    map[component.model]!!.add(Pair(component.transform, component.graphicalData))
                    map[component.model]!!.add(component)
                }
            }
        }
    }

    /**
     * A bit-wise (and optional predicate) filter to determine if a given Fixture should intersect with one from another entity
     */
    class TeamFilter(
        val team: Team = Team.TEAMLESS,
        val doesCollide: Predicate<Team> = Predicate { it != team  || it == Team.TEAMLESS || team == Team.TEAMLESS},
        val category: Long,
        val mask: Long
    ) : Filter {
        override fun isAllowed(filter: Filter): Boolean {
            filter ?: throw NullPointerException("filter can never be null...")
            return if (filter is TeamFilter) {
                //Check that the category/mask matches both directions
                //AND if team logic matches
                (this.category and filter.mask) > 0 && (filter.category and this.mask) > 0 && doesCollide.test(filter.team) && filter.doesCollide.test(
                    team
                )
            } else {
                true
            }
        }
    }
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
}

