package controllers

import ControllerLayerI
import DebugLineData
import graphics.Graphics
import math.Vector2
import models.Model
import physics.ControllableEntity
import physics.SimpleShip
import java.awt.event.KeyEvent
import java.util.BitSet

class ControllerLayer : ControllerLayerI {

    private class ControllerEntityEntry<S: PlantInterface>(val controller : Controller<S>, val plantInterface: S){
        fun update(){
            controller.update(plantInterface)
            plantInterface.update()
        }
    }

    private val controllerEntryList = mutableListOf<ControllerEntityEntry<*>>()

    override fun update() {
        controllerEntryList.removeIf{it.controller.isMarkedForRemoval()}
        for (controllerEntityEntry in controllerEntryList) {
           controllerEntityEntry.update()
        }
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {}
    override fun getDebugLines() : List<DebugLineData>{
        return listOf()
    }

    override fun <T : PlantInterface> addControllerEntry(controller: Controller<T>, `interface`: T) {
        controllerEntryList.add(ControllerEntityEntry(controller, `interface`))
    }
}

enum class ThrustKeys(val keyValue: Int, val vector: Vector2){
    UP(KeyEvent.VK_W, Vector2(0.0, 1.0)),
    DOWN(KeyEvent.VK_S, Vector2(0.0, -1.0)),
    LEFT(KeyEvent.VK_A, Vector2(-1.0, 0.0)),
    RIGHT(KeyEvent.VK_D, Vector2(1.0, 0.0))
}

enum class TorqueKeys(val keyValue: Int, val torque: Double){
    LEFT(KeyEvent.VK_Q, 1.0),
    RIGHT(KeyEvent.VK_E, -1.0)
}

sealed class Controller<in S: PlantInterface>{
    abstract fun update(plant: S)
    abstract fun isMarkedForRemoval() : Boolean
}

class PlayerController(val bitSet: BitSet) : Controller<SimpleInterface>(){
    override fun update(plant: SimpleInterface) {
        var thrust = Vector2(0.0, 0.0)
        for (entry in ThrustKeys.entries) {
            if(bitSet[entry.keyValue]) {
                thrust += entry.vector
            }
        }

        var torque = 0.0
        for (entry in TorqueKeys.entries){
            if(bitSet[entry.keyValue]){
                torque += entry.torque
            }
        }

        val firing = bitSet[KeyEvent.VK_SPACE]

        plant.setDesiredThrust(thrust)
        plant.setDesiredTorque(torque/100.0)
        plant.setFiring(firing)
    }

    override fun isMarkedForRemoval(): Boolean {
        return false
    }
}
