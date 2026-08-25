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
import math.EntityReferenceFrame
import math.ReferenceFrame
import math.ReferenceFrameVariable
import math.Transform
import math.Vector2
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
                val fEnd = (fOrigin + force.vector * 500.0)
                lines.add(DebugLineData(fOrigin, fEnd, RED, WHITE))
            }
        }
        return lines
    }

    override fun addEntity(entity: KineticEntity) {
        world.addEntity(entity)
        entity.setEntityConsumer(this)
    }

    val world: World = FlatWorld() //In theory this is so I can replace this with non-flat worlds easily... Not sure about that...
}

data class Force<S : ReferenceFrame>(val vector: Vector2, val origin: Coordinates<S>) : ReferenceFrameVariable<S>{
    override fun <S2 : ReferenceFrame> applyTransform(transform: Transform<S, S2>): Force<S2> {
        return Force(this.vector.rotate(transform.rotation), this.origin.applyTransform(transform))
    }
}