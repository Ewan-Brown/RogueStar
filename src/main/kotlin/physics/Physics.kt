package physics

import DebugLineData
import PhysicsLayerI
import effects.Effect
import graphics.BLUE
import graphics.CYAN
import graphics.GREEN
import graphics.Graphics
import graphics.RED
import graphics.WHITE
import graphics.processRenderables
import math.Coordinates
import math.EntityReferenceFrame
import math.Vector2
import math.getTransformLocalToParentFrame
import models.Model
import kotlin.collections.HashMap

data class PhysicsInput(val timeStep: Double)
data class PhysicsOutput(val effects: List<Effect>)

class PhysicsLayer() : PhysicsLayerI{
    override fun update(input: PhysicsInput): PhysicsOutput {
        world.update(input)
        return PhysicsOutput(listOf())
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {
        for (entity in world.getEntities()) {
            //TODO replace with processor call
                processRenderables(entity, {
                    modelDataMap[it.model]!!.add(it)
                })
        }
    }

    override fun getDebugLines(): List<DebugLineData> {
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

    val world: World = FlatWorld()
}

data class Force(val vector: Vector2, val origin: Coordinates<EntityReferenceFrame>)