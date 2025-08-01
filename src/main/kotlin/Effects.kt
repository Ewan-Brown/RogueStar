import org.dyn4j.geometry.Vector2
import Graphics.Model
import org.dyn4j.geometry.Vector3

sealed class EffectsRequest(val model: Model, val transformation: Transformation){
    class ExhaustRequest(transformation: Transformation, val initialVelocity: Vector2) : EffectsRequest(Model.SQUARE, transformation);
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
                    val exhaust = with(requests) { ExhaustEntity(model, initialVelocity, transformation) }
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

private class ExhaustEntity(val model: Model, val velocity: Vector2, transformation: Transformation,  var angularVelocity : Double = 0.0)
    : EffectsEntity() {

    private val MAX_LIFE: Int = 100
    private var lifetime: Int = MAX_LIFE
    private var isDead = false
    private var position: Vector3 = transformation.translation
    private var rotation = transformation.rotation.toRadians()
    private var scale = transformation.scale


    fun getLife(): Float {
        return (lifetime.toFloat() / MAX_LIFE.toFloat())
    }

    override fun getComponents(): List<Graphics.RenderableEntity> {

        val variableScale = getLife().toDouble()
        if (variableScale < 0.01) {
            isDead = true
        }
        return listOf(
            Graphics.RenderableEntity(
                model, Transformation(position.copy(), rotation, scale), Graphics.ColorData(1.0f, 0.0f, 0.0f, 1.0f,), Graphics.MetaData(1.0f)
            )
        )

    }

    override fun update(timeStep: Double) {
        lifetime--
        position.add((velocity* timeStep).toVec3())
        rotation += angularVelocity * getLife()
    }

    override fun isMarkedForRemoval(): Boolean {
        return lifetime < 0 || isDead
    }
}