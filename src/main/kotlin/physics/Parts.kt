package physics

import graphics.Graphics
import math.*
import models.Model
import java.util.*

//interface EntityPartI : InReferenceFrame<EntityReferenceFrame>, HasReferenceFrame<PartReferenceFrame> {
//    fun getModel() : Model
//    fun getScale() : Double
//    fun getColor() : Graphics.ColorData
//    fun setColor(color: Graphics.ColorData)
//    fun getMetadata() : Graphics.MetaData
//    fun isCollideable() : Boolean
//    fun getCenterOfMass() : Coordinates<PartReferenceFrame>
//    fun getMass(): Double
//    fun translate(vector: Vector2)
//    fun rotate(theta: Double)
//    fun onDamage(d: Int)
//}

//open class EntityPartImpl() : EntityPartI {
//
//    private var position: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
//    private var zpos: ZHeight<EntityReferenceFrame> = ZHeight(5.0)
//    private var rotation: Orientation<EntityReferenceFrame> = Orientation(0.0)
//    private val scale = 1.0
//    private var life = 100
//
//    private var color : Graphics.ColorData =  Graphics.ColorData(1.0f, 0.0f, 1.0f, 1.0f)
//
//    //TODO We can deduplicate this stuff between this and Ship?
//    override fun translate(vector: Vector2) {
//        this.position += vector
//    }
//
//    override fun rotate(theta: Double) {
//        this.rotation += theta
//    }
//
//    override fun onDamage(d: Int) {
//        life -= d
//    }
//
//    override fun getZHeight(): ZHeight<EntityReferenceFrame> = zpos
//    //TODO Implement this correctly
//    override fun getMass(): Double {
//        return 1.0
//    }
//
//    override fun getModel(): Model = Model.SQUARE
//    override fun getScale(): Double {
//        return scale
//    }
//
//    override fun getColor() = color
//    override fun setColor(color: Graphics.ColorData) {
//        this.color = color
//    }
//
//    override fun getMetadata(): Graphics.MetaData { return Graphics.MetaData(1.0f)
//    }
//    override fun isCollideable(): Boolean {
//        return true
//    }
//
//    override fun getCenterOfMass(): Coordinates<PartReferenceFrame> {
//        return Coordinates(Vector2())
//    }
//
//    override fun getCoordinates(): Coordinates<EntityReferenceFrame> {
//        return position
//    }
//
//    override fun getOrientation(): Orientation<EntityReferenceFrame> {
//        return rotation
//    }
//}
//
//class Cockpit() : EntityPartImpl(), Control, Torquer, Radar {
//    private var torque = 0.0
//    override fun getTorque(): Double {
//        return torque
//    }
//
//    override fun setTorque(torque: Double) {
//        this.torque = torque
//    }
//
//    override fun updateReadings(world: World) {
//        TODO("Not yet implemented")
//    }
//
//    override fun getReadings(): List<RadarReading> {
//        TODO("Not yet implemented")
//    }
//}
//
//class BasicThruster() : EntityPartImpl(), Thruster {
//    private var orientation: Vector2 = Vector2(0.0, 0.0)
//    private val maxPower = 0.01
//    private var throttle = 0.0
//
//    override fun getOrientationUnitVector(): Vector2 {
//        return orientation
//    }
//
//    override fun getMaxPower(): Double {
//        return maxPower
//    }
//
//    override fun getThrottle(): Double {
//        return throttle
//    }
//
//    override fun setThrottle(t: Double) {
//        throttle = t
//    }
//
//    override fun setOrientation(orientation: Vector2) {
//        this.orientation = orientation
//    }
//
//}
//
//class BasicGun() : EntityPartImpl(), Gun {
//
//    var firing = false;
//
//    override fun getFiringPosition(): Coordinates<PartReferenceFrame> {
//        TODO("Not yet implemented")
//    }
//
//    override fun getFiringOrientation(): Orientation<PartReferenceFrame> {
//        return Orientation(Math.PI/2.0)
//    }
//
//    override fun createProjectile(): AbstractEntity {
//        return DumbProjectile()
//    }
//
//    override fun isFiring(): Boolean {
//        return firing
//    }
//
//    override fun toggleFiring(firing: Boolean) {
//        this.firing = firing
//    }
//
//}
//
//interface HasModule : EntityPartI
//
////Center of command/control
//interface Control : HasModule{
//}
//
////Generate Torque
//interface Torquer : HasModule{
//    fun getTorque() : Double
//    fun setTorque(torque: Double)
//}
//
////Generates Thrust
//interface Thruster : HasModule{
//    fun getOrientationUnitVector() : Vector2
//    fun getMaxPower() : Double
//    fun getThrottle() : Double
//    fun setThrottle(t: Double)
//    fun setOrientation(orientation: Vector2)
//    fun getCurrentThrust() : Vector2 = getOrientationUnitVector() * (getMaxPower() * getThrottle())
//}
//
////Generates radar readings
//interface Radar : HasModule{
//    fun updateReadings(world: World)
//    fun getReadings() : List<RadarReading>
//}
//
////Generates projectiles
//interface Gun : HasModule{
//    fun getFiringPosition() : Coordinates<PartReferenceFrame>
//    fun getFiringOrientation() : Orientation<PartReferenceFrame>
//    fun createProjectile() : AbstractEntity
//    fun isFiring() : Boolean
//    fun toggleFiring(firing: Boolean)
//}

enum class Affiliation {ALLY, NEUTRAL, FOE, UNKNOWN, NEUTRALIZED}
data class RadarReading(val uuid: UUID, val position: Coordinates<WorldReferenceFrame>, val velocity: Double, val affiliation: Affiliation)