package physics

import graphics.Renderer.IntermediaryRenderable
import math.ComponentReferenceFrame
import math.Coordinates
import math.EntityReferenceFrame
import math.Orientation
import math.Vector2
import math.ZHeight
import kotlin.collections.iterator

/**
 * Stores the "blueprint" info required to build a particular type of entity
 *
 * https://gameprogrammingpatterns.com/type-object.html
 */
class EntityBlueprint {

    val hullBlueprints = mutableListOf<ComponentBlueprint<EntityHull>>()
    val moduleBlueprints = mutableListOf<ComponentBlueprint<EntityModule>>()
    val systemBlueprints = mutableListOf<SystemBlueprint<*>>()
    val stationBlueprints = mutableListOf<ComponentBlueprint<EntityStation>>()

    val hullToHullBlueprintMap = mutableMapOf<ComponentBlueprint<EntityHull>, List<ComponentBlueprint<EntityHull>>>()
    val hullToModuleBlueprintMap = mutableMapOf<ComponentBlueprint<EntityHull>, List<ComponentBlueprint<EntityModule>>>()
    val hullToStationBlueprintMap = mutableMapOf<ComponentBlueprint<EntityHull>, List<ComponentBlueprint<EntityStation>>>()

    fun build(): ShipEntity {

        val intermediateBuild = IntermediateBuild()

        for (hullB in hullBlueprints) {
            intermediateBuild.hulls[hullB] = hullB.createComponent()
        }

        for (moduleB in moduleBlueprints) {
            intermediateBuild.modules[moduleB] = moduleB.createComponent()
        }

        for (stationB in stationBlueprints) {
            intermediateBuild.stations[stationB] = stationB.createComponent()
        }

        for (systemB in systemBlueprints){
            intermediateBuild.systems[systemB] = systemB.createSystem(intermediateBuild)
        }

        //Build up an entity with the blueprints
        val entity = ShipEntity()

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

        val hullToHullMap = hullToHullBlueprintMap.entries.associate {hull1 -> intermediateBuild.hulls[hull1.key]!! to hull1.value.map { hull2 -> intermediateBuild.hulls[hull2]!! }}
        val hullToModuleMap = hullToModuleBlueprintMap.entries.associate {hull -> intermediateBuild.hulls[hull.key]!! to hull.value.map { module -> intermediateBuild.modules[module]!! }}
        val hullToStationMap = hullToStationBlueprintMap.entries.associate {hull -> intermediateBuild.hulls[hull.key]!! to hull.value.map { station -> intermediateBuild.stations[station]!! }}

        entity.addHullToHullMap(hullToHullMap)
        entity.addHullToModuleMap(hullToModuleMap)
        entity.addHullToStationMap(hullToStationMap)

        return entity
    }
}

class IntermediateBuild(){
    val hulls = mutableMapOf<ComponentBlueprint<EntityHull>, EntityHull>()
    val modules = mutableMapOf<ComponentBlueprint<EntityModule>, EntityModule>()
    val systems = mutableMapOf<SystemBlueprint<*>, EntitySystem>()
    val stations = mutableMapOf<ComponentBlueprint<EntityStation>, EntityStation>()
}

class ComponentBlueprint<out C: Component>(val boundingBox: List<Vector2>, val mass: Double, centerOfMass: Vector2, val staticRenderables: List<IntermediaryRenderable<ComponentReferenceFrame>>, val componentProducer: (List<Vector2>, Double, Vector2) -> C, private val initialConditions: (C) -> Unit = {}){
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

    fun createComponent() : C {
        val component = componentProducer(boundingBox, mass, centerOfMass.getVector());
        component.rotate(orientation.getAngle())
        component.translate(coordinates.getVector())
        component.translateZ(ZHeight.getZ())
        component.addStaticRenderables(staticRenderables)
        initialConditions(component)
        return component
    }
}

interface SystemBlueprint<S: EntitySystem>{
    fun createSystem(b: IntermediateBuild) : S
}

class ThrusterSystemBlueprint(private val thrusterBlueprints: List<ComponentBlueprint<Thruster>>, private val pilotStationBlueprint: ComponentBlueprint<EntityStation>) : SystemBlueprint<ThrusterSystem>{
    override fun createSystem(b : IntermediateBuild): ThrusterSystem {
        val thrusters = thrusterBlueprints.map {b.modules[it]} as List<Thruster>
        val station = b.stations[pilotStationBlueprint]
        return ThrusterSystem(thrusters, station)
    }
}

class TorqueSystemBlueprint(private val torquerblueprints: List<ComponentBlueprint<Torquer>>, private val pilotStationBlueprint: ComponentBlueprint<EntityStation>) : SystemBlueprint<TorqueSystem>{
    override fun createSystem(b : IntermediateBuild): TorqueSystem {
        val torquers = torquerblueprints.map {b.modules[it]} as List<Torquer>
        val station = b.stations[pilotStationBlueprint]
        return TorqueSystem(torquers, station)
    }
}

class WeaponSystemBlueprint(private val weaponblueprints: List<ComponentBlueprint<Weapon>>, private val weaponstationBlueprint: ComponentBlueprint<EntityStation>, private val projectileCreator: EntityBlueprint ,private val ammoDepotBlueprints: List<ComponentBlueprint<AmmoDepot>>) : SystemBlueprint<WeaponSystem>{
    override fun createSystem(b : IntermediateBuild): WeaponSystem {
        val weapons = weaponblueprints.map {b.modules[it]} as List<Weapon>
        val ammos = ammoDepotBlueprints.map { b.modules[it] } as List<AmmoDepot>
        val weaponStation = b.stations[weaponstationBlueprint]
        return WeaponSystem(weapons, {
            val e = projectileCreator.build()
            e.applyForce(Force(Vector2(0.1, 0.0), Coordinates(Vector2(0.0,0.0))))
            e
                                     }, weaponStation, ammos)
    }
}



