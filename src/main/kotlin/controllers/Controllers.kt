package controllers

import ControllerLayerI
import DebugLineData
import graphics.Graphics
import math.Orientation
import math.Vector2
import models.Model
import physics.Entity
import physics.ThrusterSystem
import physics.TorqueSystem
import physics.WeaponGroupSystem
import java.awt.event.KeyEvent
import java.util.BitSet

//TODO Entries are currently never removed
class ControllerLayer : ControllerLayerI {

    private class ControllerEntityEntry<S>(val controller : Controller<S>, val plantInterface: S){
        fun update(){
            controller.update(plantInterface)
        }
    }

    private val controllerEntryList = mutableListOf<ControllerEntityEntry<*>>()

    override fun update() {
        for (controllerEntityEntry in controllerEntryList) {
           controllerEntityEntry.update()
        }
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {}
    override fun getDebugLines() : List<DebugLineData>{
        return listOf()
    }

    override fun <T> addControllerEntry(controller: Controller<T>, `interface`: T) {
        controllerEntryList.add(ControllerEntityEntry(controller, `interface`))
    }
}

enum class ThrustKeys(val keyValue: Int, val vector: Vector2){
    UP(KeyEvent.VK_W, Vector2(0.0, 1.0)),
    DOWN(KeyEvent.VK_S, Vector2(0.0, -1.0)),
    LEFT(KeyEvent.VK_A, Vector2(1.0, 0.0)),
    RIGHT(KeyEvent.VK_D, Vector2(-1.0, 0.0))
}

enum class TorqueKeys(val keyValue: Int, val torque: Double){
    LEFT(KeyEvent.VK_Q, 1.0),
    RIGHT(KeyEvent.VK_E, -1.0)
}

sealed class Controller<in T>{
    abstract fun update(plant: T)
}

class PlayerController(val bitSet: BitSet) : Controller<Entity>(){

    override fun update(plant: Entity) {
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

        val thrusterSystem : ThrusterSystem = plant.getSystems().filterIsInstance<ThrusterSystem>().first()
        val torquerSystem : TorqueSystem = plant.getSystems().filterIsInstance<TorqueSystem>().first()
        val weaponsSystem : WeaponGroupSystem = plant.getSystems().filterIsInstance<WeaponGroupSystem>().first()

        val thrustAngle = Vector2().getAngleTo(thrust)
        thrusterSystem.setThrust(Orientation(thrustAngle), thrust.normalize().getMagnitude() * .02)
        torquerSystem.setTorque(torque*0.01)
        weaponsSystem.setToggle(firing)
//        plant.setDesiredThrust(thrust)
//        plant.setDesiredTorque(torque/100.0)
//        plant.setFiring(firing)
    }
}
