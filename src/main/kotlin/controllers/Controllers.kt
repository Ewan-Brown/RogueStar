package controllers

import ControllerLayerI
import DebugLineData
import graphics.Graphics
import math.Vector2
import models.Model
import physics.AbstractKinematicEntity
import physics.AbstractPawn
import physics.ControllableEntity
import physics.SimplePawn
import java.awt.event.KeyEvent
import java.util.BitSet

// World <-> Plant <-> Controller
// World updates plant, plant stores relevant information about its surroundings
// Controller interprets stored information, and interacts with the plant accordingly

// Ship example:
// World.update() -> plant.sensor -> controller.update(plant) -> Engines/Guns/etc -> World.update()

// Pawn example : ?
// Maybe pawns should store a map

class ControllerLayer : ControllerLayerI {

    private class ControllerEntityEntry<T : ControllerTarget>(val controller : Controller<T>, val entity: T){
        fun update(){
            controller.update(entity)
        }
    }
    private val controllerEntryList = mutableListOf<ControllerEntityEntry<*>>()

    override fun update() {
        controllerEntryList.removeIf{it.entity.markedForControllerRemoval()}
        for (controllerEntityEntry in controllerEntryList) {
           controllerEntityEntry.update()
        }
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {}
    override fun getDebugLines() : List<DebugLineData>{
        return listOf()
    }

    override fun <T : ControllerTarget> addControllerEntry(controller: Controller<T>, target: T) {
        controllerEntryList.add(ControllerEntityEntry(controller, target))
    }
}

interface Controller<in P>{
    fun update(plant: P)
}

interface ControllerTarget{
    fun markedForControllerRemoval() : Boolean
}

abstract class PawnController<P: AbstractPawn> : Controller<P> {}
abstract class ShipController<T : AbstractKinematicEntity>() : Controller<T> {}

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

//TODO Maybe abstract this some more. Like send commands instead of coupling this to keylistener-bitset
class PlayerShipController(val bitSet: BitSet) : ShipController<ControllableEntity>(){
    override fun update(plant: ControllableEntity) {
        val thrusters = plant.getThrusters()
        val torquers = plant.getTorquers()
        val guns = plant.getGuns()
//        val radars = plant.getRadars()
//        val readings = radars.map{ it.getReadings()}.flatten()

        var thrust = Vector2(0.0, 0.0)
        for (entry in ThrustKeys.entries) {
            if(bitSet[entry.keyValue]) {
                thrust += entry.vector
            }
        }
        
        thrust = thrust.normalize().rotate(plant.getOrientation().getAngle())

        var torque = 0.0
        for (entry in TorqueKeys.entries){
            if(bitSet[entry.keyValue]){
                torque += entry.torque
            }
        }

        for (thruster in thrusters) {
            thruster.setOrientation(thrust)
            thruster.setThrottle(1.0)
        }

        for(torquer in torquers){
            torquer.setTorque(torque/100)
        }

        for(gun in guns){
            gun.toggleFiring((bitSet[KeyEvent.VK_SPACE]))
        }
    }

}