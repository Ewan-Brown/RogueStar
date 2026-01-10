package physics

import DebugLineData
import PhysicsLayerI
import effects.Effect
import graphics.GREEN
import graphics.Graphics
import graphics.RED
import graphics.WHITE
import math.Coordinates
import math.HasReferenceFrame
import math.EntityReferenceFrame
import math.Vector2
import math.WorldReferenceFrame
import math.getTransformLocalToParentFrame
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
            val pos = entity.getCoordinates()
            val com = entity.getCenterOfMass().applyTransform(getTransformLocalToParentFrame(entity))
            val velocity = entity.getVelocity()
            lines.add(DebugLineData(com, pos, RED, GREEN))
            lines.add(DebugLineData(pos, pos + entity.getVelocity()*10.0, RED, GREEN))
            for (force in entity.getLastForces()) {
                val fOrigin = force.origin.applyTransform(getTransformLocalToParentFrame(entity))
                val fEnd = fOrigin + force.vector * 500.0
                lines.add(DebugLineData(fOrigin, fEnd, RED, WHITE))
            }
        }
        return lines
    }

    override fun addEntity(entity: AbstractKinematicEntity) {
        world.addEntity(entity)
    }

    val world: World = FlatWorld()

    interface World : HasReferenceFrame<WorldReferenceFrame>{
        public fun update(input: PhysicsInput)
        public fun getEntities(): List<AbstractKinematicEntity>
        public fun addEntity(entity: AbstractKinematicEntity)
    }

    private class FlatWorld : World {
        private val entities = mutableListOf<AbstractKinematicEntity>()
        private val entityBuffer = mutableListOf<AbstractKinematicEntity>()

        override fun update(input : PhysicsInput) {

            synchronized(entityBuffer){
                entities.addAll(entityBuffer)
                entityBuffer.clear()
            }

            // Do physics updates
            for (entity in entities) {
                //Calculate new positions
                var velocity = entity.getVelocity()
                var rotVelocity = entity.getRotationalVelocity()
                entity.translate(velocity * input.timeStep)
                entity.rotate(rotVelocity * input.timeStep)

                val entityAngle = entity.getOrientation().getAngle()

                //Calculate new derivatives
                velocity = entity.getVelocity() + entity.checkNetForce().rotate(entityAngle)/entity.getMass()
                rotVelocity = entity.getRotationalVelocity() + entity.checkNetTorque()/entity.getMass()

                //Apply friction
                velocity *= 0.99
                rotVelocity *= 0.99
                entity.setVelocity(velocity)
                entity.setRotationalVelocity(rotVelocity)
            }

            for(projectile in entities.filterIsInstance<HasCollidingPoint>()){
                val pointOfContactLocal = projectile.getPointOfContact()
//                val pointOfContactWorld = pointOfContactLocal.rotate(projectile.getWorldOrientation().value) + projectile.getWorldTransform().translation
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
            synchronized(entityBuffer){
                entityBuffer.add(entity)
            }
        }
    }
}

data class Force(val vector: Vector2, val origin: Coordinates<EntityReferenceFrame>)