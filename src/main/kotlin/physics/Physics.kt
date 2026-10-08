package physics

import graphics.BLUE
import graphics.CYAN
import graphics.DebugCircleData
import graphics.DebugLineData
import graphics.Renderer
import graphics.processRenderables
import math.CollisionInitiatorData
import math.Coordinates
import math.HasReferenceFrame
import math.LineCollisionTrigger
import math.PointCollisionTrigger
import math.Pose
import math.RadiusCollisionTrigger
import math.ReferenceFrame
import math.ReferenceFrameVariable
import math.Transform
import math.Vector2
import math.WorldReferenceFrame
import math.getTransformLocalToParentFrame
import models.Model
import kotlin.collections.HashMap

interface EntityConsumer {
    fun addEntity(entity: KinematicEntity)
}

@JvmInline
value class Timestamp(val time: Double){
    operator fun minus(t2 : Timestamp) : TimeDuration{
        return TimeDuration(time - t2.time)
    }
}
@JvmInline
value class TimeDuration(val duration: Double)

class PhysicsManager() : EntityConsumer, HasReferenceFrame<WorldReferenceFrame> {

    public var onDebugPause: (Boolean) -> Unit = {}
    private val entities = mutableListOf<KinematicEntity>()
    private val entityBuffer = mutableListOf<KinematicEntity>()

    fun update(timestep: Double) {
        synchronized(entityBuffer){
            entities.addAll(entityBuffer)
            entityBuffer.clear()
        }

        // Do entity physics updates
        for (entity in entities) {
            //Calculate new positions
            var velocity = entity.getVelocity()
            var rotVelocity = entity.getRotationalVelocity()

            val comLocal = entity.getCenterOfMass()
            var comWorld = comLocal.applyTransform(getTransformLocalToParentFrame(entity))

            entity.rotate(rotVelocity * timestep)
            comWorld += velocity * timestep
            entity.setPose(Pose(Coordinates(comWorld.getVector() - comLocal.getVector().rotate(entity.getOrientation().getAngle())), entity.getOrientation(), entity.getZHeight()))

            val entityAngle = entity.getOrientation().getAngle()

            //Calculate new derivatives
            velocity = entity.getVelocity() + if(entity.getMass() > 0) entity.popNetForce().rotate(entityAngle)/entity.getMass() else Vector2()
            rotVelocity = entity.getRotationalVelocity() + entity.popNetTorque()/entity.getMass()

            //Apply friction
            if(entity.doesFrictionApply()){
                //TODO Maybe do something better than this
                velocity *= 0.99
                rotVelocity *= 0.99
                entity.setVelocity(velocity)
                entity.setRotationalVelocity(rotVelocity)
            }

        }

        data class BoundingBox (val min : Vector2, val max: Vector2)
        //What if list of size 1? Does it make sense to return a BB?
        fun getBoundingBox(vectors: List<Vector2>) : BoundingBox {
            if(vectors.isEmpty()) throw IllegalArgumentException("Cant get a bounding box for empty list of vectors")
            var minX = vectors[0].getX()
            var minY = vectors[0].getY()
            var maxX = vectors[0].getX()
            var maxY = vectors[0].getY()
            for(v in vectors){
                if(v.getX() < minX) minX = v.getX()
                if(v.getX() > maxX) maxX = v.getX()
                if(v.getY() < minY) minY = v.getY()
                if(v.getY() > maxY) maxY = v.getY()
            }

            return BoundingBox(Vector2(minX, minY), Vector2(maxX, maxY))
        }

        for(possibleProjectile in entities){
            val collisionTriggerData = possibleProjectile.getCollisionTriggerData()
            when(collisionTriggerData){
                is LineCollisionTrigger -> {
                    val rect1 = getBoundingBox(listOf(collisionTriggerData.point1.getVector(), collisionTriggerData.point2.getVector()))
                    for (possibleTarget in entities) {
                        if(possibleTarget != possibleProjectile){
                            val circle = possibleTarget.getCrudeBoundingCircle()
                            if(possibleTarget.isCollideable(CollisionInitiatorData())){
                                if(circle != null){
                                    val rect2 = getBoundingBox(listOf(circle.center - Vector2(circle.radius, circle.radius),
                                        circle.center + Vector2(circle.radius, circle.radius)))
                                    //Crude collision measurement, TODO improve this
                                    if(rect1.max.getX() > rect2.min.getX() && rect1.min.getX() < rect2.max.getX() && rect1.max.getY() > rect2.min.getY() && rect1.min.getY() < rect2.max.getY()){
                                        //Collision consequence
                                        onDebugPause(true)
                                    }
                                }
                            }
                        }
                    }
                }
                is PointCollisionTrigger -> TODO()
                is RadiusCollisionTrigger -> TODO()
                null -> {} // do nothing!
            }
        }

        //Do pawn updates
        for(entity in entities) {
            if(entity is ComplexEntity){
                for(pawn in entity.getPawnsInside()){
                    pawn.translate(pawn.getVelocity())
                }
            }
        }

        // Do entity updates
        for (entity in entities) {
            entity.update(timestep)
        }
        entities.removeIf{it.markedForRemoval()}
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Renderer.Renderable>>) {
        for (entity in entities) {
            //TODO replace with processor call
                processRenderables(entity, {
                    modelDataMap[it.model]!!.add(it)
                })
        }
    }

    //TODO I don't like this pattern.
    fun getDebugLines(): List<DebugLineData> {
        val lines = mutableListOf<DebugLineData>()

        for (entity in entities) {
            val pos = entity.getCoordinates()
            val com = entity.getCenterOfMass().applyTransform(getTransformLocalToParentFrame(entity))
            lines.add(DebugLineData(com, pos, CYAN, BLUE))
//            lines.add(DebugLineData(com, com + entity.getVelocity()*10.0, BLUE, GREEN))
            for (force in entity.getLastForces()) {
                val fOrigin = force.origin.applyTransform(getTransformLocalToParentFrame(entity))
                val fEnd = (fOrigin + force.vector.rotate(getTransformLocalToParentFrame(entity).rotation) * 500.0)
//                lines.add(DebugLineData(fOrigin, fEnd, RED, WHITE))
            }
        }
        return lines
    }

    fun getDebugCircles(): List<DebugCircleData> {
        val circles = mutableListOf<DebugCircleData>()
        for (entity in entities) {
            val data = entity.getCrudeBoundingCircle()
            if(data != null){
                circles.add(DebugCircleData(Coordinates(data.center), data.radius, Renderer.ColorData(1.0f, 0.0f, 1.0f, 1.0f)))
            }
        }
        return circles
    }

    /**
     * List of entities that roughly overlap this point. Used in UI/Debug tools
     */
    fun getEntitiesAt(location: Vector2) : List<KinematicEntity>{
        return entities.filter { entity ->
            val circle = entity.getCrudeBoundingCircle()
            if(circle != null){
                return@filter circle.contains(location)
            }else{
                false
            }
        }
    }

//    fun getEntityClosestTo(location: Vector2) : ComplexEntity?{
//
//    }

    override fun addEntity(entity: KinematicEntity) {
        synchronized(entityBuffer){
            entityBuffer.add(entity)
            entity.setEntityConsumer(this)
        }
    }
}

data class Force<S : ReferenceFrame>(val vector: Vector2, val origin: Coordinates<S>) : ReferenceFrameVariable<S>{
    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): Force<S2> {
        return Force(this.vector.rotate(transform.rotation), this.origin.applyTransform(transform))
    }
}