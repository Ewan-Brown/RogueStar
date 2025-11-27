package physics

import PhysicsLayerI
import effects.Effect
import graphics.Graphics
import math.Polygon2
import models.Model
import math.Vector2
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

            for(projectile in entities.filterIsInstance<PointProjectileI>()){
                val pointOfContactLocal = projectile.getPointOfContact()
                val pointOfContactWorld = pointOfContactLocal.rotate(projectile.getWorldTransform().rotation) + projectile.getWorldTransform().translation
                for(entity in entities){
                    if(entity != projectile){
                        //TODO Do cheap preliminary collision checking
//                        for (part in entity.getParts()){
//                            if(part.)
//                            part.onDamage(1)
//                        }
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



