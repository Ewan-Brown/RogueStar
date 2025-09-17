import controllers.Controller
import effects.Effect
import models.Model
import effects.EffectsInput
import graphics.Graphics
import physics.Entity
import physics.EntityI
import physics.PhysicsInput
import physics.PhysicsOutput

interface PhysicsLayerI {
    fun update(input: PhysicsInput) : PhysicsOutput
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>)
    fun addEntity(entity: Entity)
}

interface EffectsLayerI {
    fun update(input: EffectsInput)
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>)
    fun addEffect(effect: Effect)
}

interface ControllerLayerI{
    fun update()
    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>)
    fun <T: EntityI> addControllerEntry(controller: Controller<T>, entity: T)
}