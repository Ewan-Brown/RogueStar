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
import math.Vector2
import java.util.*
import ControllerLayerI
import DebugLineData
import controllers.PlayerController
import graphics.CameraDetails
import graphics.GraphicsI
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

    playerEntity.effectsConsumer = effectsLayer
    playerEntity.entityConsumer = physicsLayer
    physicsLayer.addEntity(playerEntity)
    val playerController = PlayerController(bitSet)
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
            debugData.addAll(physicsLayer.getDebugLines())
            debugData.addAll(effectsLayer.getDebugLines())
            debugData.addAll(controllerLayer.getDebugLines())

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



