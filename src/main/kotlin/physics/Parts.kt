package physics

import graphics.Graphics
import math.Transformation2
import math.Transformation3
import math.Vector2
import models.Model
import java.util.*


interface EntityPartI {
    abstract fun getModel() : Model
    abstract fun getColor() : Graphics.ColorData
    abstract fun setColor(color: Graphics.ColorData)
    abstract fun getMetadata() : Graphics.MetaData
    abstract fun isCollideable() : Boolean
    abstract fun getLocalTransform() : Transformation2
    abstract fun getLocalZPos() : Double
    abstract fun getDensity() : Double
    abstract fun translate(vector: Vector2)
    abstract fun rotate(theta: Double)
    abstract fun scale(s: Double)
    abstract fun onDamage(d: Int)
}

open class EntityPartImpl() : EntityPartI {

    private var position = Vector2()
    private var zpos = 0.0;
    private var rotation = 0.0
    private var scale = 1.0
    private var life = 100

    private var color : Graphics.ColorData =  Graphics.ColorData(1.0f, 0.0f, 1.0f, 1.0f)


    //TODO We can deduplicate this stuff between this and Ship
    override fun getLocalTransform(): Transformation2 {return Transformation2(position, rotation, scale) }
    override fun translate(vector: Vector2) {
        this.position += vector
    }

    override fun rotate(theta: Double) {
        this.rotation += theta
    }

    override fun scale(s: Double) {
        this.scale *= s
    }

    override fun onDamage(d: Int) {
        life -= d
    }

    override fun getLocalZPos(): Double = zpos

    override fun getModel(): Model = Model.SQUARE
    override fun getColor() = Graphics.ColorData(color.red*(life/100f), color.green*(life/100f), color.blue*(life/100f), color.alpha*(life/100f))
    override fun setColor(color: Graphics.ColorData) {
        this.color = color
    }

    override fun getMetadata(): Graphics.MetaData { return Graphics.MetaData(1.0f)
    }
    override fun isCollideable(): Boolean {
        return true
    }

    override fun getDensity(): Double {
        return 1.0
    }
}

class Cockpit() : EntityPartImpl(), Control, Torquer, Radar {
    private var torque = 0.0
    override fun getTorque(): Double {
        return torque
    }

    override fun setTorque(torque: Double) {
        this.torque = torque
    }

    override fun updateReadings(world: PhysicsLayer.World) {
        TODO("Not yet implemented")
    }

    override fun getReadings(): List<RadarReading> {
        TODO("Not yet implemented")
    }
}

class BasicThruster() : EntityPartImpl(), Thruster {
    private var orientation: Vector2 = Vector2(0.0, 0.0)
    private val maxPower = 0.01
    private var throttle = 0.0

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

}

class BasicGun() : EntityPartImpl(), Gun {

    var firing = false;

    override fun getFiringPosition(): Vector2 {
        TODO("Not yet implemented")
    }

    override fun getFiringOrientation(): Double {
        return Math.PI/2.0
    }

    override fun createProjectile(): AbstractKinematicEntity {
        return DumbProjectile()
    }

    override fun isFiring(): Boolean {
        return firing
    }

    override fun toggleFiring(firing: Boolean) {
        this.firing = firing
    }

}

//Center of command/control
interface Control : EntityPartI{}

//Generate Torque
interface Torquer : EntityPartI{
    fun getTorque() : Double
    fun setTorque(torque: Double)
}

//Generates Thrust
interface Thruster : EntityPartI{
    fun getOrientationUnitVector() : Vector2
    fun getMaxPower() : Double
    fun getThrottle() : Double
    fun setThrottle(thrust: Double)
    fun setOrientation(orientation: Vector2)
    fun getCurrentThrust() : Vector2 = getOrientationUnitVector() * (getMaxPower() * getThrottle())
}

//Generates radar readings
interface Radar : EntityPartI{
    fun updateReadings(world: PhysicsLayer.World)
    fun getReadings() : List<RadarReading>
}

//Generates projectiles
interface Gun : EntityPartI{
    fun getFiringPosition() : Vector2
    fun getFiringOrientation() : Double
    fun createProjectile() : AbstractKinematicEntity
    fun isFiring() : Boolean
    fun toggleFiring(firing: Boolean)
}

enum class Affiliation {ALLY, NEUTRAL, FOE, UNKNOWN, NEUTRALIZED}
data class RadarReading(val uuid: UUID, val position: Vector2, val velocity: Double, val affiliation: Affiliation)