import Graphics.Model
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.MouseEvent
import com.jogamp.newt.event.MouseListener
import com.jogamp.opengl.GL
import org.dyn4j.geometry.Rotation
import org.dyn4j.geometry.Vector2
import org.dyn4j.geometry.Vector3
import java.awt.MouseInfo
import java.util.*

class Transformation(val translation: Vector3 = Vector3(), val scale : Double = 1.0, val rotation: Rotation = Rotation(0.0)){
    constructor(position : Vector3, scale : Double, rot : Double) : this(position, scale , Rotation(rot))
}

//Immutable please
//open class TransformedComponent(val model : Model, val transform: Transformation)
//class RenderableComponent(model : Model, transform: Transformation, val graphicalData: GraphicalData) : TransformedComponent(model, transform)

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
    val stream = Team::class.java.getResourceAsStream("/entities/shapes.json")
    val shapes = mapper.readValue(stream, Array<Shape>::class.java).toList()
    return shapes.associate { shape ->
        val points = shape.points.map { listOf(it.x.toFloat() / 30.0f, it.y.toFloat() / 30.0f, 0.0f) }.flatten().toFloatArray()
        shape.ID to Model(points, GL.GL_TRIANGLE_FAN)
    }
}

fun main() {

    val timeStep = 1.0;

    val entityModels = loadModels().values.toMutableList();
    val models = mutableListOf(Model.SQUARE, Model.BACKPLATE) + entityModels
    val bitSet = BitSet(256)

    val physicsLayer = PhysicsLayer(models)
    val effectsLayer = EffectsLayer()
    val controllerLayer = ControllerLayer()

    physicsLayer.loadEntities()

    //We should decouple this from clear server stuff a little better.
    val keyListener : KeyListener = object : KeyListener {

        override fun keyPressed(e: KeyEvent?) {
            if (!e!!.isAutoRepeat) {
                bitSet.set(e.keyCode.toInt(), true)
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            if (!e!!.isAutoRepeat) {
                bitSet.set(e.keyCode.toInt(), false)
            }
        }
    }

    val gui = Graphics(models, keyListener)
    val mousePositionProducer: () -> Vector2 = {gui.getMousePositionInWorldCoordinates ()}

    val testTeam = Team("test");
    //Instead, the physics layer should return some sort of 'future' like object, used later to refer to get the ID
    val playerID = physicsLayer.requestEntity(PhysicsLayer.EntityRequest(PhysicsLayer.RequestType.RANDOM_SHIP, Vector2(), r = 1.0f, g = 1.0f, b = 1.0f, team = testTeam))
    controllerLayer.addControllerEntry(PlayerController(bitSet, mousePositionProducer), playerID)
    for (i in 0..0){
        physicsLayer.requestEntity(PhysicsLayer.EntityRequest(PhysicsLayer.RequestType.RANDOM_SHIP, Vector2(10.0, 0.0).rotate((i.toFloat() / 100.0f) * Math.PI * 2), velocity = Vector2(1.0, 0.0), r = 1.0f, g = 1.0f, b = 1.0f, team = testTeam))
    }

    val modelDataMap = hashMapOf<Model, MutableList<Graphics.RenderableEntity>>()

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

    val playerData = physicsLayer.getEntityData(playerID)
    val playerPos = playerData?.position ?: Vector2()
    if(playerData == null){
        System.err.println("playerdata is null, camera will default to $playerPos")
    }
    populateData(Graphics.CameraDetails(playerPos, 1.0, 0.0))

    var lastControlActions = mapOf<Int, List<ControlCommand>>()

    while(true){
        Thread.sleep(16)
        lastControlActions = controllerLayer.update(ControllerInput(physicsLayer.getBodyData())).map
        val physicsOut = physicsLayer.update(PhysicsInput(lastControlActions, timeStep))
        val playerData = physicsLayer.getEntityData(playerID)
        populateData(Graphics.CameraDetails(playerData?.position ?: Vector2(), 1.0, 0.0))
        effectsLayer.update(EffectsInput(physicsOut.requests, physicsOut.timeElapsed))

    }
}

interface Layer<in I, out O>{
    fun update(input: I) : O
}



