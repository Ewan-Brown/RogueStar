package main

import models.Model
import effects.EffectsManager
import graphics.Renderer
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.MouseEvent
import com.jogamp.newt.event.MouseListener
import controllers.ControllerManager
import java.util.*
import controllers.PlayerController
import controllers.SimpleNPCController
import graphics.CameraDetails
import graphics.DebugCircleData
import graphics.DebugLineData
import graphics.RendererI
import graphics.loadModels
import math.Vector2
import physics.*
import physics.EntityBlueprintStore.singleHullShipBlueprint

fun main() {
    val timeStep = 1.0;
    val pauseButton = KeyEvent.VK_ESCAPE

    val entityModels = loadModels().values.toMutableList();
    val models = mutableListOf(Model.SQUARE, Model.LASER, Model.BACKPLATE)
    models.addAll(entityModels)

    val game: Game

    val effectsManager = EffectsManager()
    val controllerManager = ControllerManager()
    val physicsLayer = PhysicsManager()
    val renderer : RendererI = Renderer(models)

    game = Game(models, physicsLayer, controllerManager, effectsManager, renderer)

    physicsLayer.onDebugPause = {game.paused = it} //Set pause game on debug pause!

    //TODO We should decouple the player controls from general controls (e.g pause)
    val bitSet = BitSet(256)
    val keyListener : KeyListener = object : KeyListener {
        override fun keyPressed(e: KeyEvent?) {
            if (!e!!.isAutoRepeat) {
                bitSet.set(e.keyCode.toInt(), true)
            }
            if(e.keyCode == pauseButton){
                game.paused = !game.paused
            }
        }

        override fun keyReleased(e: KeyEvent?) {
            if (!e!!.isAutoRepeat) {
                bitSet.set(e.keyCode.toInt(), false)
            }
        }
    }

    val mouseListener : MouseListener = object : MouseListener {

        override fun mouseClicked(e: MouseEvent?) {
            val p = renderer.getMousePositionInWorldCoordinates()
            println("point : ${p}")
            val entities = physicsLayer.getEntitiesAt(p)
            println("entities found : ${entities.size}")
            for(e in entities){
                println("\t $e")
            }
            println()
        }

        override fun mouseEntered(e: MouseEvent?) {

        }

        override fun mouseExited(e: MouseEvent?) {

        }

        override fun mousePressed(e: MouseEvent?) {

        }

        override fun mouseReleased(e: MouseEvent?) {

        }

        override fun mouseMoved(e: MouseEvent?) {

        }

        override fun mouseDragged(e: MouseEvent?) {

        }

        override fun mouseWheelMoved(e: MouseEvent?) {

        }
    }

    renderer.addKeyListener(keyListener)
    renderer.addMouseListener(mouseListener)

    val playerEntity = singleHullShipBlueprint.build()
    val nonPlayerEntity = singleHullShipBlueprint.build()
    nonPlayerEntity.translate(Vector2(10.0, 0.0))

    val playerController = PlayerController(bitSet)
    val npcController = SimpleNPCController()

    controllerManager.addControllerEntry(playerController, playerEntity)
//    controllerManager.addControllerEntry(npcController, nonPlayerEntity)

    physicsLayer.addEntity(playerEntity)
    physicsLayer.addEntity(nonPlayerEntity)

    while(true){
        game.targetEntity = playerEntity
        game.update(timeStep)
    }

}

class Game(val models: MutableList<Model>, val physicsManager: PhysicsManager, val controllerManager: ControllerManager, val effectsManager: EffectsManager, val gui: RendererI){

    var paused = false;
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

        val debugLines = mutableListOf<DebugLineData>()
        val debugCircles = mutableListOf<DebugCircleData>()

        //Enable when necessary :)
        debugLines.addAll(physicsManager.getDebugLines())
        debugLines.addAll(controllerManager.getDebugLines())

        debugCircles.addAll(physicsManager.getDebugCircles())
        debugCircles.addAll(controllerManager.getDebugCircles())

        if(targetEntity != null) {
            gui.updateCamera(CameraDetails(targetEntity!!.getCoordinates().getVector(), 1.0, 0.0))
        }
        gui.updateDrawables(modelDataMap)
        gui.updateDebug(debugLines, debugCircles)
    }

    fun update(timeStep : Double){
        //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
        populateData()
        Thread.sleep(16)
        if(!paused){
            physicsManager.update(timeStep)
            effectsManager.update(timeStep)
            controllerManager.update()
        }
    }
}



