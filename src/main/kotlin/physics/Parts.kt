package physics

import graphics.Graphics
import math.Transformation3
import math.Vector2
import models.Model
import java.util.*

abstract class EntityPart(val mass: Double) {
    abstract fun getModel() : Model
    abstract fun getColor() : Graphics.ColorData
    abstract fun getMetadata() : Graphics.MetaData
    abstract fun isCollideable() : Boolean
    abstract fun getLocalTransform() : Transformation3
}

open class DumbPart(private val transform: Transformation3, density: Double) : EntityPart(density) {
    override fun getLocalTransform(): Transformation3 {return transform.copy()}
    override fun getModel(): Model { return Model.SQUARE }
    override fun getColor(): Graphics.ColorData { return Graphics.ColorData(1.0f, 0.0f, 1.0f, 1.0f) }
    override fun getMetadata(): Graphics.MetaData { return Graphics.MetaData(1.0f)
    }

    override fun isCollideable(): Boolean {
        return true
    }
}

class SuperPart(transform: Transformation3, density: Double) : DumbPart(transform, density), Thruster, Torquer{
    private var orientation: Vector2 = Vector2(0.0, 0.0)
    private val maxPower = 0.01
    private var throttle = 0.0
    private var torque = 0.0

    override fun getOrientationUnitVector(): Vector2 {
        return orientation
    }

    override fun getMaxPower(): Double {
        return maxPower
    }

    override fun getThrottle(): Double {
        return throttle
    }

    override fun setThrottle(t: Double) {
        throttle = t
    }

    override fun setOrientation(orientation: Vector2) {
        this.orientation = orientation
    }

    override fun getTorque(): Double {
        return torque
    }

    override fun setTorque(torque: Double) {
        this.torque = torque
    }

}

enum class Affiliation {ALLY, NEUTRAL, FOE, UNKNOWN, NEUTRALIZED}
data class RadarReading(val uuid: UUID, val position: Vector2, val velocity: Double, val affiliation: Affiliation)


interface Torquer {
    fun getTorque() : Double
    fun setTorque(torque: Double)
}

interface Thruster {
    fun getOrientationUnitVector() : Vector2
    fun getMaxPower() : Double
    fun getThrottle() : Double
    fun setThrottle(thrust: Double)
    fun setOrientation(orientation: Vector2)
    fun getCurrentThrust() : Vector2 = getOrientationUnitVector() * (getMaxPower() * getThrottle())
}

//interface data

interface Radar {
    fun updateReadings(world: PhysicsLayer.World)
    fun getReadings() : List<RadarReading>
}

interface Gun {
    abstract fun getFiringPosition() : Transformation3
    abstract fun getFiringOrientation() : Double
    abstract fun createProjectile() : KinematicEntityImpl
}