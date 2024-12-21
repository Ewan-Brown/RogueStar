import Graphics.Model
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind.type.TypeFactory
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.opengl.GL
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import java.awt.Color
import java.io.File
import java.util.*

class Transformation(val position: Vector2 = Vector2(), val scale : Double = 1.0, val rotation: Rotation = Rotation(0.0)){
    constructor(position : Vector2, scale : Double, rot : Double) : this(position, scale , Rotation(rot))
}

//Immutable please
open class TransformedComponent(val model : Model, val transform: Transformation)
class GraphicalData(val red : Float, val green : Float, val blue: Float, val z: Float, val health: Float = 1.0f) //Construction to represent the % this part is done being built
class RenderableComponent(model : Model, transform: Transformation, val graphicalData: GraphicalData) : TransformedComponent(model, transform)

class Team(val name : String){
    companion object{
        private var UUID_COUNTER : Int = 0
        val TEAMLESS = Team("Teamless")
    }
}

fun loadModels() : Map<Int, Model> {
    val mapper = ObjectMapper()
    val module = SimpleModule()
    module.addSerializer(Vector2::class.java, VectorSerializer())
    module.addDeserializer(Vector2::class.java, VectorDeserializer())
    mapper.registerModules(module)
    val shapes = mapper.readValue(File("shapes.json"), Array<Shape>::class.java).toList()
    return shapes.associate { shape ->
        val points = shape.points.map { listOf(it.x.toFloat(), it.y.toFloat()) }.flatten().toFloatArray()
        shape.ID to Model(points, GL.GL_TRIANGLE_FAN)
    }
}

fun main() {

    val physicsLayer = PhysicsLayer()
    val effectsLayer = EffectsLayer()
    val controllerLayer = ControllerLayer()

    val entityModels = loadModels().values.toMutableList();
    val models = mutableListOf(Model.SQUARE, Model.BACKPLATE) + entityModels

    physicsLayer.loadShips(entityModels)

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

    val uuid = null
//    val greenTeam = Team("Green")
//    val uuid = physicsLayer.requestEntity(PhysicsLayer.EntityRequest(PhysicsLayer.RequestType.SHIP, Vector2(), r=0.0f, g=1.0f, b=1.0f, team=Team("Player")))!!
//    val uuid = physicsLayer.requestEntity(PhysicsLayer.EntityRequest(PhysicsLayer.RequestType.SHIP, Vector2(), r=0.0f, g=1.0f, b=1.0f, team=Team("Player")))!!

//    controllerLayer.addControllerEntry(PlayerController(bitSet), uuid)

//    val idList = MutableList(10) {
//        physicsLayer.requestEntity(PhysicsLayer.EntityRequest(PhysicsLayer.RequestType.SHIP, Vector2(Math.random()*Math.PI*2).multiply(20.0), r=1.0f, g=1.0f, b=1.0f, team=greenTeam))!!
//    }
//    controllerLayer.addControllerEntry(ControllerLayer.BubbleMultiController(uuid, 20.0), idList)

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
        val effectsRequests = physicsLayer.update(PhysicsInput(lastControlActions)).requests
        lastControlActions = controllerLayer.update(ControllerInput(physicsLayer.getBodyData())).map
        effectsLayer.update(EffectsInput(effectsRequests))

        val playerPos = if(uuid == null){
            Vector2()
        }else{
            physicsLayer.getEntityData(uuid!!)?.position!!
        }
        populateData(Graphics.CameraDetails(playerPos.copy(), 1.0, 0.0))

    }
}

interface Layer<in I, out O>{
    fun update(input: I) : O
}



