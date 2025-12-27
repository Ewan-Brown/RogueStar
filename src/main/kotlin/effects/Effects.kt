package effects

import DebugLineData
import EffectsLayerI
import graphics.Graphics
import graphics.RED
import math.Coordinates
import math.Orientation
import models.Model
import math.Vector2
import math.WorldSpace
import math.ZHeight

data class EffectsInput(val timeStep: Double)

class EffectsLayer : EffectsLayerI{
    private val effects = mutableListOf<Effect>()
    private val effectsBuffer = mutableListOf<Effect>()

    override fun update(input: EffectsInput) {
        synchronized(effectsBuffer){
            effects.addAll(effectsBuffer)
            effectsBuffer.clear()
        }
        for (effect in effects) {
            effect.update(input.timeStep)
        }
        effects.removeIf(Effect::markedForRemoval)
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {
        for (entity in effects) {
            for (renderable in entity.getRenderables()) {
                modelDataMap[renderable.model]!!.add(renderable)
            }
        }
    }

    override fun getDebugLines(): List<DebugLineData> {
        return listOf()
    }

    override fun addEffect(effect: Effect) {
        synchronized(effectsBuffer){
            effectsBuffer.add(effect)
        }
    }
}

interface Effect{
    fun getRenderables(): List<Graphics.Renderable>
    fun update(timeStep: Double): Unit
    fun markedForRemoval(): Boolean
}

//TODO Consider entities that don't incrementally update, that are formulaic rather than iterative?
class SimpleParticle(startPosition: Coordinates<WorldSpace>, startVelocity: Vector2, startAngle: Orientation<WorldSpace>, startingAngularVelocity: Double, startLife: Int)
    : Effect {

    var position: Vector2 = startPosition.getVector()
    var angle : Double = startAngle.getAngle()

    var velocity: Vector2 = startVelocity
    var angularVelocity : Double = startingAngularVelocity

    var life: Int = startLife
    var friction = 0.01;

    override fun getRenderables() : List<Graphics.Renderable> {
        return listOf(Graphics.Renderable(
            Model.SQUARE,
            Coordinates(position),
            Orientation(angle),
            ZHeight(10.0),
            0.3,
            RED,
            Graphics.MetaData(1.0f)))
    }

    override fun update(timeStep: Double) {
        position += velocity * timeStep
        angle += angularVelocity * timeStep

        velocity *= (1.0 - friction * timeStep)
        angularVelocity *= (1.0 - friction * timeStep)

        life--
    }

    override fun markedForRemoval(): Boolean {
        return life <= 0;
    }
}