import BasicFixtureSlot.*
import PhysicsLayer.*
import RifleFixtureSlot.RifleFixture
import org.dyn4j.collision.Filter
import org.dyn4j.dynamics.AbstractPhysicsBody
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import java.util.*

/**************************
 * Game-session Entity related code
 *************************/

abstract class AbstractPhysicsEntity(
    internalFixtureSlots: List<AbstractFixtureSlot<*>>,
    private val connectionMap: Map<AbstractFixtureSlot<*>, List<AbstractFixtureSlot<*>>>,
    val worldReference: PhysicsWorld, val scale: Double
) : AbstractPhysicsBody<BasicFixture>() {

    private companion object {
        private var UUID_COUNTER = 0
    }

    var team: Team? = null
    val uuid = UUID_COUNTER++

    /**
     * This serves two purposes - to keep a list of all the components (fixture slots) as well as a reference to each of the components' fixtures -
     * if a reference is null it means the fixture is destroyed or removed
     */
    val fixtureSlotFixtureMap: MutableMap<AbstractFixtureSlot<*>, BasicFixture?> = internalFixtureSlots.associateWith { null }.toMutableMap()

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
    private fun processComponentDestruction(fixtureSlot: AbstractFixtureSlot<*>, initialDestruction: Boolean = true){
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
                    val nodesAlreadyCounted = mutableListOf<AbstractFixtureSlot<*>>()
                    val branches: List<List<AbstractFixtureSlot<*>>> = branchRoots!!.mapNotNull {
                        if (fixtureSlotFixtureMap[it] == null){
                            return@mapNotNull null
                        }
                        if (nodesAlreadyCounted.contains(it)) {
                            return@mapNotNull null
                        } else {
                            val accumulator = mutableListOf<AbstractFixtureSlot<*>>()
                            val toIgnore = fixtureSlot
                            val nodesToExplore = Stack<AbstractFixtureSlot<*>>()
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
                            val newConnections = mutableMapOf<AbstractFixtureSlot<*>, List<AbstractFixtureSlot<*>>>()
                            val tempComponentMap = branch.associateWith { it -> copyFixtureSlot(it)}
//                               Iterate across the connections of each component in this branch, creating a structural copy with the new components
                            for(connectionEntry in connectionMap.filterKeys { branch.contains(it) }) { //Iterate over each branch element
                                newConnections[connectionEntry.key] = connectionEntry.value.filter { branch.contains(it) }.map { tempComponentMap[it]!! }
                            }

                            //TODO shouldn't this be a 'DebrisEntity' or something? Should it always really be a ship?
                            val newEntity = ShipEntity(ShipDetails(newConnections.keys.toList(), newConnections,
                                this.team),worldReference, scale)
                            newEntity.translate(this.localCenter.flip()) //TODO ROGUE-9
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
    private fun reviveComponent(fixtureSlot: AbstractFixtureSlot<*>){
        if(!fixtureSlotFixtureMap.contains(fixtureSlot)){
            throw IllegalArgumentException("tried to kill a component that is not under this entity")
        }else{
            if(fixtureSlotFixtureMap[fixtureSlot] != null){
                throw IllegalStateException("tried to revive a component that is already alive")
            }else{
                val fixture = fixtureSlot.generateFixture {team}
                fixtureSlotFixtureMap[fixtureSlot] = fixture
                addFixture(fixture)
            }
        }
    }


    abstract fun isMarkedForRemoval(): Boolean

    //Check if any parts needs to be removed, and then calculate new center of mass.
    private fun updateFixtures(){
        var didLoseParts = false;
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

    open fun update(){
        updateFixtures();
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

    fun getTransformation(): Transformation {
        return Transformation(worldCenter.toVec3(), scale, getTransform().rotationAngle)
    }
}

class BasicPhysicsEntity(fixtureSlots: List<AbstractFixtureSlot<*>>, connectionMap: Map<AbstractFixtureSlot<*>,
        List<AbstractFixtureSlot<*>>>, worldReference: PhysicsWorld, scale: Double) : AbstractPhysicsEntity(fixtureSlots, connectionMap, worldReference, scale) {
    override fun isMarkedForRemoval(): Boolean {
        return false
    }
}

open class ShipEntity(shipDetails: ShipDetails, worldReference: PhysicsWorld, scale: Double) : AbstractPhysicsEntity(
    shipDetails.fixtureSlots, shipDetails.connectionMap, worldReference, scale
) {

    init {
        team = shipDetails.team
    }

    // FIXME Note that these must be explicitly typed or 'noinfer' gets attached which is irritating
    //TODO It seems that these might have changed how things work when ship is split in pieces. See bullet test case for eample
    val thrusterComponents: List<ThrusterFixtureSlot> = shipDetails.connectionMap.keys.filterIsInstance<ThrusterFixtureSlot>()
    val gunComponents: List<RifleFixtureSlot> = shipDetails.connectionMap.keys.filterIsInstance<RifleFixtureSlot>()
    val cockpits: List<CockpitFixtureSlot> = shipDetails.connectionMap.keys.filterIsInstance<CockpitFixtureSlot>()

    private val actions: MutableList<ControlCommand> = mutableListOf()

    override fun isMarkedForRemoval(): Boolean = false

    override fun testFunc(){
        this.fixtureSlotFixtureMap.forEach{
            if(it.key is ThrusterFixtureSlot ){
                fixtureSlotFixtureMap[it.key]?.kill()
            }
        }
        super.testFunc()
    }

    fun shootAllWeapons(){
        gunComponents.filter { fixtureSlotFixtureMap[it] != null }.forEach {
            val fixtureSlotGlobalTransform = getFixtureSlotGlobalTransform(this, it)
            val (projectile, transform) = it.generateProjectile(fixtureSlotFixtureMap[it] as RifleFixture)

            //TODO This needs to be done to get rotations working... make this more globally applicable somehow
            projectile.translate(projectile.localCenter.flip()) //TODO ROGUE-9

            projectile.rotate(transform.rotation)
            projectile.translate(transform.translation.toVec2())

            projectile.rotate(fixtureSlotGlobalTransform.rotation)
            projectile.translate(fixtureSlotGlobalTransform.translation.toVec2())

            worldReference.entityBuffer.add(projectile)
        }
    }

    fun queueActions(actions: List<ControlCommand>){
        this.actions.addAll(actions)
    }

    override fun update() {
        super.update()
        processControlActions(actions)
        actions.clear()
    }

    private fun processControlActions(actions: List<ControlCommand>) {
        for (action in actions) {
            when(action){
                is ControlCommand.ShootCommand -> shootAllWeapons()
                is ControlCommand.ThrustCommand -> {
                    if(!action.desiredVelocity.isZero) {
                        val force = action.desiredVelocity.product(100.0).rotate(getTransform().rotationAngle)
                        for (thrusterComponent in thrusterComponents.filter { fixtureSlotFixtureMap[it] != null }) {
                            val transform = getFixtureSlotGlobalTransform(this, thrusterComponent)
                            applyForce(force, transform.translation.toVec2())
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
    var teamProducer: () -> Team?,
    val category: Long,
    val mask: Long
) : Filter {
    override fun isAllowed(filter: Filter): Boolean {
        if (filter is TeamFilter) {
            //Check that the category/mask matches both directions
            //AND if team logic matches
            return (this.category and filter.mask) > 0 && (filter.category and this.mask) > 0 && (filter.teamProducer() == null || teamProducer() == null) || (filter.teamProducer() != this.teamProducer())
        } else {
            return filter.isAllowed(this)
        }
    }
}

fun getFixtureSlotGlobalTransform(entity: AbstractPhysicsEntity, fixtureSlot: AbstractFixtureSlot<*>) : Transformation{
    val entityAngle = entity.transform.rotation.toRadians()
    val entityPos = entity.worldCenter
    val localPos = fixtureSlot.localTransform.translation.toVec2().subtract(entity.mass.center).rotate(entityAngle)
    val absolutePos = entityPos + localPos
    val newAngle = Rotation(fixtureSlot.localTransform.rotation.toRadians() + entity.transform.rotationAngle)
    val scale = fixtureSlot.localTransform.scale
    return Transformation(absolutePos.toVec3(), scale, newAngle)
}

fun getFixtureSlotLocalTransform(entity: AbstractPhysicsEntity, fixtureSlot: AbstractFixtureSlot<*>) : Transformation{
    val localPos = fixtureSlot.localTransform.translation.toVec2().subtract(entity.mass.center)
    val newAngle = Rotation(fixtureSlot.localTransform.rotation.toRadians())
    val scale = fixtureSlot.localTransform.scale
    return Transformation(localPos.toVec3(), scale, newAngle)
}

/**************************
 * Misc code
 *************************/

public data class ShipDetails(val fixtureSlots: List<AbstractFixtureSlot<*>>,
                              val connectionMap: Map<AbstractFixtureSlot<*>, List<AbstractFixtureSlot<*>>>,
                              val team: Team?)
public enum class CollisionCategory(val bits: Long) {
    CATEGORY_SHIP(0b0001),
    CATEGORY_PROJECTILE(0b0010),
    CATEGORY_SHIELD(0b0100)
}