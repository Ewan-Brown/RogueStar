import Graphics.Model
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import java.util.*

class Transformation(val position: Vector2 = Vector2(), val scale : Double = 1.0, val rotation: Rotation = Rotation(0.0)){
    constructor(position : Vector2, scale : Double, rot : Double) : this(position, scale , Rotation(rot))
}

open class TransformedComponent(val model : Model, val transform: Transformation)
class GraphicalData(val red : Float, val green : Float, val blue: Float, val z: Float, val health: Float = 1.0f) //Construction to represent the % this part is done being built
class RenderableComponent(model : Model, transform: Transformation, val graphicalData: GraphicalData) : TransformedComponent(model, transform)
class PhysicalComponentDefinition(val model : Model, val localTransform: Transformation, val graphicalData: GraphicalData)

class Team(val name : String){
    companion object{
        private var UUID_COUNTER : Long = 0
        val TEAMLESS = Team("Teamless")
    }
    val UUID = UUID_COUNTER++
}

val physicsLayer = PhysicsLayer()
val effectsLayer = EffectsLayer()
val controllerLayer = ControllerLayer()

fun main() {

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

    val playerEntity = physicsLayer.addEntity(ShipEntity(1.0, 1.0f, 0.0f, 0.0f, Team("Player")), 0.0, Vector2())
    controllerLayer.addControlledEntity(playerEntity, PlayerController(bitSet))

    val greenEntities = mutableListOf<PhysicsEntity>()
    val blueEntities = mutableListOf<PhysicsEntity>()
    for(i in 1..0){
        greenEntities.add(physicsLayer.addEntity(ShipEntity(1.0, 0.0f, 1.0f, 0.0f, greenTeam), 0.0, Vector2(Math.random()-0.5, Math.random() - 0.5).multiply(30.0)))

    }

    controllerLayer.addControlledEntityGroup(greenEntities, controllerLayer.BubbleMultiController(
        { playerEntity.worldCenter},
        20.0, Vector2(1.0,0.0)))

    val modelDataMap = hashMapOf<Model, MutableList<Pair<Transformation, GraphicalData>>>()

    //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
    val populateData = fun () {
        for (model in models) {
            modelDataMap[model] = mutableListOf()
        }

        //Let each world append data to the model data map
        physicsLayer.populateModelMap(modelDataMap)
        effectsLayer.populateModelMap(modelDataMap)
        controllerLayer.populateModelMap(modelDataMap)

        //New entities created from controllers, like projectiles or summoned ships
        val controllerEntityRequestList = controllerLayer.getNewEntityRequests()

        gui.updateDrawables(modelDataMap, playerEntity.worldCenter.copy())
    }

    populateData()
    gui.setup(keyListener)

    while(true){
        Thread.sleep(15)

        physicsLayer.update()
        effectsLayer.update()
        controllerLayer.update()

        populateData()
    }
}

interface Layer{
    fun update()
}



