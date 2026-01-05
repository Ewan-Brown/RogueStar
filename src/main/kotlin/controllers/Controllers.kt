package controllers

import ControllerLayerI
import DebugLineData
import graphics.Graphics
import math.Vector2
import models.Model
import java.awt.event.KeyEvent
import java.util.BitSet

class ControllerLayer : ControllerLayerI {

    private class ControllerEntityEntry<S: ControllerInterface>(val controller : Controller<S>, val plant: S){
        fun update(){
            controller.update(plant)
        }
    }
    private val controllerEntryList = mutableListOf<ControllerEntityEntry<*>>()

    override fun update() {
        controllerEntryList.removeIf{it.plant.isMarkedForRemoval()}
        for (controllerEntityEntry in controllerEntryList) {
           controllerEntityEntry.update()
        }
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {}
    override fun getDebugLines() : List<DebugLineData>{
        return listOf()
    }

    override fun <T : ControllerInterface> addControllerEntry(controller: Controller<T>, plant: T) {
        controllerEntryList.add(ControllerEntityEntry(controller, plant))
    }
}
//
//abstract class DirectController<T : AbstractKinematicEntity>(){
//    abstract fun update(plant: T)
//}

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

class PlayerController(val bitSet: BitSet) : Controller<DummyControllerInterface>(){
    override fun update(plant: DummyControllerInterface) {
//        val thrusters = plant.getThrusters()
//        val torquers = plant.getTorquers()
//        val guns = plant.getGuns()
//
//        var thrust = Vector2(0.0, 0.0)
//        for (entry in ThrustKeys.entries) {
//            if(bitSet[entry.keyValue]) {
//                thrust += entry.vector
//            }
//        }
//
//        thrust = thrust.normalize().rotate(plant.getOrientation().getAngle())
//
//        var torque = 0.0
//        for (entry in TorqueKeys.entries){
//            if(bitSet[entry.keyValue]){
//                torque += entry.torque
//            }
//        }
//
//        for (thruster in thrusters) {
//            thruster.setOrientation(thrust)
//            thruster.setThrottle(1.0)
//        }
//
//        for(torquer in torquers){
//            torquer.setTorque(torque/100)
//        }
//
//        for(gun in guns){
//            gun.toggleFiring((bitSet[KeyEvent.VK_SPACE]))
//        }
    }
}

abstract class Station

interface ControllerInterface{
    abstract fun isMarkedForRemoval() : Boolean
}

abstract class Controller<S: ControllerInterface>(){
    abstract fun update(plant: S)
}

interface DummyControllerInterface : ControllerInterface{

}

