import org.dyn4j.geometry.Vector2

class EffectsWorld{
    val entities = listOf<EffectsEntity>();
}

abstract class EffectsEntity : DrawableProvider{}

class SimpleEffectsEntity(
    var position: Vector2,
    var velocity: Vector2 = Vector2(0.0, 0.0),
    var angle: Float = 0.0f,
    var angularVelocity: Float = 0.0f,
    val model: Graphics.Model,
) : EffectsEntity(){
    override fun getDrawableInstances(): List<Component> {
        return return mutableListOf(Component(model, Graphics.Transform(position.copy(), angle)))
    }

}