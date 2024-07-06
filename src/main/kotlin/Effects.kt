import org.dyn4j.geometry.Vector2
import Graphics.Model
import Graphics.Transform

class EffectsLayer : Layer{
    private val entities = mutableListOf<EffectsEntity>();

    override fun update() {
        for (entity in entities) {
            entity.update();
        }
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>) {
        for (entity in entities) {
            for (component in entity.getComponents()) {
                modelDataMap[component.model]!!.add(component.transform)
            }
        }
    }

    fun addEntity(entity : EffectsEntity){
        entities.add(entity);
    }
}

abstract class EffectsEntity{
    abstract fun getComponents(): List<Component>
    abstract fun update(): Unit
}

/**
 * Shoots off the screen at a random speed!
 */
class FleeingEffectEntity(
    var position: Vector2,
    var velocity: Vector2 = Vector2(0.0, 0.0),
    var angle: Float = 0.0f,
    var angularVelocity: Float = 0.0f,
    val model: Model,
) : EffectsEntity(){

    private val speed = Vector2(Math.random() - 0.5, Math.random() - 0.5).multiply(0.2)

    override fun getComponents(): List<Component> {
        return mutableListOf(Component(model, Transform(position.copy(), angle)))
    }

    override fun update() {
        position.add(speed);
    }

}