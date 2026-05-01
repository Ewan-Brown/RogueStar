package physics

import math.Coordinates
import math.EntityReferenceFrame

interface HasCollidingPoint{
    fun getPointOfContact() : Coordinates<EntityReferenceFrame>
    fun doesCollideWith(otherEntity: Entity) : Boolean
}

//open class DumbProjectile() : AbstractEntity(), HasCollidingPoint{
//    init {
//        val body = EntityPartImpl()
//        addPart(body)
//    }
//    override fun update(timeStep: Double) {}
//    override fun markedForRemoval(): Boolean {return false }
//
//    override fun getPointOfContact(): Coordinates<EntityReferenceFrame> {
//        return Coordinates(Vector2(0.0, 0.0))
//    }
//
//    override fun doesCollideWith(otherEntity: EntityI): Boolean {
//        return false;
//    }
//
//    override fun doesPenetrateShield(): Boolean {
//        return false
//    }
//
//    override fun getHullDamage(): Int {
//        return 0
//    }
//
//    override fun getShieldDamage(): Int {
//        return 0
//    }
//
//}