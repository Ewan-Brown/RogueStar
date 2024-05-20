package world.rendering

import org.dyn4j.geometry.Vector2

abstract class RenderableElement {
    abstract fun getMaterial() : Material
    abstract fun getAngle() : Double
    abstract fun getRelativePos() : Vector2
    abstract fun getPoints() : List<Vector2>
}