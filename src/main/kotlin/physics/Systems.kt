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
import kotlin.math.min

/**
 * Groups together modules and stations in order to provide functionality for an entity.
 *
 * Direct controllers directly reference the system to modify part state
 * Pawn controllers reference the system for state, then use pawns to attempt to modify part state via
 *
 */
abstract class EntitySystem(val entity: AbstractEntity){
    abstract fun update(timeStep: Double)
}

//TODO Add fuel system
class ThrusterSystem(entity: AbstractEntity, val thrusters: List<Thruster>, pilotStation: EntityStation?) : EntitySystem(entity) {

    fun setThrustDirection(desiredOrientation: Orientation<EntityReferenceFrame>, throttle: Double){
        for (thruster in thrusters){
            val componentOrientation = thruster.getOrientation()
            //This might need to be flipped
            val orientationDiff = desiredOrientation - componentOrientation
            thruster.thrusterOrientation = Orientation(orientationDiff)
            thruster.thrusterThrottle = throttle
        }
    }

    override fun update(timeStep: Double){
        var forceOrigin : Coordinates<EntityReferenceFrame> = Coordinates(thrusters.map { it.thrustForceOrigin.applyTransform(getTransformLocalToParentFrame(it)).getVector() }.reduce { acc, vec -> acc + vec } / thrusters.count().toDouble())
        var netForceVector : Vector2 = thrusters.map {
            val localForceVec = Vector2(it.thrusterOrientation.getAngle()) * it.thrusterThrottle
            val entityFrameForceVec = localForceVec.applyTransform(getTransformLocalToParentFrame(it))
            return@map entityFrameForceVec
        }.reduce { acc, vec -> acc + vec } / thrusters.count().toDouble()

        val force = Force(netForceVector, forceOrigin)
        entity.applyForce(force)
    }
}

class TorqueSystem(entity: AbstractEntity, val torquers: List<Torquer>, pilotStation: EntityStation?) : EntitySystem(entity){
    override fun update(timeStep: Double) {
        val netTorque = torquers.map{it.torque}.reduce { t1, t2 -> t1+t2 }
        entity.applyTorque(netTorque)
    }
}

//TODO Add Ammo system
class WeaponGroupSystem(entity: AbstractEntity, val weapons: List<Weapon>, val projectileCreator: () -> AbstractEntity, val weaponStation: EntityStation) : EntitySystem(entity){
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

}
//
//class RadarSystem() : EntitySystem(){
//
//}
//
//class ShieldSystem() : EntitySystem(){
//
//}

interface HasThrusters {
    fun getThrusters() : ThrusterSystem
}

interface HasTorquers {
    fun getTorquers() : TorqueSystem
}

interface HasWeapons{
    fun getWeapons() : WeaponGroupSystem
}
interface BasicShipInterface: HasThrusters, HasTorquers, HasWeapons