package physics

import math.Vector2

interface HasCollidingPoint : KinematicEntityI{
    fun getPointOfContact() : Vector2
    fun doesCollideWith(otherEntity: KinematicEntityI) : Boolean
    fun doesPenetrateShield() : Boolean
    fun getHullDamage() : Int
    fun getShieldDamage() : Int
}

open class DumbProjectile() : AbstractKinematicEntity(), HasCollidingPoint{

    init {
        val body = EntityPartImpl()
        body.scale(0.1)
        addPart(body)
    }
    override fun update(timeStep: Double) {}
    override fun markedForRemoval(): Boolean {return false }

    override fun getPointOfContact(): Vector2 {
        return Vector2(0.0, 0.0)
    }

    override fun doesCollideWith(otherEntity: KinematicEntityI): Boolean {
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