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

data class DebugLineData(val p1: Vector2, val p2: Vector2, val colorData: Graphics.ColorData)

interface Layer {
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>)
    fun getDebugLines() : List<DebugLineData>
}

interface PhysicsLayerI : Layer{
    fun update(input: PhysicsInput) : PhysicsOutput
    fun addEntity(entity: KinematicEntityImpl)
}

interface EffectsLayerI : Layer{
    fun update(input: EffectsInput)
    fun addEffect(effect: Effect)
}

interface ControllerLayerI : Layer{
    fun update()
    fun <T: EntityI> addControllerEntry(controller: Controller<T>, entity: T)
}