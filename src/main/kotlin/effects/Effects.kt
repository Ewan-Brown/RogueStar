package effects

import DebugLineData
import EffectsLayerI
import graphics.Graphics
import models.Model
import math.Transformation3
import math.Vector2
import math.Vector3

data class EffectsInput(val timeStep: Double)

class EffectsLayer : EffectsLayerI{
    private val entities = mutableListOf<Effect>()

    override fun update(input: EffectsInput) {
        for (entity in entities) {
            entity.update(input.timeStep)
        }
        entities.removeIf(Effect::markedForRemoval)
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {
        for (entity in entities) {
            for (renderable in entity.getRenderables()) {
                modelDataMap[renderable.model]!!.add(renderable)
            }
        }
    }

    override fun getDebugLines(): List<DebugLineData> {
        return listOf()
    }

    override fun addEffect(effect: Effect) {
        entities.add(effect)
    }
}

interface Effect{
    fun getRenderables(): List<Graphics.Renderable>
    fun update(timeStep: Double): Unit
    fun markedForRemoval(): Boolean
}

//TODO What about entities that can save on resources by not needing updates, rather just calculating their transformation when called?
private class ExhaustEntity(val velocity: Vector2, var angularVelocity : Double = 0.0)
    : Effect {
    override fun getRenderables(): List<Graphics.Renderable> {
        TODO("Not yet implemented")
    }

    override fun update(timeStep: Double) {
        TODO("Not yet implemented")
    }

    override fun markedForRemoval(): Boolean {
        TODO("Not yet implemented")
    }
}