package world.rendering

import org.dyn4j.geometry.Vector2

interface Renderable {
    fun getRenderableElements() : List<RenderableElement>
    fun getWorldPos() : Vector2
    fun getAbsoluteAngle() : Double
}