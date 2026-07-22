package physics

import graphics.Renderer
import math.Coordinates
import math.Vector2
import models.Model

class BlueprintStore{
    val singleHullShipBlueprint = EntityBlueprint()
    val bulletBlueprint = EntityBlueprint()
    init {

        val bulletHullBlueprint = ComponentBlueprint<EntityHull>(Model.SQUARE.asVectors(), 1.0, Vector2(), listOf(square(Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), 0.2)), { vec2, d, v1 -> EntityHull(vec2, d, v1) })

        bulletBlueprint.hullBlueprints.add(bulletHullBlueprint)

        val hullBlueprint1 = ComponentBlueprint<EntityHull>(Model.SQUARE.asVectors(), 1.0, Vector2(), listOf(square(Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), 1.0)), { vec2, d, v1 -> EntityHull(vec2, d, v1) })
        val thrusterBlueprint = ComponentBlueprint<Thruster>(Model.SQUARE.asVectors(), 1.0, Vector2(),
            listOf(square(Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), 1.0)),
            { vec2, d, v1 -> Thruster(vec2, d, v1) })
        val torquerBlueprint = ComponentBlueprint<Torquer>(Model.SQUARE.asVectors(), 1.0, Vector2(), listOf(square(Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), 1.0)),{ vec2, d, v1 -> Torquer(vec2, d, v1) })
        val weaponBlueprint = ComponentBlueprint<Weapon>(Model.SQUARE.asVectors(), 1.0, Vector2(), listOf(square(Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), 1.0)),{ vec2, d, v1 -> Weapon(vec2, d, v1) })
        val ammoDepotBlueprint = ComponentBlueprint<AmmoDepot>(Model.SQUARE.asVectors(), 1.0, Vector2(), listOf(square(Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), 1.0)),{ vec2, d, v1 -> AmmoDepot(vec2, d, v1) })
        val stationBlueprint = ComponentBlueprint<EntityStation>(Model.SQUARE.asVectors(), 1.0, Vector2(), listOf(square(Renderer.ColorData(1.0f, 1.0f, 1.0f, 1.0f), 1.0)), { vec2, d, v1 -> EntityStation(vec2, d, v1) })

        val thrusterSystemBlueprint = ThrusterSystemBlueprint(listOf(thrusterBlueprint), stationBlueprint)
        val torqueSystemBlueprint = TorqueSystemBlueprint(listOf(torquerBlueprint), stationBlueprint)
        val weaponSystemBlueprint =
            WeaponSystemBlueprint(listOf(weaponBlueprint), stationBlueprint, bulletBlueprint, listOf(ammoDepotBlueprint))

        singleHullShipBlueprint.hullBlueprints.add(hullBlueprint1)
        singleHullShipBlueprint.moduleBlueprints.add(thrusterBlueprint)
        singleHullShipBlueprint.moduleBlueprints.add(torquerBlueprint)
        singleHullShipBlueprint.moduleBlueprints.add(weaponBlueprint)
        singleHullShipBlueprint.moduleBlueprints.add(ammoDepotBlueprint)
        singleHullShipBlueprint.stationBlueprints.add(stationBlueprint)
        singleHullShipBlueprint.systemBlueprints.add(thrusterSystemBlueprint)
        singleHullShipBlueprint.systemBlueprints.add(torqueSystemBlueprint)
        singleHullShipBlueprint.systemBlueprints.add(weaponSystemBlueprint)
    }
}