package client

import Layer
import Transformation
import org.dyn4j.geometry.Vector2
import client.Graphics.Model
import org.dyn4j.geometry.Vector3
import toVec3

sealed class EffectsRequest(val model: Model, val initialPosition: Vector3, val initialAngle: Double){
    class ExhaustRequest(initialPosition: Vector3, initialAngle: Double, val initialVelocity: Vector2) : EffectsRequest(Model.SQUARE, initialPosition, initialAngle);
}

data class EffectsInput(val input: List<EffectsRequest>)

class EffectsLayer : Layer<EffectsInput, Unit> {
    private val entities = mutableListOf<EffectsEntity>()

    override fun update(input: EffectsInput) {
        val effectsRequests = input.input
        for (effect in effectsRequests) {
            when (effect) {
                is EffectsRequest.ExhaustRequest -> {
                    val exhaust = with(effect) { ExhaustEntity(model, initialPosition, initialAngle, initialVelocity) }
                    entities.add(exhaust)
                }
            }
        }
        for (entity in entities) {
            entity.update()
        }
        entities.removeIf(EffectsEntity::isMarkedForRemoval)
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.RenderableEntity>>) {
        for (entity in entities) {
            for (component in entity.getComponents()) {
                modelDataMap[component.model]!!.add(component)
            }
        }
    }
}

private abstract class EffectsEntity{
    abstract fun getComponents(): List<Graphics.RenderableEntity>
    abstract fun update(): Unit
    abstract fun isMarkedForRemoval(): Boolean
}

private class ExhaustEntity(val model: Model, val position: Vector3, var rotation : Double, val velocity: Vector2, var drotation : Double = 0.0, var scale: Double = 1.0)
    : EffectsEntity() {

    private val MAX_LIFE: Int = 100
    private var lifetime: Int = MAX_LIFE
    private var isDead = false

    fun getLife(): Float {
        return (lifetime.toFloat() / MAX_LIFE.toFloat())
    }

    override fun getComponents(): List<Graphics.RenderableEntity> {

        val variableScale = getLife().toDouble()
        if (variableScale < 0.01) {
            isDead = true
        }
        val transform = Transformation(position.copy(), variableScale, rotation)
        return listOf(
//            RenderableComponent(
//                model,
//                transform,
//                GraphicalData(getLife(),0.0f,0.0f,10-getLife()))
        )

    }

    override fun update() {
        lifetime--
        position.add(velocity.toVec3())
        rotation += drotation * getLife()
    }

    override fun isMarkedForRemoval(): Boolean {
        return lifetime < 0 || isDead
    }
}