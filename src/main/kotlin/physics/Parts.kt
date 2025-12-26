package physics

import graphics.Graphics
import math.*
import models.Model
import java.util.*


interface EntityPartI {
    fun getModel() : Model
    fun getScale() : Double
    fun getColor() : Graphics.ColorData
    fun setColor(color: Graphics.ColorData)
    fun getMetadata() : Graphics.MetaData
    fun isCollideable() : Boolean
    fun getCenterOfMass() : Coordinates<PartSpace>
    fun getZHeight() : ZHeight<ShipSpace>
    fun getMass(): Double
    fun translate(vector: Vector2)
    fun rotate(theta: Double)
    fun onDamage(d: Int)
    fun getTransform() : Transform<PartSpace, ShipSpace>
    /**
     * Implicitly local center is always (0, 0)
     */
    fun getCoordinates() : Coordinates<ShipSpace>
    fun getOrientation() : Orientation<ShipSpace>
}

open class EntityPartImpl() : EntityPartI {

    private var position: Coordinates<ShipSpace> = Coordinates(Vector2())
    private var zpos: ZHeight<ShipSpace> = ZHeight(0.0)
    private var rotation: Orientation<ShipSpace> = Orientation(0.0)
    private val scale = 1.0
    private var life = 100

    private var color : Graphics.ColorData =  Graphics.ColorData(1.0f, 0.0f, 1.0f, 1.0f)

    //TODO We can deduplicate this stuff between this and Ship?
    override fun translate(vector: Vector2) {
        this.position += vector
    }

    override fun rotate(theta: Double) {
        this.rotation += theta
    }

    override fun onDamage(d: Int) {
        life -= d
    }

    override fun getZHeight(): ZHeight<ShipSpace> = zpos
    //TODO Implement this correctly
    override fun getMass(): Double {
        return 1.0
    }

    override fun getModel(): Model = Model.SQUARE
    override fun getScale(): Double {
        return scale;
    }

    override fun getColor() = color
    override fun setColor(color: Graphics.ColorData) {
        this.color = color
    }

    override fun getMetadata(): Graphics.MetaData { return Graphics.MetaData(1.0f)
    }
    override fun isCollideable(): Boolean {
        return true
    }

    override fun getCenterOfMass(): Coordinates<PartSpace> {
        return Coordinates(Vector2())
    }

    final override fun getTransform(): Transform<PartSpace, ShipSpace> {
        return Transform(position.getVector(), rotation.getAngle(), zpos.getZ())
    }

    override fun getCoordinates(): Coordinates<ShipSpace> {
        return position
    }

    override fun getOrientation(): Orientation<ShipSpace> {
        return rotation
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

    override fun getFiringPosition(): Coordinates<PartSpace> {
        TODO("Not yet implemented")
    }

    override fun getFiringOrientation(): Orientation<PartSpace> {
        return Orientation(Math.PI/2.0)
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
    fun getFiringPosition() : Coordinates<PartSpace>
    fun getFiringOrientation() : Orientation<PartSpace>
    fun createProjectile() : AbstractKinematicEntity
    fun isFiring() : Boolean
    fun toggleFiring(firing: Boolean)
}

enum class Affiliation {ALLY, NEUTRAL, FOE, UNKNOWN, NEUTRALIZED}
data class RadarReading(val uuid: UUID, val position: Coordinates<WorldSpace>, val velocity: Double, val affiliation: Affiliation)