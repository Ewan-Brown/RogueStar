import Graphics
import org.dyn4j.geometry.Vector2
import Graphics.Model
import org.dyn4j.geometry.Vector3

sealed class EffectsRequest(val model: Model, val initialPosition: Vector3, val initialAngle: Double){
    class ExhaustRequest(initialPosition: Vector3, initialAngle: Double, val initialVelocity: Vector2) : EffectsRequest(Model.SQUARE, initialPosition, initialAngle);
}

data class EffectsInput(val input: List<EffectsRequest>, val timeStep: Double)

//TODO make use of ECS?
class EffectsLayer : Layer<EffectsInput, Unit> {
    private val entities = mutableListOf<EffectsEntity>()

    override fun update(input: EffectsInput) {
        val effectsRequests = input.input
        for (requests in effectsRequests) {
            when (requests) {
                is EffectsRequest.ExhaustRequest -> {
                    val exhaust = with(requests) { ExhaustEntity(model, initialPosition, initialAngle, initialVelocity) }
                    entities.add(exhaust)
                }
            }
        }
        for (entity in entities) {
            entity.update(input.timeStep)
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
    abstract fun update(timeStep: Double): Unit
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
        val transform = Transformation(position.copy().add(Vector3(0.0, 0.0, 1.0)), variableScale, rotation)
        return listOf(
            Graphics.RenderableEntity(
                model, transform, Graphics.ColorData(1.0f, 0.0f, 0.0f, 1.0f,), Graphics.MetaData(1.0f)
            )
        )

    }

    override fun update(timeStep: Double) {
        lifetime--
        position.add((velocity* timeStep).toVec3())
        rotation += drotation * getLife()
    }

    override fun isMarkedForRemoval(): Boolean {
        return lifetime < 0 || isDead
    }
}