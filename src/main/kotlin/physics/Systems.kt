package physics

import math.ComponentReferenceFrame
import math.Coordinates
import math.EntityReferenceFrame
import math.Orientation
import math.Pose
import math.Vector2
import math.ZHeight
import math.combineTransforms
import math.getTransformLocalToParentFrame
import math.getTransformParentToLocalFrame
import kotlin.math.min

/**
 * Groups together modules and stations in order to provide functionality for an entity.
 *
 * Direct controllers directly reference the system to modify part state
 * Pawn controllers reference the system for state, then use pawns to attempt to modify part state via
 *
 */
abstract class EntitySystem(protected val entity: Entity){
    abstract fun update(timeStep: Double)
}

//TODO Add fuel system
class ThrusterSystem(entity: Entity, private val thrusters: List<Thruster>, private val pilotStation: EntityStation?) : EntitySystem(entity) {

    fun setThrust(direction: Orientation<EntityReferenceFrame>, thrust: Double){
        for (thruster in thrusters){
            val desiredOrientation = direction.applyTransform(getTransformParentToLocalFrame(thruster))
            thruster.thrusterOrientation = desiredOrientation
            thruster.thrusterThrottle = thrust
        }
    }

    override fun update(timeStep: Double){
        var forceOrigin : Coordinates<EntityReferenceFrame> = Coordinates(thrusters.map { it.thrustForceOrigin.applyTransform(getTransformLocalToParentFrame(it)).getVector() }.reduce { acc, vec -> acc + vec } / thrusters.count().toDouble())
        var netForceVector : Vector2 = thrusters.map {
            val localForceVec = Vector2(it.thrusterOrientation.getAngle()) * it.thrusterThrottle
            val entityFrameForceVec = localForceVec.rotate(getTransformLocalToParentFrame(it).rotation)
            return@map entityFrameForceVec
        }.reduce { acc, vec -> acc + vec } / thrusters.count().toDouble()

//        println("totalForce = $netForceVector")
//        println ("forceOrigin = $forceOrigin")
//        println("                    ")
        val force = Force(netForceVector, forceOrigin)
        entity.applyForce(force)
    }
}

class TorqueSystem(entity: Entity, private val torquers: List<Torquer>, private val pilotStation: EntityStation?) : EntitySystem(entity){
    override fun update(timeStep: Double) {
        val netTorque = torquers.map{it.torque}.reduce { t1, t2 -> t1+t2 }
        entity.applyTorque(netTorque)
    }

    fun setTorque(torque: Double){
        for (torquer in torquers){
            torquer.torque = torque
        }
    }
}

//TODO Add Ammo system
class WeaponGroupSystem(entity: Entity, private val weapons: List<Weapon>, private val projectileCreator: () -> Entity, private val weaponStation: EntityStation) : EntitySystem(entity){
    override fun update(timeStep: Double) {
        for(weapon in weapons){
            weapon.cooldownRemaining = min(0.0, weapon.cooldownRemaining - timeStep)
            if(weapon.cooldownRemaining <= 0.0 && weapon.isToggledOn){
                // fire!
                val proj = projectileCreator()
                val spawnPoseInComponentCoords: Pose<ComponentReferenceFrame> = Pose(weapon.projectileSpawnLocation, Orientation(0.0), ZHeight(0.0))
                val t1 = getTransformLocalToParentFrame(weapon)
                val t2 = getTransformLocalToParentFrame(entity)
                val t3 = combineTransforms(t1, t2)
                val spawnPoseInWorldCoords = spawnPoseInComponentCoords.applyTransform(t3)

                proj.setPose(spawnPoseInWorldCoords)

                entity.sendEntity(proj)
            }
        }
    }

    fun setToggle(toggle: Boolean){
        for (weapon in weapons){
            weapon.isToggledOn = toggle
        }
    }

}