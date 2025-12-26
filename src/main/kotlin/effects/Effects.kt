package effects

import DebugLineData
import EffectsLayerI
import graphics.Graphics
import graphics.RED
import math.Transformation3
import models.Model
import math.Vector2
import math.Vector3
import math.getRandomSign
import java.awt.Color

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

//TODO Consider entities that don't incrementally update, that are formulaic rather than iterative?
class SimpleParticle(startPosition: Vector2, startVelocity: Vector2, startAngle: Double, startingAngularVelocity: Double, startLife: Int)
    : Effect {

    var position: Vector2 = startPosition
    var angle : Double = startAngle

    var velocity: Vector2 = startVelocity
    var angularVelocity : Double = startingAngularVelocity

    var life: Int = startLife
    var friction = 0.01;

    override fun getRenderables() : List<Graphics.Renderable> {
        return listOf(Graphics.Renderable(
            Model.SQUARE, Transformation3(Vector3(position, 10.0), angle, 0.3 ), RED, Graphics.MetaData(1.0f)
        ))
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