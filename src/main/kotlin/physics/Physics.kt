package physics

import PhysicsLayerI
import effects.Effect
import graphics.Graphics
import models.Model
import math.Vector2
import math.doCollide
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

    override fun addEntity(entity: KinematicEntityImpl) {
        world.addEntity(entity)
    }

    val world: World = FlatWorld()

    interface World{
        public fun update(input: PhysicsInput)
        public fun getEntities(): List<KinematicEntityI>
        public fun addEntity(entity: KinematicEntityI)
    }

    data class KinematicData(val velocity: Vector2, val rotationalVelocity: Double)

    private class FlatWorld : World {
        private val entities = mutableListOf<KinematicEntityI>()

        override fun update(input : PhysicsInput) {

            // Do physics updates
            for (entity in entities) {
                //Calculate new positions
                entity.translate(entity.getVelocity().extruded(0.0) * input.timeStep)
                entity.rotate(entity.getRotationalVelocity() * input.timeStep)
                //Calcualte new derivatives
                entity.setVelocity(entity.getVelocity()*0.99)
                entity.setRotationalVelocity(entity.getRotationalVelocity()*0.99)
            }

            //Do collision updates
            for (entity1 in entities) {
                for(entity2 in entities){
                    if(entity1 != entity2) {
                        for (p1 in entity1.getKinematicParts()) {
                            for (p2 in entity2.getKinematicParts()) {
                                val colliding = doCollide(p1.getPolygon(), p2.getPolygon())
                                if(colliding){
                                    entity1.setVelocity(Vector2(0.0, 0.0))
                                    entity2.setVelocity(Vector2(0.0, 0.0))
                                    entity1.setRotationalVelocity(0.0)
                                    entity2.setRotationalVelocity(0.0)
                                }
                            }
                        }
                    }
                }
            }

            // Do entity updates
            for (entity in entities) {
                entity.update(input.timeStep)
            }
            entities.removeIf{it.markedForRemoval()}

        }
        override fun getEntities(): List<KinematicEntityI> {
            return entities
        }

        override fun addEntity(entity: KinematicEntityI) {
            entities.add(entity)
        }
    }

}



