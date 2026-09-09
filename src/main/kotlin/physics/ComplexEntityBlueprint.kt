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
 *
 * TODO Could use generic type here to tag blueprint to a format of ship to controllers etc... ?
 * ComplexEntityBlueprint<ComplexEntityType>
 *
 *
 * TODO TO make re-constructable ships, maybe look at entities keeping a ref to blueprint, and component->bluprint reference by ID
 *
 */
class ComplexEntityBlueprint {

    val hullBlueprints = mutableListOf<ComponentBlueprint<EntityHull>>()
    val moduleBlueprints = mutableListOf<ComponentBlueprint<EntityModule>>()
    val systemBlueprints = mutableListOf<SystemBlueprint<*>>()
    val stationBlueprints = mutableListOf<ComponentBlueprint<EntityStation>>()

    val hullToHullBlueprintMap = mutableMapOf<ComponentBlueprint<EntityHull>, List<ComponentBlueprint<EntityHull>>>()
    val hullToModuleBlueprintMap =
        mutableMapOf<ComponentBlueprint<EntityHull>, List<ComponentBlueprint<EntityModule>>>()
    val hullToStationBlueprintMap =
        mutableMapOf<ComponentBlueprint<EntityHull>, List<ComponentBlueprint<EntityStation>>>()

    fun build(): ComplexEntity {

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

        for (systemB in systemBlueprints) {
            intermediateBuild.systems[systemB] = systemB.createSystem(intermediateBuild)
        }

        //Build up an entity with the blueprints
        val entity = ComplexEntity()

        for (hull in intermediateBuild.hulls) {
            entity.addHull(hull.value)
        }

        for (module in intermediateBuild.modules) {
            entity.addModule(module.value)
        }

        for (station in intermediateBuild.stations) {
            entity.addStation(station.value)
        }

        for (system in intermediateBuild.systems) {
            entity.addSystem(system.value)
        }

        val hullToHullMap =
            hullToHullBlueprintMap.entries.associate { hull1 -> intermediateBuild.hulls[hull1.key]!! to hull1.value.map { hull2 -> intermediateBuild.hulls[hull2]!! } }
        val hullToModuleMap =
            hullToModuleBlueprintMap.entries.associate { hull -> intermediateBuild.hulls[hull.key]!! to hull.value.map { module -> intermediateBuild.modules[module]!! } }
        val hullToStationMap =
            hullToStationBlueprintMap.entries.associate { hull -> intermediateBuild.hulls[hull.key]!! to hull.value.map { station -> intermediateBuild.stations[station]!! } }

        entity.addHullToHullMap(hullToHullMap)
        entity.addHullToModuleMap(hullToModuleMap)
        entity.addHullToStationMap(hullToStationMap)

        return entity
    }
}


class IntermediateBuild() {
    val hulls = mutableMapOf<ComponentBlueprint<EntityHull>, EntityHull>()
    val modules = mutableMapOf<ComponentBlueprint<EntityModule>, EntityModule>()
    val systems = mutableMapOf<SystemBlueprint<*>, EntitySystem>()
    val stations = mutableMapOf<ComponentBlueprint<EntityStation>, EntityStation>()
}

class ComponentBlueprint<out C : Component>(
    val boundingBox: List<Vector2>,
    val mass: Double,
    centerOfMass: Vector2,
    val staticRenderables: List<IntermediaryRenderable<ComponentReferenceFrame>>,
    val componentProducer: (List<Vector2>, Double, Vector2) -> C,
    private val initialConditions: (C) -> Unit = {}
) {
    val centerOfMass = Coordinates<ComponentReferenceFrame>(centerOfMass)
    var coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    var orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    var ZHeight: ZHeight<EntityReferenceFrame> =
        ZHeight(0.0) //TODO stupid name. This is not folded into coordinates to make math less clunky, as it's solely for graphical purposes

    fun rotate(rotation: Double) {
        this.orientation += rotation
    }

    fun translate(translation: Vector2) {
        this.coordinates += translation
    }

    fun createComponent(): C {
        val component = componentProducer(boundingBox, mass, centerOfMass.getVector());
        component.rotate(orientation.getAngle())
        component.translate(coordinates.getVector())
        component.translateZ(ZHeight.getZ())
        component.addStaticRenderables(staticRenderables)
        initialConditions(component)
        return component
    }
}

abstract class SystemBlueprint<S : EntitySystem> {
    abstract fun createSystem(b: IntermediateBuild): S
    inline fun <reified C : EntityModule> mapBlueprintsToModules(
        blueprints: List<ComponentBlueprint<*>>,
        b: IntermediateBuild,
    ): List<C> {
        return blueprints.map { b.modules[it] } as List<C>
    }
}

//TODO What's with the repeat here?
class ThrusterSystemBlueprint(
    private val thrusterBlueprints: List<ComponentBlueprint<Thruster>>,
    private val pilotStationBlueprint: ComponentBlueprint<EntityStation>
) : SystemBlueprint<ThrusterSystem>() {
    override fun createSystem(b: IntermediateBuild): ThrusterSystem {
        val thrusters = mapBlueprintsToModules<Thruster>(thrusterBlueprints, b)
        val station = b.stations[pilotStationBlueprint]
        return ThrusterSystem(thrusters, station)
    }
}

class TorqueSystemBlueprint(
    private val torquerblueprints: List<ComponentBlueprint<Torquer>>,
    private val pilotStationBlueprint: ComponentBlueprint<EntityStation>
) : SystemBlueprint<TorqueSystem>() {
    override fun createSystem(b: IntermediateBuild): TorqueSystem {
        val torquers = mapBlueprintsToModules<Torquer>(torquerblueprints, b)
        val station = b.stations[pilotStationBlueprint]
        return TorqueSystem(torquers, station)
    }
}

class WeaponSystemBlueprint<W : Weapon, P : KineticEntity>(
    private var weaponblueprints: List<ComponentBlueprint<W>> = mutableListOf(),
    var weaponStationBlueprint: ComponentBlueprint<EntityStation>,
    private val generator: (EntityStation?, List<AmmoDepot>) -> WeaponSystem<W, P>,
    private val ammoDepotBlueprints: List<ComponentBlueprint<AmmoDepot>>
) : SystemBlueprint<WeaponSystem<W, P>>() {


    override fun createSystem(b: IntermediateBuild): WeaponSystem<W, P> {

        val weapons = weaponblueprints.map { b.modules[it] } as List<W>
        val ammos = ammoDepotBlueprints.map { b.modules[it] } as List<AmmoDepot>
        val weaponStation = b.stations[weaponStationBlueprint]
        val g = generator(weaponStation, ammos)
        g.weapons.addAll(weapons)

        return g
    }
}



