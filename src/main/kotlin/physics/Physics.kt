package physics

import DebugLineData
import PhysicsLayerI
import effects.Effect
import graphics.GREEN
import graphics.Graphics
import graphics.RED
import graphics.WHITE
import math.Vector2
import models.Model
import kotlin.collections.HashMap

data class PhysicsInput(val timeStep: Double)
data class PhysicsOutput(val effects: List<Effect>)

class PhysicsLayer() : PhysicsLayerI{

    override fun update(input: PhysicsInput): PhysicsOutput {
        world.update(input)
        return PhysicsOutput(listOf())
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.Renderable>>) {
        for (entity in world.getEntities()) {
            for (renderableComponent in entity.getRenderables()) {
                modelDataMap[renderableComponent.model]!!.add(renderableComponent)
            }
        }
    }

    override fun getDebugLines(): List<DebugLineData> {
        val lines = mutableListOf<DebugLineData>()

        for (entity in world.getEntities()) {
            val pos = entity.getWorldTransform().translation
            val com = entity.getCenterOfMass()
            val velocity = entity.getVelocity()
            lines.add(DebugLineData(com, pos, RED, GREEN))
            lines.add(DebugLineData(pos, pos + entity.getVelocity()*10.0, RED, GREEN))
            for (force in entity.getLastForces()) {
                val fOrigin = force.origin
                val fEnd = force.vector*500.0 + fOrigin
                lines.add(DebugLineData(fOrigin, fEnd, RED, WHITE))
            }
        }
        return lines
    }

    override fun addEntity(entity: AbstractKinematicEntity) {
        world.addEntity(entity)
    }

    val world: World = FlatWorld()

    interface World{
        public fun update(input: PhysicsInput)
        public fun getEntities(): List<AbstractKinematicEntity>
        public fun addEntity(entity: AbstractKinematicEntity)
    }

    private class FlatWorld : World {
        private val entities = mutableListOf<AbstractKinematicEntity>()

        override fun update(input : PhysicsInput) {

            // Do physics updates
            for (entity in entities) {
                //Calculate new positions
                var velocity = entity.getVelocity()
                var rotVelocity = entity.getRotationalVelocity()
                entity.translate(velocity * input.timeStep)
                entity.rotate(rotVelocity * input.timeStep)

                //Calculate new derivatives
                velocity = entity.getVelocity() + entity.checkNetForce()/entity.getMass()
                rotVelocity = entity.getRotationalVelocity() + entity.checkNetTorque()/entity.getMass()

                //Apply friction
                velocity *= 0.99
                rotVelocity *= 0.99
                entity.setVelocity(velocity)
                entity.setRotationalVelocity(rotVelocity)
            }

            for(projectile in entities.filterIsInstance<HasCollidingPoint>()){
                val pointOfContactLocal = projectile.getPointOfContact()
                val pointOfContactWorld = pointOfContactLocal.rotate(projectile.getWorldTransform().rotation) + projectile.getWorldTransform().translation
                for(entity in entities){
                    if(entity != projectile){
                        //TODO Do cheap preliminary collision checking
                    }
                }
            }

            // Do entity updates
            for (entity in entities) {
                entity.update(input.timeStep)
            }
            entities.removeIf{it.markedForRemoval()}

        }
        override fun getEntities(): List<AbstractKinematicEntity> {
            return entities
        }

        override fun addEntity(entity: AbstractKinematicEntity) {
            entities.add(entity)
        }
    }
}

data class Force(val vector: Vector2, val origin: Vector2)