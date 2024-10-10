import org.dyn4j.geometry.Vector2
import Graphics.Model
import org.dyn4j.geometry.Rotation

sealed class EffectsRequest(val model: Model, val initialPosition: Vector2, val initialAngle: Double){
    class ExhaustRequest(initialPosition: Vector2, initialAngle: Double, val initialVelocity: Vector2) : EffectsRequest(Model.SQUARE1, initialPosition, initialAngle);
}

class EffectsLayer : Layer {
    private val entities = mutableListOf<EffectsEntity>()

    fun update(effectsRequests: List<EffectsRequest>) {
        for (effect in effectsRequests) {
            when (effect) {
                is EffectsRequest.ExhaustRequest -> {
                    val exhaust = with(effect) {ExhaustEntity(model, initialPosition, initialAngle, initialVelocity) }
                    entities.add(exhaust)
                }
            }
        }
        for (entity in entities) {
            entity.update()
        }
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        entities.removeIf(EffectsEntity::isMarkedForRemoval)
        for (entity in entities) {
            for (component in entity.getComponents()) {
                modelDataMap[component.model]!!.add(Pair(component.transform, component.graphicalData))
            }
        }
    }
}

private abstract class EffectsEntity{
    abstract fun getComponents(): List<RenderableComponent>
    abstract fun update(): Unit
    abstract fun isMarkedForRemoval(): Boolean
}

private class BasicEffectEntity(val model: Model, var x : Double, var y : Double, var rotation : Double,
                             val graphicalData: GraphicalData, var dx : Double = 0.0, var dy : Double = 0.0, var drotation : Double = 0.0, var scale: Double = 1.0) : EffectsEntity() {

    private var isDead = false

    override fun getComponents(): List<RenderableComponent>{

        val entityPos = Vector2(x, y)
        return listOf(RenderableComponent(
            model,
            Transformation(entityPos, scale, rotation),
            graphicalData
        ))

    }
    override fun update() {
        x += dx
        y += dy
        rotation += drotation
    }
    override fun isMarkedForRemoval(): Boolean{
        return isDead
    }
}


private class ExhaustEntity(val model: Model, val position: Vector2, var rotation : Double, val velocity: Vector2, var drotation : Double = 0.0, var scale: Double = 1.0)
    : EffectsEntity() {

    private val MAX_LIFE: Int = 100
    private var lifetime: Int = MAX_LIFE
    private var isDead = false

    fun getLife(): Float {
        return (lifetime.toFloat() / MAX_LIFE.toFloat())
    }

    override fun getComponents(): List<RenderableComponent> {

        val variableScale = getLife().toDouble()
        if (variableScale < 0.01) {
            isDead = true
        }
        val transform = Transformation(position, variableScale, rotation)
        return listOf(
            RenderableComponent(
                model,
                transform,
                GraphicalData(getLife(),0.0f,0.0f,10-getLife()))
        )

    }

    override fun update() {
        lifetime--
        position.add(velocity)
        rotation += drotation * getLife()
    }

    override fun isMarkedForRemoval(): Boolean {
        return lifetime < 0 || isDead
    }
}