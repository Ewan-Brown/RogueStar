import Graphics.*
import PhysicsLayer.*
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.dynamics.BodyFixture
import org.dyn4j.geometry.Convex
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Polygon
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.WorldCollisionData
import java.util.*
import java.util.function.Predicate

/**************************
 * Game-session Entity related code
 *************************/

abstract class PhysicsEntity protected constructor(
    internalFixtureSlots: List<FixtureSlot<*>>,
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
    open fun testFunc(){}

    /**
     * Will remove a given component, and if not disabled, will do 'split check' which checks if this results in a fragment of the entity being lost due to lack of physical connection
     * The reason for the flag is to allow for when we are removing components *due* to an initial destruction, where we don't need to trigger this because we're already processing it.
     * */
    private fun processComponentDestruction(fixtureSlot: FixtureSlot<*>, initialDestruction: Boolean = true){
        // The concept of a root here just made thinking about / visualizing it easier. maybe not necessary
        val root = fixtureSlotFixtureMap.keys.first()
        if(!fixtureSlotFixtureMap.contains(fixtureSlot)){
            throw IllegalArgumentException("tried to kill a component that is not under this entity")
        }else{
            if(fixtureSlotFixtureMap[fixtureSlot] == null){
                throw IllegalStateException("tried to kill a component that is already dead under this entity - $fixtureSlot")
            }else {
                removeFixture(fixtureSlotFixtureMap[fixtureSlot])
                fixtureSlotFixtureMap[fixtureSlot] = null
                //Split check!
                if (initialDestruction) {
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

                            val thrusters = newConnections.keys.filterIsInstance<ThrusterFixtureSlot>();
                            val guns = newConnections.keys.filterIsInstance<GunFixtureSlot>();
                            val cockpits = newConnections.keys.filterIsInstance<CockpitFixtureSlot>();

                            //TODO shouldn't this be a 'DebrisEntity' or something? Should it always really be a ship?
                            val newEntity = ShipEntity(this.team, ShipDetails(newConnections.keys.toList(), newConnections
                            ),worldReference )
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

    fun getTransformation(): Transformation {
        return Transformation(worldCenter.toVec3(), 1.0, getTransform().rotationAngle)
    }
}
open class ShipEntity(team: Team, shipDetails: ShipDetails, worldReference: PhysicsWorld) : PhysicsEntity(
    shipDetails.fixtureSlots, TeamFilter(
        team = team, doesCollide = { it != team }, category = CollisionCategory.CATEGORY_SHIP.bits,
        mask = CollisionCategory.CATEGORY_SHIP.bits or CollisionCategory.CATEGORY_PROJECTILE.bits
    ), shipDetails.connectionMap, worldReference
) {

    val thrusterComponents = shipDetails.connectionMap.keys.filterIsInstance<ThrusterFixtureSlot>()
    val gunComponents = shipDetails.connectionMap.keys.filterIsInstance<GunFixtureSlot>()
    val cockpits = shipDetails.connectionMap.keys.filterIsInstance<CockpitFixtureSlot>()

    override fun isMarkedForRemoval(): Boolean = false

    override fun testFunc(){
        super.testFunc()
    }

    fun shootAllWeapons(){
        gunComponents.filter { fixtureSlotFixtureMap[it] != null }.forEach {
            //TODO Make this a reusable function :) maybe even as a generic Transformation util
            val shipTransform = getTransformation()
            val slotTransform = getFixtureSlotTransform(this, it)

            val translation = shipTransform.translation + slotTransform.translation.toVec2().rotate(shipTransform.rotation)
            val rotation = shipTransform.rotation.toRadians() + slotTransform.rotation.toRadians()

            val finalTransform = Transformation(translation, rotation, 1.0)
//            worldReference.requestEntity(EntityRequest(RequestType.BULLET))
            //TODO Guns/weapons need a way to keep a reference to what they create/shoot.
            // Note that this may be dynamically created entities, and may be based off something a bit more static (e.g guns that shoot bullets)
            // Maybe it's safe for guns to hold onto and require reference to their respective bullets' factories. that would solve alot of problems.
        }
    }

    override fun processControlActions(actions: List<ControlCommand>) {
        for (action in actions) {
            when(action){
                is ControlCommand.ShootCommand -> shootAllWeapons()
                is ControlCommand.ThrustCommand -> {
                    if(!action.desiredVelocity.isZero) {
                        val force = action.desiredVelocity.product(100.0).rotate(getTransform().rotationAngle)
                        applyForce(force, worldCenter)
                        for (thrusterComponent in thrusterComponents) {
                            val transform = getFixtureSlotTransform(this, thrusterComponent)
                            worldReference.effectsBuffer.add(
                                EffectsRequest.ExhaustRequest(
                                    transform.translation,
                                    transform.rotation.toRadians(),
                                    this.linearVelocity - force.normalized * 10.0
                                )
                            )
                        }
                    }
                }
                is ControlCommand.TurnCommand -> {
                    //TODO Why does this have to be negativified
                    val angle = Vector2(-this.transform.rotationAngle).getAngleBetween(Vector2(action.desiredAngle))
                    applyTorque(-angle*120 - angularVelocity*60)
                }
                is ControlCommand.TestCommand -> {
                    testFunc()
                }
            }
        }
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

fun getFixtureSlotTransform(entity: PhysicsEntity, fixtureSlot: FixtureSlot<*>) : Transformation{
    val entityAngle = entity.getTransform().rotation.toRadians()
    val entityPos = entity.worldCenter
    val absolutePos = fixtureSlot.localTransform.translation.toVec2().subtract(entity.getMass().center).rotate(entityAngle).add(entityPos)
    val newAngle = Rotation(fixtureSlot.localTransform.rotation.toRadians() + entity.transform.rotationAngle)
    val scale = fixtureSlot.localTransform.scale
    return Transformation(absolutePos.toVec3(), scale, newAngle)
}

/**************************
 * Misc code
 *************************/

public data class ShipDetails(val fixtureSlots: List<FixtureSlot<*>>,
                              val connectionMap: Map<FixtureSlot<*>,
                                      List<FixtureSlot<*>>>)
public enum class CollisionCategory(val bits: Long) {
    CATEGORY_SHIP(0b0001),
    CATEGORY_PROJECTILE(0b0010),
    CATEGORY_SHIELD(0b0100)
}