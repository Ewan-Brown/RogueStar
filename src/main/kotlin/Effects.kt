import org.dyn4j.geometry.Vector2
import Graphics.Model
import Graphics.Transform

class EffectsLayer : Layer{
    private val entities = mutableListOf<EffectsEntity>();

    init {
        val testEntity = SimpleEffectsEntity(Vector2(0.0,0.0), model = Model.TRIANGLE)
        addEntity(testEntity)
    }

    override fun update() {
//        TODO("Not yet implemented")
    }

    override fun populateModelMap(modelDataMap: HashMap<Graphics.Model, MutableList<Graphics.Transform>>) {
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
}

class SimpleEffectsEntity(
    var position: Vector2,
    var velocity: Vector2 = Vector2(0.0, 0.0),
    var angle: Float = 0.0f,
    var angularVelocity: Float = 0.0f,
    val model: Graphics.Model,
) : EffectsEntity(){

    override fun getComponents(): List<Component> {
        return return mutableListOf(Component(model, Graphics.Transform(position.copy(), angle)))
    }

}