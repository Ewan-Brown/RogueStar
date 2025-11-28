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
private class ExhaustEntity(val model: Model, val velocity: Vector2, private var transformation: Transformation3, var angularVelocity : Double = 0.0)
    : Effect {

    private val MAX_LIFE: Int = 100
    private var lifetime: Int = MAX_LIFE
    private var isDead = false

    fun getLife(): Float {
        return (lifetime.toFloat() / MAX_LIFE.toFloat())
    }

    override fun getRenderables(): List<Graphics.Renderable> {

        val variableScale = getLife().toDouble()
        if (variableScale < 0.01) {
            isDead = true
        }
        return listOf(
            Graphics.Renderable(
                model, transformation, Graphics.ColorData(1.0f, 0.0f, 0.0f, 1.0f,), Graphics.MetaData(1.0f)
            )
        )

    }

    override fun update(timeStep: Double) {
        lifetime--
        transformation.translation += Vector3(velocity * timeStep)
        transformation.rotation += angularVelocity * getLife()
    }

    override fun markedForRemoval(): Boolean {
        return lifetime < 0 || isDead
    }
}