package physics

import graphics.BLUE
import graphics.CYAN
import graphics.DebugLineData
import graphics.GREEN
import graphics.Renderer
import graphics.RED
import graphics.WHITE
import graphics.processRenderables
import math.Coordinates
import math.HasReferenceFrame
import math.Pose
import math.ReferenceFrame
import math.ReferenceFrameVariable
import math.Transform
import math.Vector2
import math.WorldReferenceFrame
import math.getTransformLocalToParentFrame
import models.Model
import kotlin.collections.HashMap

interface EntityConsumer {
    fun addEntity(entity: KineticEntity)
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

    private val entities = mutableListOf<KineticEntity>()
    private val entityBuffer = mutableListOf<KineticEntity>()

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
            velocity *= 0.99
            rotVelocity *= 0.99
            entity.setVelocity(velocity)
            entity.setRotationalVelocity(rotVelocity)
        }

        for(projectile in entities){
            val collisionTriggerData = projectile.getCollisionTriggerData()
            when(collisionTriggerData){
                is LineCollisionTrigger -> println("line projectile interaction not defined!")
                is PointCollisionTrigger -> println("point projectile interaction not defined!")
                is RadiusCollisionTrigger -> TODO()
                is DisabledInteraction -> {}  // do nothing!
                null -> {} // do nothing!
            }
//            val pointOfContactLocal = projectile.getPointOfContact()
//            val pointOfContactWorld = pointOfContactLocal.rotate(projectile) + projectile.getWorldTransform().translation
//            for(entity in entities){
//                if(entity != projectile){
//                    //TODO Do cheap preliminary collision checking
//                }
//            }
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

    fun getDebugLines(): List<DebugLineData> {
        val lines = mutableListOf<DebugLineData>()

        for (entity in entities) {
            val pos = entity.getCoordinates()
            val com = entity.getCenterOfMass().applyTransform(getTransformLocalToParentFrame(entity))
            lines.add(DebugLineData(com, pos, CYAN, BLUE))
            lines.add(DebugLineData(com, com + entity.getVelocity()*10.0, BLUE, GREEN))
            for (force in entity.getLastForces()) {
                val fOrigin = force.origin.applyTransform(getTransformLocalToParentFrame(entity))
                val fEnd = (fOrigin + force.vector * 500.0)
                lines.add(DebugLineData(fOrigin, fEnd, RED, WHITE))
            }
        }
        return lines
    }

    override fun addEntity(entity: KineticEntity) {
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