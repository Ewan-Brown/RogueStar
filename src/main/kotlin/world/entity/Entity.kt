package world.entity

import org.dyn4j.dynamics.Body
import org.dyn4j.geometry.Vector2
import world.rendering.Model

abstract class Entity : Body() {
    abstract fun getComponents(): List<Component>
}

class Component(val model: Model, var localPos: Vector2, var localAngle: Float)