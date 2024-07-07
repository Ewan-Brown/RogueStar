import Graphics.Model
import Graphics.Transform
import org.dyn4j.geometry.Vector2


/**
 * Test
 */

fun main() {

    val models = listOf(Model.TRIANGLE, Model.SQUARE1, Model.SQUARE2)

    val gui = Graphics(models)

    val physicsLayer = PhysicsLayer()
    val effectsLayer = EffectsLayer()

//    physicsLayer.addEntity(listOf(Component(Model.SQUARE1, Transform(Vector2(Math.random()*2,Math.random()*2), Math.random().toFloat()))), 0.0, Vector2(0.0, 0.0))
//    effectsLayer.addEntity(FleeingEffectEntity(Vector2(), model = Model.TRIANGLE))
    val modelDataMap = hashMapOf<Model, MutableList<Transform>>()

    //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
    val populateData = fun () {
        for (model in models) {
            modelDataMap[model] = mutableListOf()
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

//TODO Maybe these two should be the same call, to avoid looping over things doubly?
interface Layer{
    fun update()
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>)
}

data class Component(val model: Model, val transform: Transform)


