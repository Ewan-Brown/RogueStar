import Graphics.Model
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import lombok.Getter
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import org.dyn4j.world.WorldCollisionData
import java.util.*

class Transformation(public val position: Vector2, val scale : Double, val rotation: Rotation){
    constructor(position : Vector2, scale : Double, rot : Double) : this(position, scale , Rotation(rot))
}
open class TransformedComponent(val model : Model, val transform: Transformation)
class GraphicalData(val red : Float, val green : Float, val blue: Float, val z: Float)
class RenderableComponent(model : Model, transform: Transformation, val graphicalData: GraphicalData) : TransformedComponent(model, transform)
class ComponentDefinition(val model : Model, val localTransform: Transformation, val graphicalData: GraphicalData, val category: Long, val mask: Long)


val physicsLayer = PhysicsLayer()
val effectsLayer = EffectsLayer()
val controllerLayer = ControllerLayer()

fun main() {

    val models = listOf(Model.TRIANGLE, Model.SQUARE1, Model.SQUARE2, Model.BACKPLATE)

    val gui = Graphics(models)

    val bitSet = BitSet(256);

    val keyListener : KeyListener = object : KeyListener {

        override fun keyPressed(e: KeyEvent?) {
            if(!e!!.isAutoRepeat){
                bitSet.set(e.keyCode.toInt(), true);
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            if(!e!!.isAutoRepeat){
                bitSet.set(e.keyCode.toInt(), false);
            }
        }
    }

    val shipEntity = physicsLayer.addEntity(DumbEntity(), 0.0, Vector2())
    controllerLayer.addControlledEntity(shipEntity, PlayerController(bitSet))

    val entities = mutableListOf<PhysicsEntity>()
    for(i in 1..10){
        entities.add(physicsLayer.addEntity(DumbEntity(), 0.0, Vector2(Math.random()-0.5, Math.random() - 0.5).multiply(30.0)))
    }
    controllerLayer.addMultiControlledEntities(entities, ControllerLayer.EncircleMultiController())

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
        val controllerEntityRequestList = controllerLayer.getNewEntityRequests();

        //Graphical entities created by physics logic, like from collisions or destructions

//        cameraTargetPos = shipEntity.worldCenter
        gui.updateDrawables(modelDataMap, shipEntity.worldCenter.copy())
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

//data class Component(val model: Model, val transform: Transform)


