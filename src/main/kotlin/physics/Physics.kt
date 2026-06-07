package physics

import effects.Effect
import graphics.BLUE
import graphics.CYAN
import graphics.DebugLineData
import graphics.GREEN
import graphics.Renderer
import graphics.RED
import graphics.WHITE
import graphics.processRenderables
import math.Coordinates
import math.EntityReferenceFrame
import math.Vector2
import math.getTransformLocalToParentFrame
import models.Model
import kotlin.collections.HashMap

interface EntityConsumer {
    fun addEntity(entity: Entity)
}

@JvmInline
value class Timestamp(val time: Double){
    operator fun minus(t2 : Timestamp) : TimeDuration{
        return TimeDuration(time - t2.time)
    }
}
@JvmInline
value class TimeDuration(val duration: Double)

class PhysicsManager() : EntityConsumer {
    fun update(timeStep: Double) {
        world.update(timeStep)
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Renderer.Renderable>>) {
        for (entity in world.getEntities()) {
            //TODO replace with processor call
                processRenderables(entity, {
                    modelDataMap[it.model]!!.add(it)
                })
        }
    }

    fun getDebugLines(): List<DebugLineData> {
        val lines = mutableListOf<DebugLineData>()

        for (entity in world.getEntities()) {
            val pos = entity.getCoordinates()
            val com = entity.getCenterOfMass().applyTransform(getTransformLocalToParentFrame(entity))
            lines.add(DebugLineData(com, pos, CYAN, BLUE))
            lines.add(DebugLineData(com, com + entity.getVelocity()*10.0, BLUE, GREEN))
            for (force in entity.getLastForces()) {
                val fOrigin = force.origin.applyTransform(getTransformLocalToParentFrame(entity))
                val fEnd = fOrigin + force.vector.rotate(entity.getOrientation().getAngle()) * 500.0
                lines.add(DebugLineData(fOrigin, fEnd, RED, WHITE))
            }
        }
        return lines
    }

    override fun addEntity(entity: Entity) {
        world.addEntity(entity)
    }

    val world: World = FlatWorld() //In theory this is so I can replace this with non-flat worlds easily... Not sure about that...
}

data class Force(val vector: Vector2, val origin: Coordinates<EntityReferenceFrame>)