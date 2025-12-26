import controllers.Controller
import effects.Effect
import models.Model
import effects.EffectsInput
import graphics.Graphics
import math.Coordinate
import math.Vector2
import math.WorldSpace
import physics.AbstractKinematicEntity
import physics.PhysicsInput
import physics.PhysicsOutput

data class DebugLineData(val p1: Coordinate<WorldSpace>, val p2: Coordinate<WorldSpace>, val colorData1: Graphics.ColorData, val colorData2: Graphics.ColorData)

interface Layer {
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>)
    fun getDebugLines() : List<DebugLineData>
}

interface PhysicsLayerI : Layer, EntityConsumer{
    fun update(input: PhysicsInput) : PhysicsOutput
}

interface EffectsLayerI : Layer, EffectsConsumer{
    fun update(input: EffectsInput)
}

interface EntityConsumer {
    fun addEntity(entity: AbstractKinematicEntity)
}

interface EffectsConsumer {
    fun addEffect(effect: Effect)
}

interface ControllerLayerI : Layer{
    fun update()
    fun <T: AbstractKinematicEntity> addControllerEntry(controller: Controller<T>, entity: T)
}
