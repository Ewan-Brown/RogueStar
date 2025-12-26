package main

import EffectsLayerI
import PhysicsLayerI
import models.Model
import codec.VectorDeserializer
import codec.VectorSerializer
import effects.EffectsInput
import effects.EffectsLayer
import graphics.Graphics
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.opengl.GL
import controllers.ControllerLayer
import designers.Shape
import math.Transformation3
import math.Vector2
import math.Vector3
import java.util.*
import ControllerLayerI
import DebugLineData
import controllers.Controller
import controllers.PlayerController
import graphics.CameraDetails
import graphics.GREEN
import graphics.GraphicsI
import graphics.WHITE
import physics.*

fun loadModels() : Map<Int, Model> {
    val mapper = ObjectMapper()
    val module = SimpleModule()
    module.addSerializer(Vector2::class.java, VectorSerializer())
    module.addDeserializer(Vector2::class.java, VectorDeserializer())
    mapper.registerModules(module)
    val stream = Graphics::class.java.getResourceAsStream("/entities/shapes.json")
    val shapes = mapper.readValue(stream, Array<Shape>::class.java).toList()
    return shapes.associate { shape ->
        val points = shape.points.map { listOf(it.getX().toFloat() / 30.0f, it.getY().toFloat() / 30.0f, 0.0f) }.flatten().toFloatArray()
        shape.ID to Model(points, GL.GL_TRIANGLE_FAN)
    }
}

fun main() {
    val timeStep = 1.0;

    val entityModels = loadModels().values.toMutableList();
    val models = mutableListOf(Model.SQUARE, Model.BACKPLATE)
    models.addAll(entityModels)

    val physics = PhysicsLayer()

    val effectsLayer: EffectsLayerI = EffectsLayer()
    val controllerLayer: ControllerLayerI = ControllerLayer()
    val physicsLayer: PhysicsLayerI = physics
    val gui : GraphicsI = Graphics(models)

    //We should decouple this from clear server stuff a little better.
    val bitSet = BitSet(256)
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

    gui.addListener(keyListener)
    val game = Game(models, physicsLayer, controllerLayer, effectsLayer, gui)

    val playerEntity = ControllableEntity()
    playerEntity.setEffectsConsumer(effectsLayer)
    playerEntity.setEntityConsumer(physicsLayer)
    physicsLayer.addEntity(playerEntity)
    val playerController : Controller<ControllableEntity> = PlayerController(bitSet)
    controllerLayer.addControllerEntry(playerController, playerEntity)

    while(true){
        game.update(timeStep)
    }

}

class Game(val models: MutableList<Model>, val physicsLayer: PhysicsLayerI, val controllerLayer: ControllerLayerI, val effectsLayer: EffectsLayerI, val gui: GraphicsI){

    fun update(timeStep : Double){
        val modelDataMap = hashMapOf<Model, MutableList<Graphics.Renderable>>()

        //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
        val populateData = fun (details : CameraDetails) {
            for (model in models) {
                modelDataMap[model] = mutableListOf()
            }
            //Let each world append data to the model data map
            physicsLayer.populateModelMap(modelDataMap)
            effectsLayer.populateModelMap(modelDataMap)
            controllerLayer.populateModelMap(modelDataMap)

            val debugData = mutableListOf<DebugLineData>()

            //Enable when necessary :)
//            debugData.addAll(physicsLayer.getDebugLines())
//            debugData.addAll(effectsLayer.getDebugLines())
//            debugData.addAll(controllerLayer.getDebugLines())

            gui.updateDrawables(modelDataMap)
            gui.updateCamera(details)
            gui.updateDebug(debugData)
        }

        populateData(CameraDetails(Vector2(0.0) , 1.0, 0.0))

        while(true){
            Thread.sleep(16)
            val pOut = physicsLayer.update(PhysicsInput(timeStep))
            for(p in pOut.effects){
                effectsLayer.addEffect(p)
            }
            effectsLayer.update(EffectsInput(timeStep))
            controllerLayer.update()
            populateData(CameraDetails(Vector2(0.0) , 1.0, 0.0))
        }
    }
}



