package physics

import math.Coordinates
import math.Vector2
import models.Model


fun createBullet() : Entity {
    val bullet = Entity()
    bullet.addHull(EntityHull(Model.SQUARE.asVectors(), 1.0, Vector2()))
    bullet.applyForce(Force(Vector2(1.0, 0.0), Coordinates(Vector2())))
    bullet
    return bullet
}

class BlueprintStore{
    val basicEntityBlueprint = EntityBlueprint()

    init {
        val hullBlueprint1 = HullBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2())

        val thrusterBlueprint = ModuleBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2(), { vec2, d, v1 -> Thruster(vec2, d, v1) })
        val torquerBlueprint = ModuleBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2(), { vec2, d, v1 -> Torquer(vec2, d, v1) })
        val weaponBlueprint = ModuleBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2(), { vec2, d, v1 -> Weapon(vec2, d, v1) })
        val ammoDepotBlueprint = ModuleBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2(), { vec2, d, v1 -> AmmoDepot(vec2, d, v1) })

        val stationBlueprint = StationBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2())

        val thrusterSystemBlueprint = ThrusterSystemBlueprint(listOf(thrusterBlueprint), stationBlueprint)
        val torqueSystemBlueprint = TorqueSystemBlueprint(listOf(torquerBlueprint), stationBlueprint)
        val weaponSystemBlueprint =
            WeaponSystemBlueprint(listOf(weaponBlueprint), stationBlueprint, { createBullet() }, listOf(ammoDepotBlueprint))

        basicEntityBlueprint.hullBlueprints.add(hullBlueprint1)
        basicEntityBlueprint.moduleBlueprints.add(thrusterBlueprint)
        basicEntityBlueprint.moduleBlueprints.add(torquerBlueprint)
        basicEntityBlueprint.moduleBlueprints.add(weaponBlueprint)
        basicEntityBlueprint.moduleBlueprints.add(ammoDepotBlueprint)
        basicEntityBlueprint.stationBlueprints.add(stationBlueprint)
        basicEntityBlueprint.systemBlueprints.add(thrusterSystemBlueprint)
        basicEntityBlueprint.systemBlueprints.add(torqueSystemBlueprint)
        basicEntityBlueprint.systemBlueprints.add(weaponSystemBlueprint)
    }
}