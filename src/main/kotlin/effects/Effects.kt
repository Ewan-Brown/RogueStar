package effects

import graphics.Renderer
import graphics.RED
import main.Timestamp
import math.Coordinates
import math.InReferenceFrame
import math.Orientation
import models.Model
import math.Vector2
import math.WorldReferenceFrame
import math.ZHeight
import physics.Entity

interface EffectsConsumer {
    fun addEffect(effect: Effect)
}

class EffectsManager : EffectsConsumer {
    private val effects = mutableListOf<Effect>()
    private val effectsBuffer = mutableListOf<Effect>()

    fun update(timeStep: Double) {
        //FIXME Why synchronized...?
        synchronized(effectsBuffer){
            effects.addAll(effectsBuffer)
            effectsBuffer.clear()
        }
        for (effect in effects) {
            effect.update(timeStep)
        }
        effects.removeIf(Effect::markedForRemoval)
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Renderer.Renderable>>) {
        for (entity in effects) {
            for (renderable in entity.getRenderables()) {
                modelDataMap[renderable.model]!!.add(renderable)
            }
        }
    }

    override fun addEffect(effect: Effect) {
        //FIXME Why synchronized...?
        synchronized(effectsBuffer){
            effectsBuffer.add(effect)
        }
    }
}

interface Effect : InReferenceFrame<WorldReferenceFrame>{
    fun getRenderables(): List<Renderer.Renderable>
    fun update(timeStep: Double): Unit
    fun markedForRemoval(): Boolean
}

//TODO Consider entities that don't incrementally update, that are formulaic rather than iterative?
class SimpleParticle(var position: Coordinates<WorldReferenceFrame>, startVelocity: Vector2, var angle: Orientation<WorldReferenceFrame>, startingAngularVelocity: Double, startLife: Int)
    : Effect {

    var velocity: Vector2 = startVelocity
    var angularVelocity : Double = startingAngularVelocity

    var life: Int = startLife
    var friction = 0.01;

    override fun getRenderables() : List<Renderer.Renderable> {
        return listOf(Renderer.Renderable(
            Model.SQUARE,
            position.getVector(),
            angle.getAngle(),
            10.0,
            0.3,
            RED,
            Renderer.MetaData(1.0f)))
    }

    override fun update(timeStep: Double) {
        position += velocity * timeStep
        angle += angularVelocity * timeStep

        velocity *= (1.0 - friction * timeStep)
        angularVelocity *= (1.0 - friction * timeStep)

        life--
    }

    override fun markedForRemoval(): Boolean {
        return life <= 0;
    }

    override fun getCoordinates(): Coordinates<WorldReferenceFrame> {
        return position
    }

    override fun getOrientation(): Orientation<WorldReferenceFrame> {
        return angle
    }

    override fun getZHeight(): ZHeight<WorldReferenceFrame> {
        return getZHeight()
    }
}