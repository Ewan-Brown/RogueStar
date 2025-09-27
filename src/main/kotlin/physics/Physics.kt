package physics

import PhysicsLayerI
import effects.Effect
import graphics.Graphics
import models.Model
import math.Vector2
import math.getCollisionMTV
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
                if(!entity.isImmovable()){

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
            }

            val entitiesProcessed = mutableListOf<KinematicEntityI>()

            //Do collision updates
            for (entity1 in entities) {
                entitiesProcessed.add(entity1)
                for(entity2 in entities){
                    if(!entitiesProcessed.contains(entity2)) {
                        var smallest: Vector2? = null
                        for (p1 in entity1.getKinematicParts()) {
                            for (p2 in entity2.getKinematicParts()) {
                                val colliding = getCollisionMTV(p1.getPolygon(), p2.getPolygon())
                                if(colliding != null){
                                    println("mtv : $colliding")
                                    if(smallest == null || colliding.getMagnitude() < smallest.getMagnitude()) {
                                        smallest = colliding
                                    }
//                                    entity1.setVelocity(colliding)
//                                    entity2.setVelocity(colliding * -1.0)
//                                    entity1.setVelocity(Vector2(0.0, 0.0))
//                                    entity2.setVelocity(Vector2(0.0, 0.0))
//                                    entity1.setRotationalVelocity(0.0)
//                                    entity2.setRotationalVelocity(0.0)
                                }
                            }
                        }
                        if(smallest != null){
//                            println("smallest: $smallest")
//                            smallest *= 1.1
//                            if(!entity1.isImmovable()){
//                                entity1.translate(smallest)
//                            }
//                            if(!entity2.isImmovable()){
//                                entity2.translate(smallest * -1.0)
//                            }
//                            entity1.setVelocity(entity1.getVelocity() + smallest)
//                            entity2.setVelocity(entity2.getVelocity() + smallest * -1.0)
//                            println()
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



