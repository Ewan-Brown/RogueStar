import org.dyn4j.geometry.Vector2
import Graphics.Model
import Graphics.Transform
import java.util.function.Predicate

class EffectsUtils{
    companion object {
        fun emitThrustParticles(entity: PhysicsEntity, thrust: Vector2) {
            if (thrust.magnitude > 0) {
                val adjustedThrust = thrust.product(-0.002).rotate((Math.random() - 0.5) / 3)
                effectsLayer.addEntity(
                    TangibleEffectsEntity(
                        entity.worldCenter.x,
                        entity.worldCenter.y,
                        Math.random(),
                        listOf(
                            Component(Model.SQUARE2, Transform(Vector2(0.0, 0.0), 0f, 0.2f, 1.0f, 0.0f, 0.0f, 1.0f))
                        ),
                        dx = entity.linearVelocity.x / 100.0 + adjustedThrust.x,
                        dy = entity.linearVelocity.y / 100.0 + adjustedThrust.y,
                        drotation = (Math.random() - 0.5) * 10
                    )
                )
            }
        }
    }
}

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
        val variableScale = Math.min(1.0, Vector2(dx, dy).magnitude*10) * scale;
        if(variableScale < 0.01){
            isDead = true
        }

        for (baseComponent in baseComponents) {
            val m = baseComponent.model
            val t = baseComponent.transform

            val newPos = t.position.copy().multiply(scale).rotate(entityAngle).add(entityPos)
            val newAngle = entityAngle + t.angle
            components.add(Component(baseComponent.model,
                Transform(newPos, newAngle.toFloat(), t.scale*variableScale.toFloat(), t.red, t.green, t.blue, t.alpha)
            ))

        }

        return components
    }
    override fun update() {
        lifetime++
        x += dx
        y += dy
        rotation += drotation

        dx -= dx/100.0
        dy -= dy/100.0
        drotation -= drotation/100.0
    }
    override fun isMarkedForRemoval(): Boolean{
        return lifetime > 1000 || isDead
    }
}