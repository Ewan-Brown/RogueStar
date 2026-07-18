package physics

import math.ComponentReferenceFrame
import math.Coordinates
import math.EntityReferenceFrame
import math.Orientation
import math.Vector2
import math.ZHeight

/**
 * Stores the "blueprint" info required to build a particular type of entity
 *
 * https://gameprogrammingpatterns.com/type-object.html
 */
class EntityBlueprint {

    val hullBlueprints = mutableListOf<HullBlueprint>()
    val moduleBlueprints = mutableListOf<ModuleBlueprint<*>>()
    val systemBlueprints = mutableListOf<SystemBlueprint<*>>()
    val stationBlueprints = mutableListOf<StationBlueprint>()

    val hullToHullBlueprintMap = mutableMapOf<HullBlueprint, HullBlueprint>()
    val hullToModuleBlueprintMap = mutableMapOf<HullBlueprint, List<ModuleBlueprint<*>>>()
    val hullToStationBlueprint = mutableMapOf<HullBlueprint, List<StationBlueprint>>()

    fun build(): Entity {

        val intermediateBuild = IntermediateBuild()

        for (hullB in hullBlueprints) {
            intermediateBuild.hulls[hullB] = hullB.createHull()
        }

        for (moduleB in moduleBlueprints) {
            intermediateBuild.modules[moduleB] = moduleB.createModule()
        }

        for (stationB in stationBlueprints) {
            intermediateBuild.stations[stationB] = stationB.createStation()
        }

        for (systemB in systemBlueprints){
            intermediateBuild.systems[systemB] = systemB.createSystem(intermediateBuild)
        }

        //Build up an entity with the blueprints
        val entity = Entity()

        for (hull in intermediateBuild.hulls) {
            entity.addHull(hull.value)
        }

        for (module in intermediateBuild.modules) {
            entity.addModule(module.value)
        }

        for (system in intermediateBuild.systems) {
            entity.addSystem(system.value)
        }

        return entity
    }
}

class IntermediateBuild(){
    val hulls = mutableMapOf<HullBlueprint, EntityHull>()
    val modules = mutableMapOf<ModuleBlueprint<*>, EntityModule>()
    val systems = mutableMapOf<SystemBlueprint<*>, EntitySystem>()
    val stations = mutableMapOf<StationBlueprint, EntityStation>()
}

abstract class ComponentBlueprint(val boundary: List<Vector2>, val mass: Double, centerOfMass: Vector2){
    val centerOfMass = Coordinates<ComponentReferenceFrame>(centerOfMass)
    private var coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private var ZHeight: ZHeight<EntityReferenceFrame> = ZHeight(0.0)

    fun rotate(rotation: Double) {
        this.orientation += rotation
    }

    fun translate(translation: Vector2) {
        this.coordinates += translation
    }
}

class HullBlueprint(boundary: List<Vector2>, mass: Double, centerOfMass: Vector2) : ComponentBlueprint(boundary, mass, centerOfMass){
    fun createHull() : EntityHull{
        val hull = EntityHull(boundary, mass, centerOfMass.getVector());
        //TODO apply transform to hull
        return hull
    }
}

class ModuleBlueprint<M: EntityModule>(val boundary: List<Vector2>, val mass: Double, val centerOfMass: Vector2, val moduleProducer: () -> M) {
    fun createModule() : M{
        val module = moduleProducer();
        //TODO apply transform to module
        return module;
    }
}

interface SystemBlueprint<S: EntitySystem>{
    fun createSystem(b: IntermediateBuild) : S
}

class ThrusterSystemBlueprint(private val thrusterBlueprints: List<ModuleBlueprint<Thruster>>, private val pilotStationBlueprint: StationBlueprint) : SystemBlueprint<ThrusterSystem>{
    override fun createSystem(b : IntermediateBuild): ThrusterSystem {
        val thrusters = thrusterBlueprints.map {b.modules[it]} as List<Thruster>
        val station = b.stations[pilotStationBlueprint]
        return ThrusterSystem(thrusters, station)
    }
}

class TorqueSystemBlueprint(private val torquerblueprints: List<ModuleBlueprint<Torquer>>, private val pilotStationBlueprint: StationBlueprint) : SystemBlueprint<TorqueSystem>{
    override fun createSystem(b : IntermediateBuild): TorqueSystem {
        val torquers = torquerblueprints.map {b.modules[it]} as List<Torquer>
        val station = b.stations[pilotStationBlueprint]
        return TorqueSystem(torquers, station)
    }
}

class WeaponSystemBlueprint(private val weaponblueprints: List<ModuleBlueprint<Weapon>>, private val weaponstationBlueprint: StationBlueprint, private val projectileCreator: () -> Entity ,private val ammoDepotBlueprints: List<ModuleBlueprint<AmmoDepot>>) : SystemBlueprint<WeaponSystem>{
    override fun createSystem(b : IntermediateBuild): WeaponSystem {
        val weapons = weaponblueprints.map {b.modules[it]} as List<Weapon>
        val ammos = ammoDepotBlueprints.map { b.modules[it] } as List<AmmoDepot>
        val weaponstation = b.stations[weaponstationBlueprint]
        return WeaponSystem(weapons, projectileCreator, weaponstation, ammos)
    }
}

class StationBlueprint(){
    fun createStation() : EntityStation{
        val station = EntityStation();
        //TODO apply transform to hull return station
        return station
    }
}



