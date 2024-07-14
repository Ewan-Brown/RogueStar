import Graphics.Model
import Graphics.Transform
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import org.dyn4j.geometry.Vector2
import java.util.*
import kotlin.collections.HashMap


/**
 * Test
 */

fun main() {

    val models = listOf(Model.TRIANGLE, Model.SQUARE1, Model.SQUARE2, Model.BACKPLATE)

    val gui = Graphics(models)

    val physicsLayer = PhysicsLayer()
    val effectsLayer = EffectsLayer()

    val playerEntity = physicsLayer.addEntity(listOf(Component(Model.SQUARE1, Transform(Vector2(0.0,0.0), 0f))), 0.0, Vector2(0.0, 0.0))
    val otherEntity = physicsLayer.addEntity(listOf(Component(Model.SQUARE1, Transform(Vector2(0.0,0.0), 0f))), 0.0, Vector2(0.0, 0.0))

    otherEntity.translate(Vector2(1.0, 0.0))

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
    val bitSet = BitSet(256);

    val keyListener : KeyListener = object : KeyListener {

        override fun keyPressed(e: KeyEvent?) {
            bitSet.set(e!!.keyCode.toInt(), true);
        }

        override fun keyReleased(e: KeyEvent?) {
            bitSet.set(e!!.keyCode.toInt(), false);
        }
    }

    populateData()
    gui.setup(keyListener)

    while(true){
        Thread.sleep(15)

//        val entity = physicsLayer.addEntity(listOf(Component(Model.SQUARE1, Transform(Vector2(Math.random()*3, Math.random()*3), 0f))), 0.0, Vector2(0.0, 0.0))
        physicsLayer.update()
        effectsLayer.update()

        var x = 0.0;
        var y = 0.0;


        if(bitSet[KeyEvent.VK_W.toInt()]){
            y++
        }
        if(bitSet[KeyEvent.VK_S.toInt()]){
            y--
        }
        if(bitSet[KeyEvent.VK_A.toInt()]){
            x--
        }
        if(bitSet[KeyEvent.VK_D.toInt()]){
            x++
        }

        playerEntity.applyForce(Vector2(x,y))

        populateData()
    }
}

//TODO Maybe these two should be the same call, to avoid looping over things doubly?
interface Layer{
    fun update()
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>)
}

data class Component(val model: Model, val transform: Transform)


