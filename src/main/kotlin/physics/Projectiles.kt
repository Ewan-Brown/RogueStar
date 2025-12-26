package physics

import math.Coordinate
import math.ShipSpace
import math.Vector2

interface HasCollidingPoint : KinematicEntityI{
    fun getPointOfContact() : Coordinate<ShipSpace>
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

    override fun getPointOfContact(): Coordinate<ShipSpace> {
        return Coordinate(Vector2(0.0, 0.0))
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