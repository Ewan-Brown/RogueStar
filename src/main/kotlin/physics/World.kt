package physics

import math.HasReferenceFrame
import math.WorldReferenceFrame

class FlatWorld : World {
    private val entities = mutableListOf<AbstractEntity>()
    private val entityBuffer = mutableListOf<AbstractEntity>()

    override fun update(input : PhysicsInput) {

        synchronized(entityBuffer){
            entities.addAll(entityBuffer)
            entityBuffer.clear()
        }

        // Do entity physics updates
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

        //Do pawn updates
        for(entity in entities) {
            for(pawn in entity.getPawnsInside()){
                pawn.translate(pawn.getVelocity())
//                pawn.setVelocity(Vector2())
            }
        }

        // Do entity updates
        for (entity in entities) {
            entity.update(input.timeStep)
        }
        entities.removeIf{it.markedForRemoval()}

    }
    override fun getEntities(): List<AbstractEntity> {
        return entities
    }

    override fun addEntity(entity: AbstractEntity) {
        synchronized(entityBuffer){
            entityBuffer.add(entity)
        }
    }
}

interface World : HasReferenceFrame<WorldReferenceFrame>{
    public fun update(input: PhysicsInput)
    public fun getEntities(): List<AbstractEntity>
    public fun addEntity(entity: AbstractEntity)
}