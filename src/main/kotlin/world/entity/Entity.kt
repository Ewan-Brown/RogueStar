package world.entity

import org.dyn4j.dynamics.Body
import org.dyn4j.geometry.Vector2
import world.rendering.Model

abstract class Entity : Body() {
    abstract fun getModels(): Set<Model>
    abstract fun getTransformedComponents(): Set<Component>
}

/**
 * An entity made up of an assumedly unique set of static components
 * This means we can't optimize for multiple entities sharing the same set of components (in which case we could amalgamate the components under an instanced draw call)
 * This also means the components will not move relative to the local coordinate space on the entity
 */
class StaticUniqueComponentEntity(private val components: Set<Component>) : Entity() {
    private val models = components.map { component -> component.model }.toSet()
    override fun getModels(): Set<Model> {return models}
    override fun getTransformedComponents(): Set<Component> {

        val entityAngle = this.getTransform().rotationAngle
        val entityPos = this.worldCenter

        val newComponents = components.map {
            val newPos = it.properties.pos.copy().rotate(entityAngle.toDouble()).add(entityPos)
            val newAngle = it.properties.angle + entityAngle;
            Component(it.model, ComponentProperties(newPos, newAngle))
        }.toSet()

        return newComponents;
    }

}

class ComponentProperties(var pos: Vector2, var angle: Double){
    val size = 3; //TODO Make this programmatic (# of floats for property)
}
class Component(val model: Model, val properties: ComponentProperties)