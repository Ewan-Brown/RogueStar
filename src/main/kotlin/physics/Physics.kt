package physics

import PhysicsLayerI
import effects.Effect
import graphics.Graphics
import models.Model
import math.Vector2
import math.extruded
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
            for (renderableComponent in entity.getRenderableComponents()) {
                modelDataMap[renderableComponent.model]!!.add(renderableComponent)
            }
        }
    }

    override fun addEntity(entity: Entity) {
        world.addEntity(entity)
    }

    val world: World = FlatWorld()

    interface World{
        public fun update(input: PhysicsInput)
        public fun getEntities(): List<KinematicEntity>
        public fun addEntity(entity: KinematicEntity)
    }

    data class KinematicData(val velocity: Vector2, val rotationalVelocity: Double)

    private class FlatWorld : World {
        private val entities = mutableListOf<KinematicEntity>()

        override fun update(input : PhysicsInput) {

            // Do physics updates
            for (entity in entities) {
                entity.translate(entity.getVelocity().extruded(0.0) * input.timeStep)
                entity.rotate(entity.getRotationalVelocity() * input.timeStep)
                entity.setVelocity(entity.getVelocity()*0.99)
                entity.setRotationalVelocity(entity.getRotationalVelocity()*0.99)
            }

            // Do entity updates
            for (entity in entities) {
                entity.update(input.timeStep)
            }
            entities.removeIf{it.markedForRemoval()}

        }
        override fun getEntities(): List<KinematicEntity> {
            return entities
        }

        override fun addEntity(entity: KinematicEntity) {
            entities.add(entity)
        }
    }

}



