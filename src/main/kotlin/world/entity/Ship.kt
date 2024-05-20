package world.entity

import org.dyn4j.geometry.Geometry
import org.dyn4j.geometry.Vector2
import world.rendering.Renderable
import world.rendering.RenderableElement

class Ship(private val points : List<Vector2>) : Renderable{
    val centroid : Vector2 = Geometry.getAverageCenter(points)
    val normalizedPoints : List<Vector2> = points.map { vector2 ->  vector2.subtract(centroid)}

    fun getPointsAsFloats() : List<Float>{
        val floats = emptyList<Float>().toMutableList()
        normalizedPoints.iterator().forEach {
            floats.add(it.x.toFloat())
            floats.add(it.y.toFloat())
            floats.add(0.0f)
        }
        return floats
    }

    fun getPos_temp() : Vector2 {
        return Vector2(0.0, 0.0)
    }

    fun getAngle_temp() : Double{
        return 0.0
    }

    override fun getRenderableElements(): List<RenderableElement> {
        TODO("Not yet implemented")
    }

    override fun getWorldPos(): Vector2 {
        TODO("Not yet implemented")
    }

    override fun getAbsoluteAngle(): Double {
        TODO("Not yet implemented")
    }
}