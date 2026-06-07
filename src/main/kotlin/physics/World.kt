package physics

import math.Coordinates
import math.HasReferenceFrame
import math.Pose
import math.WorldReferenceFrame
import math.getTransformLocalToParentFrame

class FlatWorld : World {
    private val entities = mutableListOf<Entity>()
    private val entityBuffer = mutableListOf<Entity>()

    override fun update(timestep: Double) {

        synchronized(entityBuffer){
            entities.addAll(entityBuffer)
            entityBuffer.clear()
        }

        // Do entity physics updates
        for (entity in entities) {
            //Calculate new positions
            var velocity = entity.getVelocity()
            var rotVelocity = entity.getRotationalVelocity()

            val comLocal = entity.getCenterOfMass()
            var comWorld = comLocal.applyTransform(getTransformLocalToParentFrame(entity))

            entity.rotate(rotVelocity * timestep)
            comWorld += velocity * timestep

            entity.setPose(Pose(Coordinates(comWorld.getVector() - comLocal.getVector().rotate(entity.getOrientation().getAngle())), entity.getOrientation(), entity.getZHeight()))

            val entityAngle = entity.getOrientation().getAngle()

            //Calculate new derivatives
            velocity = entity.getVelocity() + entity.checkAndResetNetForce().rotate(entityAngle)/entity.getMass()
            rotVelocity = entity.getRotationalVelocity() + entity.checkAndResetNetTorque()/entity.getMass()

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
            entity.update(timestep)
        }
        entities.removeIf{it.markedForRemoval()}

    }
    override fun getEntities(): List<Entity> {
        return entities
    }

    override fun addEntity(entity: Entity) {
        synchronized(entityBuffer){
            entityBuffer.add(entity)
        }
    }
}

interface World : HasReferenceFrame<WorldReferenceFrame>{
    public fun update(timestep: Double)
    public fun getEntities(): List<Entity>
    public fun addEntity(entity: Entity)
}