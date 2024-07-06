import Graphics.Model
import Graphics.Transform
import org.dyn4j.geometry.MassType
import org.dyn4j.geometry.Vector2


/**
 * Test
 */

fun main() {

    val models = listOf(Model.TRIANGLE, Model.SQUARE1, Model.SQUARE2)

    val gui = Graphics(models)

    val physicsLayer = PhysicsLayer()
    val effectsLayer = EffectsLayer()

    gui.setup()

    val modelDataMap = hashMapOf<Model, MutableList<Graphics.Transform>>()
    for (model in models) {
        modelDataMap[model] = mutableListOf()
    }

    while(true){
        Thread.sleep(15)

        physicsLayer.update()
        effectsLayer.update()

        //Reset model data map
        for (model in models) {
            modelDataMap[model]!!.clear()
        }

        //Let each world append data to the model data map
        physicsLayer.populateModelMap(modelDataMap)
        effectsLayer.populateModelMap(modelDataMap)

        //Pass the model data map to UI for drawing
        gui.updateDrawables(modelDataMap)
    }
}

interface Layer{
    fun update()
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>)
}

data class Component(val model: Model, val transform: Graphics.Transform)


