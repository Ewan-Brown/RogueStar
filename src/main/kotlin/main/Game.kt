package main

import models.Model
import effects.EffectsManager
import graphics.Renderer
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import controllers.ControllerManager
import java.util.*
import controllers.PlayerController
import controllers.SimpleNPCController
import graphics.CameraDetails
import graphics.DebugLineData
import graphics.RendererI
import graphics.loadModels
import physics.*
import physics.EntityBlueprintStore.singleHullShipBlueprint

fun main() {
    val timeStep = 1.0;

    val entityModels = loadModels().values.toMutableList();
    val models = mutableListOf(Model.SQUARE, Model.BACKPLATE)
    models.addAll(entityModels)

    val effectsManager = EffectsManager()
    val controllerManager = ControllerManager()
    val physicsLayer = PhysicsManager()
    val renderer : RendererI = Renderer(models)

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

    renderer.addListener(keyListener)
    val game = Game(models, physicsLayer, controllerManager, effectsManager, renderer)

    val playerEntity = singleHullShipBlueprint.build()
    val nonPlayerEntity = singleHullShipBlueprint.build()

    val playerController = PlayerController(bitSet)
    val npcController = SimpleNPCController()

    controllerManager.addControllerEntry(playerController, playerEntity)
    controllerManager.addControllerEntry(npcController, nonPlayerEntity)

    physicsLayer.addEntity(playerEntity)
    physicsLayer.addEntity(nonPlayerEntity)

    while(true){
        game.targetEntity = playerEntity
        game.update(timeStep)
    }

}

class Game(val models: MutableList<Model>, val physicsManager: PhysicsManager, val controllerManager: ControllerManager, val effectsManager: EffectsManager, val gui: RendererI){

    var targetEntity: ComplexEntity? = null

    fun populateData() {
        val modelDataMap = hashMapOf<Model, MutableList<Renderer.Renderable>>()
        for (model in models) {
            modelDataMap[model] = mutableListOf()
        }
        //Let each world append data to the model data map
        physicsManager.populateModelMap(modelDataMap)
        effectsManager.populateModelMap(modelDataMap)
        controllerManager.populateModelMap(modelDataMap)

        val debugData = mutableListOf<DebugLineData>()

        //Enable when necessary :)
        debugData.addAll(physicsManager.getDebugLines())
        debugData.addAll(controllerManager.getDebugLines())

        if(targetEntity != null) {
            gui.updateCamera(CameraDetails(targetEntity!!.getCoordinates().getVector(), 1.0, 0.0))
        }
        gui.updateDrawables(modelDataMap)
        gui.updateDebug(debugData)
    }

    fun update(timeStep : Double){
        //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
        populateData()
        Thread.sleep(16)
        val pOut = physicsManager.update(timeStep)
        effectsManager.update(timeStep)
        controllerManager.update()
    }
}



