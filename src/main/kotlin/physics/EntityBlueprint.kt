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

        for (station in intermediateBuild.stations){
            entity.addStation(station.value)
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

abstract class ComponentBlueprint(val boundingBox: List<Vector2>, val mass: Double, centerOfMass: Vector2){
    val centerOfMass = Coordinates<ComponentReferenceFrame>(centerOfMass)
    var coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    var orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    var ZHeight: ZHeight<EntityReferenceFrame> = ZHeight(0.0) //TODO stupid name. This is not folded into coordinates to make math less clunky, as it's solely for graphical purposes

    fun rotate(rotation: Double) {
        this.orientation += rotation
    }

    fun translate(translation: Vector2) {
        this.coordinates += translation
    }
}

class HullBlueprint(boundingBox: List<Vector2>, mass: Double, centerOfMass: Vector2) : ComponentBlueprint(boundingBox, mass, centerOfMass){
    fun createHull() : EntityHull{
        val hull = EntityHull(boundingBox, mass, centerOfMass.getVector());
        hull.rotate(orientation.getAngle())
        hull.translate(coordinates.getVector())
        hull.translateZ(ZHeight.getZ())
        return hull
    }
}

class ModuleBlueprint<M: EntityModule>(boundingBox: List<Vector2>, mass: Double, centerOfMass: Vector2, val moduleProducer: (List<Vector2>, Double, Vector2) -> M) : ComponentBlueprint(boundingBox, mass, centerOfMass){
    fun createModule() : M{
        val module = moduleProducer(boundingBox, mass, centerOfMass.getVector());
        module.rotate(orientation.getAngle())
        module.translate(coordinates.getVector())
        module.translateZ(ZHeight.getZ())
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
        val weaponStation = b.stations[weaponstationBlueprint]
        return WeaponSystem(weapons, projectileCreator, weaponStation, ammos)
    }
}

class StationBlueprint(boundingBox: List<Vector2>, mass: Double, centerOfMass: Vector2) : ComponentBlueprint(boundingBox, mass, centerOfMass){
    fun createStation() : EntityStation{
        val station = EntityStation(boundingBox, mass, centerOfMass.getVector());
        station.rotate(orientation.getAngle())
        station.translate(coordinates.getVector())
        station.translateZ(ZHeight.getZ())
        return station
    }
}



