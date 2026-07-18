package physics

import math.Vector2
import models.Model
import kotlin.test.Test

class EntityFactoryTests {

    @Test
    fun singleHullEntityTest(){
        val blueprint = EntityBlueprint()
        val hullBlueprint1 = HullBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2())
        blueprint.hullBlueprints.add(hullBlueprint1)

        val entity = blueprint.build()

        assert(entity.getHull().size == 1)
    }

    @Test
    fun entityBlueprintTest(){
        val blueprint = EntityBlueprint()

        val hullBlueprint1 = HullBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2())

        val thrusterProducer: (List<Vector2>, Double, Vector2) -> Thruster = {vec, d1, v2 ->
            Thruster(vec, d1, v2)
        }
        val moduleBlueprint1 = ModuleBlueprint<Thruster>(Model.SQUARE.asVectors(), 1.0, Vector2(), thrusterProducer)

        val stationBlueprint = StationBlueprint(Model.SQUARE.asVectors(), 1.0, Vector2())

        val systemBlueprint = ThrusterSystemBlueprint(listOf(moduleBlueprint1), stationBlueprint)

        blueprint.hullBlueprints.add(hullBlueprint1)
        blueprint.moduleBlueprints.add(moduleBlueprint1)
        blueprint.stationBlueprints.add(stationBlueprint)
        blueprint.systemBlueprints.add(systemBlueprint)

        val entity = blueprint.build()

        assert(entity.getHull().size == 1)
        assert(entity.getModules().size == 1)
        assert(entity.getSystems().size == 1)

        assert(entity.getChildren().size == 2)
    }

}