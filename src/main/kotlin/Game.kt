import Graphics.Model
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Vector2


/**
 * Test
 */
fun main() {

    val models = listOf(Model.TRIANGLE, Model.SQUARE1)

    val gui = Graphics(models)
    val physicsWorld = PhysicsWorld();

    physicsWorld.setGravity(0.0,0.0)

    val testPhysicsEntity1 = PhysicsEntity(listOf(Component(Model.SQUARE1, Graphics.Transform(Vector2(0.0,0.0), 0.0f))))
    testPhysicsEntity1.setMass(MassType.NORMAL)
    physicsWorld.addBody(testPhysicsEntity1)

    val testPhysicsEntity2 = PhysicsEntity(listOf(Component(Model.SQUARE1, Graphics.Transform(Vector2(0.0,0.0), 0.0f))))
    testPhysicsEntity2.setMass(MassType.NORMAL)
    physicsWorld.addBody(testPhysicsEntity2)

    val effectsWorld = EffectsWorld();

    val testEntity = SimpleEffectsEntity(Vector2(0.0,0.0), model = Model.TRIANGLE)
    effectsWorld.addEntity(testEntity)


    gui.setup()

    val modelDataMap = hashMapOf<Model, MutableList<Graphics.Transform>>()
    for (model in models) {
        modelDataMap[model] = mutableListOf()
    }

    while(true){
        Thread.sleep(15)

        testEntity.position = testEntity.position.add(0.01,0.0)
        testEntity.angle += 0.01f;
        testPhysicsEntity1.applyForce(Vector2(1.0,1.0))

        physicsWorld.update(1.0)

        //Reset model data map
        for (model in models) {
            modelDataMap[model]!!.clear()
        }

        //Let each world append data to the model data map
        physicsWorld.populateModelMap(modelDataMap)
        effectsWorld.populateModelMap(modelDataMap)

        //Pass the model data map to UI for drawing
        gui.updateDrawables(modelDataMap)
    }
}

data class Component(val model: Model, val transform: Graphics.Transform)


