import Graphics.Model
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import PhysicsLayer
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import java.util.*
import javax.naming.ldap.Control

class Transformation(val position: Vector2 = Vector2(), val scale : Double = 1.0, val rotation: Rotation = Rotation(0.0)){
    constructor(position : Vector2, scale : Double, rot : Double) : this(position, scale , Rotation(rot))
}

//Immutable please
open class TransformedComponent(val model : Model, val transform: Transformation)
class GraphicalData(val red : Float, val green : Float, val blue: Float, val z: Float, val health: Float = 1.0f) //Construction to represent the % this part is done being built
class RenderableComponent(model : Model, transform: Transformation, val graphicalData: GraphicalData) : TransformedComponent(model, transform)
class PhysicalComponentDefinition(val model : Model, val localTransform: Transformation, val graphicalData: GraphicalData)

class Team(val name : String){
    companion object{
        private var UUID_COUNTER : Int = 0
        val TEAMLESS = Team("Teamless")
    }
    val UUID = UUID_COUNTER++
}

fun main() {

    val physicsLayer = PhysicsLayer()
    val effectsLayer = EffectsLayer()
    val controllerLayer = ControllerLayer()

    val models = listOf(Model.TRIANGLE, Model.SQUARE1, Model.BACKPLATE)
    val gui = Graphics(models)
    val bitSet = BitSet(256)

    val keyListener : KeyListener = object : KeyListener {

        override fun keyPressed(e: KeyEvent?) {
            if(!e!!.isAutoRepeat){
                bitSet.set(e.keyCode.toInt(), true)
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            if(!e!!.isAutoRepeat){
                bitSet.set(e.keyCode.toInt(), false)
            }
        }
    }

    val greenTeam = Team("Green")
    val uuid = physicsLayer.requestEntity(PhysicsLayer.EntityRequest(PhysicsLayer.RequestType.SHIP, Vector2(), r=0.0f, g=1.0f, b=1.0f, team=Team("Player")))!!

    controllerLayer.addControllerEntry(PlayerController(bitSet), uuid)

    val idList = List(10) {
        physicsLayer.requestEntity(PhysicsLayer.EntityRequest(PhysicsLayer.RequestType.SHIP, Vector2(Math.random()*2).multiply(10.0), r=1.0f, g=0.0f, b=1.0f, team=greenTeam))!!
    }

//    controllerLayer.addControllerEntry(ControllerLayer.BubbleMultiController(), idList)

    val modelDataMap = hashMapOf<Model, MutableList<Pair<Transformation, GraphicalData>>>()

    //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
    val populateData = fun (details : Graphics.CameraDetails) {
        for (model in models) {
            modelDataMap[model] = mutableListOf()
        }
        //Let each world append data to the model data map
        physicsLayer.populateModelMap(modelDataMap)
        effectsLayer.populateModelMap(modelDataMap)
        controllerLayer.populateModelMap(modelDataMap)

        gui.updateDrawables(modelDataMap, details)
    }

    populateData(Graphics.CameraDetails(Vector2(), 1.0, 0.0))
    gui.setup(keyListener)

    var lastControlActions = mapOf<Int, List<ControlAction>>()

    while(true){
        Thread.sleep(16)
        val effectsRequests = physicsLayer.update(lastControlActions)
        lastControlActions = controllerLayer.update(physicsLayer.getBodyData())
        effectsLayer.update(effectsRequests)

        val playerPos = physicsLayer.getEntityData(uuid!!)?.position!!
        populateData(Graphics.CameraDetails(playerPos.copy(), 1.0, 0.0))
    }
}

interface Layer{
//    fun update()
}



