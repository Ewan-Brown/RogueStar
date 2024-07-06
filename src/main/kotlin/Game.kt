import Graphics.Model
import Graphics.Transform


/**
 * Test
 */

fun main() {

    val models = listOf(Model.TRIANGLE, Model.SQUARE1, Model.SQUARE2)

    val gui = Graphics(models)

    val physicsLayer = PhysicsLayer()
    val effectsLayer = EffectsLayer()

    val modelDataMap = hashMapOf<Model, MutableList<Transform>>()

    //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
    val populateData = fun () : Unit{
        for (model in models) {
            modelDataMap[model] = mutableListOf()
        }

        //Reset model data map
        for (model in models) {
            modelDataMap[model]!!.clear()
        }

        //Let each world append data to the model data map
        physicsLayer.populateModelMap(modelDataMap)
        effectsLayer.populateModelMap(modelDataMap)
        gui.updateDrawables(modelDataMap)
    }


    populateData()
    gui.setup()

    while(true){
        Thread.sleep(15)

        physicsLayer.update()
        effectsLayer.update()

        populateData()
    }
}

interface Layer{
    fun update()
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>)
}

data class Component(val model: Model, val transform: Transform)


