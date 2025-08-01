import BasicFixtureSlot.*
import Graphics.Model
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.geometry.*
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData
import kotlin.collections.HashMap
import kotlin.math.PI

//data class ComponentDefinition(val model : Model, val localTransform: Transformation)

data class PhysicsInput(val map : Map<Int, List<ControlCommand>>, val timeStep: Double)
data class PhysicsOutput(val requests: List<EffectsRequest>, val timeElapsed: Double)

class PhysicsLayer(val models: List<Model>) : Layer<PhysicsInput, PhysicsOutput> {
    /**
     * Output of EntityDesigner, serialized to file and consumed by loadEntities().
     */
    class EntityBlueprint(){
        var components: List<ComponentBlueprint> = listOf()
        var connections: Map<Int, List<Int>> = mapOf()
        constructor(comp: List<ComponentBlueprint>, conn: Map<Int, List<Int>>) : this(){
            components = comp
            connections = conn
        }
    }

    //TODO Duplicated code, not very extensible 'loader' functions...
    val shipLoader:(EntityBlueprint, PhysicsWorld, Team?, Double) -> ShipEntity = {
            entityBlueprint: EntityBlueprint, worldRef: PhysicsWorld, team: Team?, scale: Double ->
        val components = entityBlueprint.components
        val connections = entityBlueprint.connections
        val fixtureSlotMapping: Map<ComponentBlueprint, AbstractFixtureSlot<*>> = components.associateWith {
            val model = models[it.shape]
            val transform = Transformation(Vector3(it.position.x / 30.0 * scale, it.position.y / 30.0 * scale, 0.0), it.scale * scale, it.rotation * PI / 2.0) //TODO Clean up this

            when(it.type){
                Type.THRUSTER -> ThrusterFixtureSlot(model, transform)
                Type.COCKPIT -> CockpitFixtureSlot(model, transform)
                Type.GUN -> {
                    RifleFixtureSlot(model, transform) { _: RifleFixtureSlot.RifleFixture -> bulletFactory.generate(scale) }
                }
                Type.BODY -> BasicFixtureSlot(model, transform, CollisionCategory.CATEGORY_SHIP, CollisionCategory.CATEGORY_SHIP.bits)
            }
        }

        fun getMatchingComponent(id: Int) = fixtureSlotMapping[components[id]]

        val trueConnectionMap = connections.entries.associate { entry: Map.Entry<Int, List<Int>> ->
            getMatchingComponent(entry.key)!! to entry.value.map { getMatchingComponent(it)!! }.toList()
        }
        val s = ShipDetails(fixtureSlotMapping.values.toList(), trueConnectionMap, team)
        ShipEntity(s, worldRef, scale)
    }

    val simpleLoader: (EntityBlueprint, PhysicsWorld, Double) -> AbstractPhysicsEntity = {
        entityBlueprint: EntityBlueprint, worldRef: PhysicsWorld, scale: Double ->
        val components = entityBlueprint.components
        val connections = entityBlueprint.connections
        val fixtureSlotMapping: Map<ComponentBlueprint, AbstractFixtureSlot<*>> = components.associateWith {
            val model = models[it.shape]
            val transform = Transformation(Vector3(it.position.x / 30.0 * scale, it.position.y / 30.0 * scale, 0.0), it.scale * scale   , it.rotation * PI / 2.0) //TODO Clean up this

            when(it.type){
                Type.THRUSTER -> ThrusterFixtureSlot(model, transform)
                Type.COCKPIT -> CockpitFixtureSlot(model, transform)
                Type.GUN -> {
                    RifleFixtureSlot(model, transform) { _: RifleFixtureSlot.RifleFixture -> bulletFactory.generate(scale) }
                }
                Type.BODY -> BasicFixtureSlot(model, transform, CollisionCategory.CATEGORY_SHIP, CollisionCategory.CATEGORY_SHIP.bits)
            }
        }

        fun getMatchingComponent(id: Int) = fixtureSlotMapping[components[id]]

        val trueConnectionMap = connections.entries.associate { entry: Map.Entry<Int, List<Int>> ->
            getMatchingComponent(entry.key)!! to entry.value.map { getMatchingComponent(it)!! }.toList()
        }
        BasicPhysicsEntity(fixtureSlotMapping.values.toList(), trueConnectionMap, worldRef, scale)
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

    private val bulletFactory = EntityFactory( "bullet", {b, s ->  simpleLoader(b, physicsWorld, s)}, mutableListOf());
    private val defaultShipFactory = EntityFactory("ship_default", {b, s -> shipLoader(b, physicsWorld, null, s)}, mutableListOf());

    public class EntityFactory<T : AbstractPhysicsEntity>(val internalName: String, private val generator : (EntityBlueprint, Double) -> T, val blueprintData: MutableList<EntityBlueprint>) {
        fun generate(scale: Double) : T{
            val entity = generator.invoke(blueprintData.first(), scale)
            entity.setMass(MassType.NORMAL)
            return entity //TODO Make loading from multiple resources choices possible?
        }
    }

    //Just a list of the ship factories available, useful for testing.
    private val shipFactories = mutableListOf<(Double) -> AbstractPhysicsEntity>()

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
            blueprint.generate(1.0)
            blueprint.generate(1.0)
            blueprint.generate(1.0)
        }
        shipFactories.add ( blueprintGenerator.invoke(Blueprint.DEFAULT_SHIP)::generate )
    }



    private val physicsWorld = PhysicsWorld(blueprintGenerator)
    private var time = 0.0

    init {
        physicsWorld.setGravity(0.0, 0.0)
    }

    fun getBodyData(): Map<Int, PhysicsBodyData> {
        return physicsWorld.bodies.associate { it.uuid to it.createBodyData() }
    }

    private fun getEntity(uuid: Int): AbstractPhysicsEntity? {
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
        physicsWorld.update(input.timeStep)

        var i = physicsWorld.bodies.size
        if(controlActions.size > i){
            error("More controlActions were supplied than there are entities in the PhysicsWorld!")
        }
        while(i > 0){
            i--
            val body = physicsWorld.bodies[i]
            if(body is ShipEntity){
                val actions = controlActions.get(body.uuid)
                if(actions != null){
                    body.queueActions(actions)
                }
            }
            body.update()
            if(body.isMarkedForRemoval()){
                physicsWorld.removeBody(body)
                i--
            }
        }
        physicsWorld.processEntityBuffer()
        val effects = physicsWorld.effectsBuffer
        physicsWorld.effectsBuffer = mutableListOf()

        return PhysicsOutput(effects, physicsWorld.timeStep.deltaTime)
    }


    //TODO Can we delegate this or something
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.RenderableEntity>>) {
        physicsWorld.populateModelMap(modelDataMap)
    }

    /**
     * Process a request to add a new entity to the game. Returns an integer referring to the id of the entity if successful, otherwise null
     */
    fun requestEntity(request: EntityRequest): Int {
        val entity = when (request.type) {
            RequestType.RANDOM_SHIP ->{
                val ship = shipFactories.random().invoke(request.scale)
                ship.team = request.team
                ship.translate(request.position)
                ship.rotate(request.angle)
                addEntity(ship)
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
        val scale: Double = 1.0, val r: Float, val g: Float, val b: Float, val team: Team?
    )

    private fun <E : AbstractPhysicsEntity> addEntity(entity: E): E {
        entity.setMass(MassType.NORMAL)
        physicsWorld.addBody(entity)
        return entity
    }

    //physics DTO as layer output
    open class PhysicsBodyData(
        val uuid: Int, val position: Vector2, val velocity: Vector2,
        val angle: Double, val angularVelocity: Double, val traceRadius: Double,
        val changeInPosition: Vector2, val changeInOrientation: Double, val team: Team? = null
    )

    class PhysicsWorld(val blueprintGenerator: (Blueprint) -> EntityFactory<*>) : AbstractPhysicsWorld<BasicFixture, AbstractPhysicsEntity, WorldCollisionData<BasicFixture, AbstractPhysicsEntity>>() {

        //TODO This shouldn't be tied to physicsworld
        val graphicsService = GraphicsService()
        val entityBuffer = mutableListOf<AbstractPhysicsEntity>()
        var effectsBuffer = mutableListOf<EffectsRequest>()
        var enableCollisionProcessing = false

        override fun processCollisions(iterator: Iterator<WorldCollisionData<BasicFixture, AbstractPhysicsEntity>>) {
            if(enableCollisionProcessing){
                super.processCollisions(iterator)
                contactCollisions.forEach {
                it.pair.first.fixture.onCollide(it)
                it.pair.second.fixture.onCollide(it)
            }
            }
        }

        override fun addBody(body: AbstractPhysicsEntity) {
            super.addBody(body)
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

        override fun createCollisionData(pair: CollisionPair<CollisionItem<AbstractPhysicsEntity, BasicFixture>>?): WorldCollisionData<BasicFixture, AbstractPhysicsEntity> {
            return WorldCollisionData(pair)
        }

        fun populateModelMap(map: HashMap<Model, MutableList<Graphics.RenderableEntity>>) {
            for (body in this.bodies) {
                if (body.isEnabled) {
                    val components = body.fixtureSlotFixtureMap.entries
                    for (component in components) {
                        val renderable = graphicsService.componentToRenderable(body, component.key)
                        if(renderable != null){
                            map[component.key.model]!!.add(renderable)
                        }
                    }
                }
            }
        }
    }
}

