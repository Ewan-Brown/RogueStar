import Graphics.Model
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import java.util.UUID
import kotlin.collections.HashMap
import kotlin.math.PI

//data class ComponentDefinition(val model : Model, val localTransform: Transformation)

data class PhysicsInput(val map : Map<UUID, List<ControlCommand>>, val timeStep: Double)
data class PhysicsOutput(val requests: List<EffectsRequest>)


class PhysicsLayer(val models: List<Model>) : Layer<PhysicsInput, PhysicsOutput> {

    override fun update(input: PhysicsInput): PhysicsOutput {
        world.update(input)
        return PhysicsOutput(listOf())
    }

    override fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Graphics.RenderableEntity>>) {
        world.populateModelMap(modelDataMap)
    }

    val world = FlatWorld()

    //A world, that is flat
    class FlatWorld{
        private val entities = mutableListOf<Entity>(
            TestEntity(
                Transformation3(
                    Vector3(0.0, 0.0, 0.0),
                    0.0,
                    1.0),
            KinematicData(Vector2(0.1, 0.0), 0.0)
        ))

        fun update(input : PhysicsInput) {

            // Do physics updates
            for (entity in entities) {
                entity.updatePhysics(input.timeStep)
            }

            // Do entity updates
            for (entity in entities) {
                entity.update(input.timeStep)
            }
            entities.removeIf{it.markedForRemoval()}

        }

        fun populateModelMap(modelDataMap: java.util.HashMap<Model, MutableList<Graphics.RenderableEntity>>) {
            for (entity in entities) {
                for (renderableComponent in entity.getRenderableComponents()) {
                    modelDataMap[renderableComponent.model]!!.add(renderableComponent)
                }
            }
        }

    }

    data class KinematicData(val velocity: Vector2, val rotationalVelocity: Double)

    abstract class Entity(transform: Transformation3, kinematicData: KinematicData){

        private var position = transform.translation
        private var rotation = transform.rotation
        private var scale = transform.scale
        private var velocity = kinematicData.velocity
        private var rotationalVelocity = kinematicData.rotationalVelocity

        fun getGlobalTransform(): Transformation3 {
            return Transformation3(position, rotation, scale)
        }

        fun getVelocity() = velocity
        fun getRotationalVelocity() = rotationalVelocity

        abstract fun getEntityComponents() : List<EntityComponent>

        abstract fun update(timeStep: Double)

        fun updatePhysics(timeStep: Double) {
            position += getVelocity().extruded(0.0) * timeStep
        }

        fun getRenderableComponents() : List<Graphics.RenderableEntity> {
            val getFinalTransform = fun(localTransform : Transformation3) : Transformation3 {
                val finalTranslation = getGlobalTransform().translation + (localTransform.translation * getGlobalTransform().scale).rotate(getGlobalTransform().rotation)
                val finalRotation = getGlobalTransform().rotation + localTransform.rotation
                val finalScale = getGlobalTransform().scale * localTransform.scale
                return Transformation3(finalTranslation, finalRotation, finalScale)
            }
            return getEntityComponents().map {Graphics.RenderableEntity(it.getModel(), getFinalTransform(it.getLocalTransform()), it.getColor(), it.getMetadata())}
        }
        abstract fun markedForRemoval() : Boolean
    }

    interface EntityComponent{
        fun getLocalTransform() : Transformation3
        fun getModel() : Model
        fun getColor() : Graphics.ColorData
        fun getMetadata() : Graphics.MetaData
    }

    class TestEntity(transform: Transformation3, kinematicData: KinematicData) : Entity(transform, kinematicData) {
        private val localComponents: List<EntityComponent> = listOf(TestComponent(Transformation3(Vector3(0.0, 0.0, 0.0), 0.0, 1.0)))
        override fun getEntityComponents(): List<EntityComponent> {
            return localComponents
        }

        override fun update(timeStep: Double) {

        }

        override fun markedForRemoval(): Boolean {
            return false
        }
    }

    class TestComponent(private val localTransform: Transformation3) : EntityComponent {
        override fun getLocalTransform(): Transformation3 {
            return localTransform
        }

        override fun getModel(): Model {
            return Model.SQUARE
        }

        override fun getColor(): Graphics.ColorData {
            return Graphics.ColorData(1.0f, 0.0f, 1.0f, 1.0f)
        }

        override fun getMetadata(): Graphics.MetaData {
            return Graphics.MetaData(1.0f)
        }

    }
}

