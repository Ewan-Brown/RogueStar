package physics

import math.Transformation3
import math.Vector2
import physics.PhysicsLayer.KinematicData

interface PointProjectileI : KinematicEntityI{
    fun getPointOfContact() : Vector2
    fun doesCollide(otherEntity: KinematicEntityI) : Boolean
    fun doesPenetrateShield() : Boolean
    fun getHullDamage() : Int
    fun getShieldDamage() : Int
}

open class DumbProjectile() : DumbEntity(), PointProjectileI{
    override fun getPointOfContact(): Vector2 {
        return Vector2(0.0, 0.0)
    }

    override fun doesCollide(otherEntity: KinematicEntityI): Boolean {
        return false;
    }

    override fun doesPenetrateShield(): Boolean {
        return false
    }

    override fun getHullDamage(): Int {
        return 0
    }

    override fun getShieldDamage(): Int {
        return 0
    }

}