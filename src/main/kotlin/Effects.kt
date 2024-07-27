import org.dyn4j.geometry.Vector2
import Graphics.Model
import Graphics.Transform
import java.util.function.Predicate

class EffectsLayer : Layer{
    private val entities = mutableListOf<EffectsEntity>();

    override fun update() {
        for (entity in entities) {
            entity.update();
        }
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Transform>>) {
        entities.removeIf(EffectsEntity::isMarkedForRemoval)
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
    abstract fun isMarkedForRemoval(): Boolean
}

class TangibleEffectsEntity(var x : Double, var y : Double, var rotation : Double,
                            val baseComponents: List<Component>, var dx : Double = 0.0, var dy : Double = 0.0, var drotation : Double = 0.0, var scale: Double = 1.0) : EffectsEntity() {

    var lifetime: Int = 0
    var isDead = false;

    override fun getComponents(): List<Component>{
        val components = mutableListOf<Component>()

        val entityAngle = rotation;
        val entityPos = Vector2(x, y)
        val variableScale = scale;
//        val variableScale = Math.min(1.0, Vector2(dx, dy).magnitude*10) * scale;
        if(variableScale < 0.01){
            isDead = true
        }

        for (baseComponent in baseComponents) {
            val m = baseComponent.model
            val t = baseComponent.transform

            val newPos = baseComponent.transform.position.copy().multiply(scale).rotate(entityAngle.toDouble()).add(entityPos).add(Vector2(1.0,1.0))
            val newAngle = entityAngle + baseComponent.transform.angle
            components.add(Component(baseComponent.model,Graphics.Transform(newPos, newAngle.toFloat(), baseComponent.transform.scale*variableScale.toFloat())))

        }

        return components
    }
    override fun update(): Unit{
        lifetime++
        x += dx
        y += dy
        rotation += drotation

        dx -= dx/100.0
        dy -= dy/100.0
        drotation -= drotation/100.0
//        isDead = lifetime > 5;
    }
    override fun isMarkedForRemoval(): Boolean{
        return lifetime > 1000 || isDead
    }
}