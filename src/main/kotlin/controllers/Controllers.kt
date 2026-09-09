package controllers

import graphics.DebugCircleData
import graphics.DebugLineData
import graphics.Renderer
import math.Orientation
import math.Vector2
import models.Model
import physics.ComplexEntity
import physics.ThrusterSystem
import physics.TorqueSystem
import physics.WeaponSystem
import java.awt.event.KeyEvent
import java.util.BitSet

//TODO Entries are currently never removed
class ControllerManager {

    private class ControllerEntityEntry<S>(val controller : Controller<S>, val plantInterface: S){
        fun update(){
            controller.update(plantInterface)
        }
    }

    private val controllerEntryList = mutableListOf<ControllerEntityEntry<*>>()

    fun update() {
        for (controllerEntityEntry in controllerEntryList) {
           controllerEntityEntry.update()
        }
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Renderer.Renderable>>) {}

    fun getDebugLines() : List<DebugLineData>{
        return emptyList()
    }

    fun getDebugCircles(): List<DebugCircleData> {
        return emptyList()
    }

    fun <T> addControllerEntry(controller: Controller<T>, `interface`: T) {
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

class PlayerController(val bitSet: BitSet) : Controller<ComplexEntity>(){

    override fun update(plant: ComplexEntity) {
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
        val weaponsSystem : WeaponSystem<*, *> = plant.getSystems().filterIsInstance<WeaponSystem<*, *>>().first()

        val thrustAngle = Vector2().getAngleTo(thrust)
        thrusterSystem.setThrust(Orientation(thrustAngle), thrust.normalize().getMagnitude() * .02)
        torquerSystem.setTorque(torque*0.01)

    }
}

class SimpleNPCController() : Controller<ComplexEntity>(){

    private var currentThrust: Vector2 = Vector2(0.5, 0.0)

    override fun update(plant: ComplexEntity) {
        var thrust = currentThrust

        currentThrust = currentThrust.rotate(0.07)
        var torque = Math.random() - 0.5
        val firing = false

        val thrusterSystem : ThrusterSystem = plant.getSystems().filterIsInstance<ThrusterSystem>().first()
        val torquerSystem : TorqueSystem = plant.getSystems().filterIsInstance<TorqueSystem>().first()
        val weaponsSystem : WeaponSystem<*, *> = plant.getSystems().filterIsInstance<WeaponSystem<*, *>>().first()

        val thrustAngle = Vector2().getAngleTo(thrust)
        thrusterSystem.setThrust(Orientation(thrustAngle), thrust.normalize().getMagnitude() * .02)
        torquerSystem.setTorque(torque*0.01)
    }
}