import controllers.Controller
import controllers.ControllerTarget
import effects.Effect
import models.Model
import effects.EffectsInput
import graphics.Graphics
import math.Coordinates
import math.WorldReferenceFrame
import physics.AbstractKinematicEntity
import physics.PhysicsInput
import physics.PhysicsOutput

data class DebugLineData(val p1: Coordinates<WorldReferenceFrame>, val p2: Coordinates<WorldReferenceFrame>, val colorData1: Graphics.ColorData, val colorData2: Graphics.ColorData)

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

interface ControllerConsumer {
    fun <T: ControllerTarget> addControllerEntry(controller: Controller<T>, target: T)
}

interface ControllerLayerI : Layer, ControllerConsumer{
    fun update()
}
