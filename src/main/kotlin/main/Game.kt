package main

import models.Model
import effects.EffectsManager
import graphics.Renderer
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import controllers.ControllerManager
import math.Vector2
import java.util.*
import controllers.PlayerController
import graphics.CameraDetails
import graphics.DebugLineData
import graphics.RendererI
import graphics.loadModels
import math.Coordinates
import physics.*

@JvmInline
value class Timestamp(val time: Double){
    operator fun minus(t2 : Timestamp) : TimeDuration{
        return TimeDuration(time - t2.time)
    }
}
@JvmInline
value class TimeDuration(val duration: Double)

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

    val playerEntity = Entity()

    val hull = DummyHull()
    val thruster = Thruster()
    thruster.translate(Vector2(0.0, 1.0))
    val torquer = Torquer()
    torquer.translate(Vector2(0.0, 2.0))

    val gun = Weapon()
    gun.translate(Vector2(0.0, 1.0))

    fun createBullet() : Entity {
        val bullet = Entity()
        bullet.addHull(DummyHull())
        bullet.applyForce(Force(Vector2(1.0, 0.0), Coordinates(Vector2())))
        return bullet
    }

    playerEntity.addHull(hull)
    playerEntity.addModule(thruster)
    playerEntity.addModule(torquer)
    playerEntity.addModule(gun)

    playerEntity.addSystem(ThrusterSystem( listOf(thruster), EntityStation()))
    playerEntity.addSystem(TorqueSystem( listOf(torquer), EntityStation()))
    playerEntity.addSystem(WeaponGroupSystem(listOf(gun), {createBullet()} , EntityStation()))

    playerEntity.effectsConsumer = effectsManager
    playerEntity.entityConsumer = physicsLayer
    physicsLayer.addEntity(playerEntity)

    val playerController = PlayerController(bitSet)
    controllerManager.addControllerEntry(playerController, playerEntity)

    while(true){
        game.update(timeStep)
    }

}

class Game(val models: MutableList<Model>, val physicsManager: PhysicsManager, val controllerManager: ControllerManager, val effectsManager: EffectsManager, val gui: RendererI){

    fun update(timeStep : Double){
        val modelDataMap = hashMapOf<Model, MutableList<Renderer.Renderable>>()

        //Need to populate data to GUI atleast once before calling gui.setup() or else we get a crash on laptop. Maybe different GPU is reason?
        val populateData = fun (details : CameraDetails) {
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

            gui.updateDrawables(modelDataMap)
            gui.updateCamera(details)
            gui.updateDebug(debugData)
        }

        populateData(CameraDetails(Vector2(0.0) , 1.0, 0.0))

        while(true){
            Thread.sleep(16)
            val pOut = physicsManager.update(timeStep)
            effectsManager.update(timeStep)
            controllerManager.update()
            populateData(CameraDetails(Vector2(0.0) , 1.0, 0.0))
        }
    }
}



