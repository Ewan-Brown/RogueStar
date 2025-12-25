import controllers.Controller
import effects.Effect
import models.Model
import effects.EffectsInput
import graphics.Graphics
import math.Vector2
import physics.KinematicEntityImpl
import physics.EntityI
import physics.PhysicsInput
import physics.PhysicsOutput

data class DebugLineData(val p1: Vector2, val p2: Vector2, val colorData1: Graphics.ColorData, val colorData2: Graphics.ColorData)

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
    fun addEntity(entity: KinematicEntityImpl)
}

interface EffectsConsumer {
    fun addEffect(effect: Effect)
}

interface ControllerLayerI : Layer{
    fun update()
    fun <T: EntityI> addControllerEntry(controller: Controller<T>, entity: T)
}
