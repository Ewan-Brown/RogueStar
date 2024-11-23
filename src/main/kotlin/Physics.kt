import Graphics.Model
import PhysicsLayer.PhysicsEntity.*
import org.dyn4j.collision.CollisionItem
import org.dyn4j.collision.CollisionPair
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.*
import org.dyn4j.geometry.*
import org.dyn4j.world.AbstractPhysicsWorld
import org.dyn4j.world.WorldCollisionData
import java.util.Stack
import java.util.function.Predicate

private data class ComponentDefinition(val model : Model, val localTransform: Transformation, val graphicalData: GraphicalData)

data class PhysicsInput(val map : Map<Int, List<ControlAction>>)
data class PhysicsOutput(val requests: List<EffectsRequest>)

class PhysicsLayer : Layer<PhysicsInput, PhysicsOutput> {

    private enum class CollisionCategory(val bits: Long) {
        CATEGORY_SHIP(0b0001),
        CATEGORY_PROJECTILE(0b0010),
        CATEGORY_SHIELD(0b0100)
    }

    private val physicsWorld = PhysicsWorld()
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

    //TODO this comment needsa  second thought
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
        val effectsRequests = mutableListOf<EffectsRequest>()
        while(i > 0){
            i--
            val body = physicsWorld.bodies[i]
            val (eff, ent) = body.update(controlActions.getOrElse(i) { emptyList() })
            if(body.isMarkedForRemoval()){
                physicsWorld.removeBody(body)
                i--
            }
            effectsRequests.addAll(eff)
            for(entityReq in ent){
                addEntity(entityReq)
            }
        }
        return PhysicsOutput(effectsRequests)
    }

    //TODO Can we delegate this or something
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        physicsWorld.populateModelMap(modelDataMap)
    }

    /**
     * Process a request to add a new entity to the game. Returns an integer referring to the id of the entity if successful, otherwise null
     *
     * TODO Maybe this should throw an error when it fails - when could we possibly expect entity spawning to fail?
     */
    fun requestEntity(request: EntityRequest): Int? {
        val entity = when (request.type) {
            RequestType.SHIP -> {
                val details = createTestShip(request.scale, request.r, request.g, request.b, request.team);
                ShipEntity(request.team, details)
            }

            RequestType.PROJECTILE -> {
                TODO()
            }
        }
        addEntity(entity, request.angle, request.position)
        return entity.uuid
    }

    /**
     * The input DTO for this layer - represents an outer request to create an entity.
     * Generally only used at game start or when an entity is immaculately conceived.
     * Otherwise, entities should be created here in Physics via other entities controls.
     */


    public enum class RequestType {
        SHIP, PROJECTILE
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
        internalComponents: List<Component>,
        //TODO rename this to something ship-agnostic
        val cockpit: Component = internalComponents[0],
        val teamFilter: TeamFilter,
        private val connectionMap: Map<Component, List<Component>>
    ) : AbstractPhysicsBody<CustomFixture>() {

        private companion object {
            private var UUID_COUNTER = 0
        }

        val team = teamFilter.team
        val uuid = UUID_COUNTER++

        /**
         * This serves two purposes - to keep a list of all the components (fixture slots) as well as a reference to each of the components' fixtures -
         * if a reference is null it means the fixture is destroyed or removed
         */
        val componentFixtureMap: MutableMap<Component, CustomFixture?> = internalComponents.associateWith { null }.toMutableMap()

        init {
            for(value in internalComponents){
                reviveComponent(value)
            }
        }

        /**
         * Function used purely for manually testing proof of concept or debugging.
         */
        open fun testFunc(){
            println("PhysicsEntity.testFunc")
        }

        /**
         * Will remove a given component, and if not disabled, will do 'split check' which checks if this results in a fragment of the entity being lost due to lack of physical connection
         * The reason for the flag is to allow for when we are removing components *due* to an initial destruction, where we don't need to trigger this because we're already processing it.
         * */
        private fun processComponentDestruction(component: Component, trueDestruction: Boolean = true) : updateOutput{
            val entityList = mutableListOf<PhysicsEntity>()
            val effectList = mutableListOf<EffectsRequest>()
            if(!componentFixtureMap.contains(component)){
                throw IllegalArgumentException("tried to kill a component that is not under this entity")
            }else{
               if(componentFixtureMap[component] == null){
                   throw IllegalStateException("tried to kill a component that is already dead under this entity")
               }else {
//                   println("Removing")
                   removeFixture(componentFixtureMap[component])
                   componentFixtureMap[component] = null
                   //Split check!
                   if (trueDestruction) {
//                       println("Split check...")
                       val branchRoots = connectionMap[component]
                       val nodesAlreadyCounted = mutableListOf<Component>()
                       val branches: List<List<Component>> = branchRoots!!.mapNotNull {
//                           println("entering branch from root node")
                           if (componentFixtureMap[it] == null){
//                               println("this root node is dead, ignoring")
                               return@mapNotNull null
                           }
                           if (nodesAlreadyCounted.contains(it)) {
//                               println("this root node has already been counted, ignoring")
                               return@mapNotNull null
                           } else {
//                               println("exploring branch")
                               val accumulator = mutableListOf<Component>()
                               val toIgnore = component
                               val nodesToExplore = Stack<Component>()
                               nodesToExplore.push(it)
                               while (nodesToExplore.isNotEmpty()) {
//                                   println("\tthere are ${nodesToExplore.size} nodes left. Branch is ${accumulator.size} big")
                                   val node = nodesToExplore.pop()
                                   //Ignore the node that represents the broken piece that caused this split
                                   if (node != toIgnore && !accumulator.contains(node) && componentFixtureMap[node] != null) {
//                                       println("\tfound a new node to explore")
                                       accumulator.add(node)
                                       nodesToExplore.addAll(connectionMap[node]!!)
                                   } else {
//                                       println("\tignoring node")
                                   }
                               }
//                               println("\t completed this branch with ${accumulator.size} nodes found")
                               return@mapNotNull accumulator
                           }
                       }
//                       println("completed exploring all branches")
//                       println("rebuilding fragmented branches - ${branches.size - 1} branches to rebuild")
                       for (branch in branches) {
                           if (branch.contains(cockpit)) {
                               //This branch will remain a part of this entity - all other branches will be fragmented off!
                           } else {
//                               println("inspecting branch with ${branch.size} nodes")
                               //Remove fragmented branch from this entity
//                               println("\tkilling branch")
                               for (branchComponent in branch) {
                                   val (eff, ent) = processComponentDestruction(branchComponent, false)
                                   effectList.addAll(eff)
                                   entityList.addAll(ent)
                               }
//                               println("\trebuilding branch")
                               //Generate a new connection map for this new entity
                               val newConnections = mutableMapOf<Component, List<Component>>()
                               val tempComponentMap = branch.associateWith { it -> Component(it.definition, teamFilter) }
                               //Iterate across the connections of each component in this branch, creating a structural copy with the new components
                               for(connectionEntry in connectionMap.filterKeys { branch.contains(it) }) { //Iterate over each branch element
                                   newConnections[connectionEntry.key] = connectionEntry.value.filter { branch.contains(it) }.map { tempComponentMap[it]!! }
                               }

                               val newShip = ShipEntity(this.team, ShipDetails(newConnections.keys.toList(), listOf(), newConnections, newConnections.keys.toList()[0]))
                               newShip.rotate(this.transform.rotationAngle)
                               println(this.worldCenter.toString() + " " + this.localCenter.toString())
                               newShip.translate(this.worldCenter)
                               entityList.add(newShip)
                           }
                       }
                   }
               }
            }
            return updateOutput(effectList, entityList)
        }

        /**
         * Assuming this component's fixture is 'dead' this will regenerate it and add it back to the body.
         */
        fun reviveComponent(component: Component){
            if(!componentFixtureMap.contains(component)){
                throw IllegalArgumentException("tried to kill a component that is not under this entity")
            }else{
                if(componentFixtureMap[component] != null){
                    throw IllegalStateException("tried to revive a component that is already alive")
                }else{
                    val fixture = component.createFixture()
                    componentFixtureMap[component] = fixture
                    addFixture(fixture)
                }
            }
        }

        /**
         * A Component describes the "slot" for a body fixture
         * Component visibility is restricted to the Body they are a part of.
         * Components references must be valid for the lifetime of the ship they are a part of, but a fixture may or may not be alive for a given component at any point in time (as represented in the connectionMap)
         */
        open class Component(val definition: ComponentDefinition, private val filter: TeamFilter){
            fun createFixture(): CustomFixture {
                val vertices = arrayOfNulls<Vector2>(definition.model.points)
                for (i in vertices.indices) {
                    vertices[i] = definition.model.asVectorData[i].copy()
                        .multiply(definition.localTransform.scale.toDouble())
                }
                val polygon = Polygon(*vertices)
                polygon.translate(definition.localTransform.position.copy())
                polygon.rotate(definition.localTransform.rotation.toRadians())
                val fixture = CustomFixture(polygon)
                fixture.filter = filter
                return fixture
            }
            fun onDestruction() : updateOutput {return updateOutput(listOf(), listOf())
            }
        }

        class ThrusterComponent(definition: ComponentDefinition, filter:TeamFilter, val localExhaustDirection: Rotation) : Component(definition, filter)

        abstract fun isMarkedForRemoval(): Boolean

        //Check if any parts needs to be removed, and then calculate new center of mass.
        private fun updateFixtures() : updateOutput{
            var didLoseParts = false;
            val effectsList = mutableListOf<EffectsRequest>()
            val entityList = mutableListOf<PhysicsEntity>();
            for (entry in componentFixtureMap) {
                entry.value?.let{
                    if (it.isMarkedForRemoval()){
                        entry.key.onDestruction()
                        val (eff, ent) = processComponentDestruction(entry.key)
                        effectsList.addAll(eff)
                        entityList.addAll(ent)
                        didLoseParts = true
                    }
                }
            }
            if(didLoseParts) setMass(MassType.NORMAL)
            return updateOutput(effectsList, entityList)
        }

        data class updateOutput(val effects : List<EffectsRequest>, val entities: List<PhysicsEntity>){
            constructor(u1 : updateOutput, u2 : updateOutput) : this(u1.effects + u2.effects, u1.entities + u2.entities)
        }

        fun update(actions: List<ControlAction>) : updateOutput{
            val out1 = updateFixtures();
            val out2 = processControlActions(actions);
            return updateOutput(out1, out2)
        }

        abstract fun processControlActions(actions: List<ControlAction>): updateOutput

        fun getRenderableComponents(): List<RenderableComponent> {
            if (!isEnabled) {
                return listOf()
            } else {
                val result: MutableList<RenderableComponent> = ArrayList()

                val entityAngle = getTransform().rotationAngle.toFloat()
                val entityPos = this.worldCenter

                for (comp in componentFixtureMap.entries){
                    val renderable = transformLocalRenderableToGlobal(comp.key)
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
        fun transformLocalRenderableToGlobal(component: Component) : RenderableComponent?{
            val fixture = componentFixtureMap[component]
            if(fixture != null){
                val renderable = RenderableComponent(component.definition.model, component.definition.localTransform, component.definition.graphicalData)
                val entityAngle = getTransform().rotation.toRadians()
                val entityPos = this.worldCenter
                val newPos = renderable.transform.position.copy().subtract(this.getMass().center)
                    .rotate(entityAngle.toDouble()).add(entityPos)
                val newAngle =
                    Rotation(renderable.transform.rotation.toRadians() + this.transform.rotationAngle)
                val scale = renderable.transform.scale

                //TODO We should probably have two different types -
                //  'renderable data that is static and attached to fixture'
                //  and 'class that represents ephemeral data describing a fixture's rendering'
                return RenderableComponent(
                    renderable.model,
                    Transformation(newPos, scale, newAngle),
                    GraphicalData(
                        renderable.graphicalData.red,
                        renderable.graphicalData.green,
                        renderable.graphicalData.blue,
                        renderable.graphicalData.z,
                        fixture.getHealth().toFloat() / 100.0f
                    )
                )
            }
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

    private data class ShipDetails(val components: List<Component>, val thrusters: List<Component>, val connectionMap: Map<Component, List<Component>>, val cockpit: Component)

    companion object{
        private fun createTestShip(scale: Double, red: Float, green: Float, blue: Float, team: Team) : ShipDetails {
            val body = mutableListOf<Component>()
            val thrusters = mutableListOf<ThrusterComponent>()
            val cockpit = Component(
                    ComponentDefinition(
                        Model.TRIANGLE,
                        Transformation(Vector2(), scale),
                        GraphicalData(red, green, blue, 0.0f)),
                    TeamFilter(team, {it.UUID != team.UUID},
                        category = CollisionCategory.CATEGORY_SHIP.bits,
                        mask = CollisionCategory.CATEGORY_SHIP.bits))
            val center1 = ThrusterComponent(
                ComponentDefinition(
                    Model.SQUARE1,
                    Transformation(Vector2(-1.5, 0.0), scale),
                    GraphicalData(red, green/2.0f, blue, 0.0f)),
                TeamFilter(team, {it.UUID != team.UUID},
                    category = CollisionCategory.CATEGORY_SHIP.bits,
                    mask = CollisionCategory.CATEGORY_SHIP.bits),
                Rotation()
            )
            val center2 = ThrusterComponent(
                ComponentDefinition(
                    Model.SQUARE1,
                    Transformation(Vector2(-2.5, 0.0), scale),
                    GraphicalData(red, green/2.0f, blue, 0.0f)),
                TeamFilter(team, {it.UUID != team.UUID},
                    category = CollisionCategory.CATEGORY_SHIP.bits,
                    mask = CollisionCategory.CATEGORY_SHIP.bits),
                Rotation()
            )
            val thruster = ThrusterComponent(
                ComponentDefinition(
                    Model.SQUARE1,
                    Transformation(Vector2(-3.5, 0.0), scale),
                    GraphicalData(red, green/2.0f, blue/2.0f, 0.0f)),
                TeamFilter(team, {it.UUID != team.UUID},
                    category = CollisionCategory.CATEGORY_SHIP.bits,
                    mask = CollisionCategory.CATEGORY_SHIP.bits),
                Rotation()
            )
            thrusters.add(thruster)
            body.add(cockpit)
            body.add(center1)
            body.add(center2)
            body.add(thruster)
            val connectionMap = mapOf(cockpit to listOf(center1),
                center1 to listOf(cockpit, center2),
                center2 to listOf(center1, thruster),
                thruster to listOf(center2))
            return ShipDetails(body, thrusters, connectionMap, cockpit)
        }
    }

    private open class ShipEntity(team: Team, val shipDetails: ShipDetails) : PhysicsEntity(
        shipDetails.components, shipDetails.cockpit, TeamFilter(
            team = team, teamPredicate = { it != team }, category = CollisionCategory.CATEGORY_SHIP.bits,
            mask = CollisionCategory.CATEGORY_SHIP.bits or CollisionCategory.CATEGORY_PROJECTILE.bits
        ), shipDetails.connectionMap
    ) {

        val thrusterComponents = shipDetails.thrusters
        init {
//            if(thrusterComponents.isEmpty()){
//                throw Exception("Attempted to create a ship with no thruster components! Not acceptable right now.")
//            }
        }
        override fun isMarkedForRemoval(): Boolean = false

        var flag = false
        override fun testFunc(){
            super.testFunc()
            if(!flag) {
//                println("ShipEntity.testFunc - removing the middle bit")
                componentFixtureMap.entries.stream().skip(2).findFirst().ifPresent {
                    it.value?.kill()
                }
            }else{
                componentFixtureMap.entries.stream().filter{it -> it.value == null}.forEach{reviveComponent(it.key)}
                setMass(MassType.NORMAL)
            }
            flag = !flag
        }

        override fun processControlActions(actions: List<ControlAction>): updateOutput {
            val effectsList = mutableListOf<EffectsRequest>()
            for (action in actions) {
                when(action){
                    is ControlAction.ShootAction -> TODO()
                    is ControlAction.ThrustAction -> {
                        val thrusterCount = thrusterComponents.count { return@count componentFixtureMap[it] != null }
                        applyForce(action.thrust.product(thrusterCount.toDouble() / thrusterComponents.size.toDouble()))
                        for (thrusterComponent in thrusterComponents) {
                            transformLocalRenderableToGlobal(thrusterComponent)?.transform?.let{
                                effectsList.add(EffectsRequest.ExhaustRequest(it.position, it.rotation.toRadians(), Vector2()))
                            }
                        }
                    }
                    is ControlAction.TurnAction -> {
                        applyTorque(action.torque)
                    }
                    is ControlAction.TestAction -> {
                        testFunc()
                    }
                }
            }
            return updateOutput(effectsList, listOf())
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
    private class PhysicsWorld : AbstractPhysicsWorld<CustomFixture, PhysicsEntity, WorldCollisionData<CustomFixture, PhysicsEntity>>() {

        override fun processCollisions(iterator: Iterator<WorldCollisionData<CustomFixture, PhysicsEntity>>) {
            super.processCollisions(iterator)
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
                for (component in body.getRenderableComponents()) {
                    map[component.model]!!.add(Pair(component.transform, component.graphicalData))
                }
            }
        }
    }

    /**
     * A bit-wise (and optional predicate) filter to determine if a given Fixture should intersect with one from another entity
     */
    class TeamFilter(
        val team: Team = Team.TEAMLESS,
        val teamPredicate: Predicate<Team> = Predicate { true },
        val category: Long,
        val mask: Long
    ) : Filter {
        override fun isAllowed(filter: Filter): Boolean {
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
    /**
     * Needed a custom extension of this to easily react for fixture<->fixture collision events. Dyn4J is focused on Body<->Body collisions - this just makes life easier for me.
     */
    private class CustomFixture(shape: Convex): BodyFixture(shape) {
        private var health = 100;
        fun onCollide(data: WorldCollisionData<CustomFixture, PhysicsEntity>) {
            health -= 10;
        }

        /**
         * Just for testing at the moment
         */
        fun kill(){health = 0}
        fun getHealth(): Int = health
        fun isMarkedForRemoval(): Boolean = health <= 0
    }
}

