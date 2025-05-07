import Graphics.*
import PhysicsLayer.*
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Convex
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.WorldCollisionData
import java.util.*
import java.util.function.Predicate

/**************************
 * Game-session Entity related code
 *************************/

abstract class PhysicsEntity protected constructor(
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
    var flag = false;
    open fun testFunc(){
        if(!flag){
            processComponentDestruction(fixtureSlotFixtureMap.keys.toList()[1])
            flag = true;
        }
//            val bullet = worldReference.blueprintGenerator(Blueprint.BULLET).generate()
//            for (entry in fixtureSlotFixtureMap.entries) {
//                entry.key
//            }
//            bullet.translate(-bullet.worldCenter.x, -bullet.worldCenter.y)
//            worldReference.entityBuffer.add(bullet);
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
                            newEntity.setLinearVelocity(this.linearVelocity.x, this.linearVelocity.y)
                            newEntity.angularVelocity = this.angularVelocity.toDouble() // TODO does this copy value instead of reference... ?
                            newEntity.setMass(MassType.NORMAL)
                            this.setMass(MassType.NORMAL)
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

    fun update(actions: List<ControlCommand>){
        updateFixtures();
        processControlActions(actions);
    }

    abstract fun processControlActions(actions: List<ControlCommand>)

    fun getRenderableComponents(): List<RenderableEntity> {
        if (!isEnabled) {
            return listOf()
        } else {
            val result: MutableList<RenderableEntity> = ArrayList()

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
    fun transformLocalRenderableToGlobal(graphicsService: GraphicsService, fixtureSlot: FixtureSlot<*>) : RenderableEntity?{
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

    override fun processControlActions(actions: List<ControlCommand>) {
        for (action in actions) {
            when(action){
                is ControlCommand.ShootCommand -> TODO()
                is ControlCommand.ThrustCommand -> {
                    val thrusterCount = thrusterComponents.count { return@count fixtureSlotFixtureMap[it] != null }
//                        applyForce(action.thrust.product(thrusterCount.toDouble() / thrusterComponents.size.toDouble()))
                    applyForce(action.desiredVelocity.product(thrusterCount.toDouble() / thrusterComponents.size.toDouble()), worldCenter)
                    for (thrusterComponent in thrusterComponents) {
                        transformLocalRenderableToGlobal(worldReference.graphicsService, thrusterComponent)?.transform?.let{
                            worldReference.effectsBuffer.add(EffectsRequest.ExhaustRequest(it.translation, it.rotation.toRadians(), Vector2()))
                        }
                    }
                }
                is ControlCommand.TurnCommand -> {
//                        applyTorque(action.torque * this.getMass().mass)
                }
                is ControlCommand.TestCommand -> {
                    testFunc()
                }
            }
        }
    }
}

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

/**************************
 * Filter related code
 *************************/

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
        filter
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

/**************************
 * Misc code
 *************************/

public data class ShipDetails(val fixtureSlots: List<FixtureSlot<*>>, val thrusters: List<ThrusterFixtureSlot>, val connectionMap: Map<FixtureSlot<*>, List<FixtureSlot<*>>>, val cockpit: FixtureSlot<*>)
public enum class CollisionCategory(val bits: Long) {
    CATEGORY_SHIP(0b0001),
    CATEGORY_PROJECTILE(0b0010),
    CATEGORY_SHIELD(0b0100)
}