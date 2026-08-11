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
abstract class EntitySystem(){
    abstract fun update(timeStep: Double, entity: ComplexEntity)
}

//TODO Decide
/**
 * Should each system just have 1 system?
 */

//TODO Add fuel system
class ThrusterSystem(private val thrusters: List<Thruster>, private val pilotStation: EntityStation?) : EntitySystem() {

    fun setThrust(direction: Orientation<EntityReferenceFrame>, thrust: Double){
        for (thruster in thrusters){
            val desiredOrientation = direction.applyTransform(getTransformParentToLocalFrame(thruster))
            thruster.thrusterOrientation = desiredOrientation
            thruster.thrusterThrottle = thrust
        }
    }

    override fun update(timeStep: Double, entity: ComplexEntity){
        var forceOrigin : Coordinates<EntityReferenceFrame> = Coordinates(thrusters.map { it.thrustForceOrigin.applyTransform(getTransformLocalToParentFrame(it)).getVector() }.reduce { acc, vec -> acc + vec } / thrusters.count().toDouble())
        var netForceVector : Vector2 = thrusters.map {
            val localForceVec = Vector2(it.thrusterOrientation.getAngle()) * it.thrusterThrottle
            val entityFrameForceVec = localForceVec.rotate(getTransformLocalToParentFrame(it).rotation)
            return@map entityFrameForceVec
        }.reduce { acc, vec -> acc + vec } / thrusters.count().toDouble()
        val force = Force(netForceVector, forceOrigin)
        entity.applyForce(force)
    }
}

class TorqueSystem(private val torquers: List<Torquer>, private val pilotStation: EntityStation?) : EntitySystem(){
    override fun update(timeStep: Double, entity: ComplexEntity) {
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
abstract class WeaponSystem<W : Weapon, P: ProjectileEntity>(private val weapons: List<W>, private val projectileCreator: () -> P, private val weaponStation: EntityStation?, private val ammoDepot: List<AmmoDepot>) : EntitySystem(){
    override fun update(timeStep: Double, entity: ComplexEntity) {
        for(weapon in weapons){
//            weapon.cooldownRemaining = min(0.0, weapon.cooldownRemaining - timeStep)
//            if(weapon.cooldownRemaining <= 0.0 && weapon.isToggledOn){
//                // fire!
//                val proj = projectileCreator()
//                val spawnPoseInComponentCoords: Pose<ComponentReferenceFrame> = Pose(weapon.projectileSpawnLocation, Orientation(0.0), ZHeight(0.0))
//                val t1 = getTransformLocalToParentFrame(weapon)
//                val t2 = getTransformLocalToParentFrame(entity)
//                val t3 = combineTransforms(t1, t2)
//                val spawnPoseInWorldCoords = spawnPoseInComponentCoords.applyTransform(t3)
//
//                proj.setPose(spawnPoseInWorldCoords)
//
//                entity.sendEntity(proj)
//
//                weapon.cooldownRemaining = weapon.maxCooldown
//            }
        }
    }
}

abstract class BulletWeaponSystem