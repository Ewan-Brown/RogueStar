import org.dyn4j.geometry.Vector2
import Graphics.Model
import org.dyn4j.geometry.Rotation

class EffectsUtils{
    companion object {
        fun emitThrustParticles(data: PhysicsLayer.PhysicsBodyData, thrust: Vector2) {
            if (thrust.magnitude > 0) {
                val adjustedThrust = thrust.product(-0.0002).rotate((Math.random() - 0.5) / 3)
                effectsLayer.addEntity(
                    ParticleEntity(
                        Model.SQUARE1,
                        data.position.x,
                        data.position.y,
                        Math.random(),
                        graphicsDataProvider = { GraphicalData(it.getLife(),0.0f,0.0f,10-it.getLife()) },
                                dx = data.changeInPosition.x + adjustedThrust.x
                        ,dy = data.changeInPosition.y + adjustedThrust.y,
                        drotation = (Math.random() - 0.5)
                    )
                )
            }
        }
        fun debris(data: PhysicsLayer.PhysicsBodyData, partinfo: PhysicsLayer.PartInfo){
            partinfo.renderableProducer()?.let{ renderableData ->
                val newPos = renderableData.transform.position.copy().rotate(data.angle).add(data.position)
                val newAngle = Rotation(renderableData.transform.rotation.toRadians() + data.angle)
                val scale = renderableData.transform.scale
                val graphData = renderableData.graphicalData;

                effectsLayer.addEntity(
                    BasicEffectEntity(
                        renderableData.model,
                        newPos.x,
                        newPos.y,
                        newAngle.toRadians(),
                        GraphicalData(graphData.red*0.5f, graphData.green*0.5f, graphData.blue*0.5f, graphData.z, graphData.health),
                        data.changeInPosition.x,
                        data.changeInPosition.y,
                        data.changeInOrientation, scale = scale
                    )
                )
            }
        }
    }
}

class EffectsLayer : Layer {
    private val entities = mutableListOf<EffectsEntity>()

    override fun update() {
        for (entity in entities) {
            entity.update()
        }
    }

    fun populateModelMap(modelDataMap: HashMap<Model, MutableList<Pair<Transformation, GraphicalData>>>) {
        entities.removeIf(EffectsEntity::isMarkedForRemoval)
        for (entity in entities) {
            for (component in entity.getComponents()) {
                modelDataMap[component.model]!!.add(Pair(component.transform, component.graphicalData))
            }
        }
    }

    fun addEntity(entity : EffectsEntity){
        entities.add(entity)
    }
}

abstract class EffectsEntity{
    abstract fun getComponents(): List<RenderableComponent>
    abstract fun update(): Unit
    abstract fun isMarkedForRemoval(): Boolean
}


class BasicEffectEntity(val model: Model, var x : Double, var y : Double, var rotation : Double,
                             val graphicalData: GraphicalData, var dx : Double = 0.0, var dy : Double = 0.0, var drotation : Double = 0.0, var scale: Double = 1.0) : EffectsEntity() {

    private var isDead = false

    override fun getComponents(): List<RenderableComponent>{

        val entityPos = Vector2(x, y)
        return listOf(RenderableComponent(
            model,
            Transformation(entityPos, scale, rotation),
            graphicalData
        ))

    }
    override fun update() {
        x += dx
        y += dy
        rotation += drotation
    }
    override fun isMarkedForRemoval(): Boolean{
        return isDead
    }
}


class ParticleEntity(val model: Model, var x : Double, var y : Double, var rotation : Double,
                     val graphicsDataProvider: (ParticleEntity) -> GraphicalData, var dx : Double = 0.0, var dy : Double = 0.0, var drotation : Double = 0.0, var scale: Double = 1.0)
    : EffectsEntity() {

    private val MAX_LIFE: Int = 100
    private var lifetime: Int = MAX_LIFE
    private var isDead = false

    fun getLife(): Float {
        return (lifetime.toFloat() / MAX_LIFE.toFloat())
    }

    override fun getComponents(): List<RenderableComponent> {

        val entityPos = Vector2(x, y)
        val variableScale = getLife().toDouble()
        if (variableScale < 0.01) {
            isDead = true
        }
        return listOf(
            RenderableComponent(
                model,
                Transformation(entityPos, variableScale, rotation),
                graphicsDataProvider(this)
            )
        )

    }

    override fun update() {
        lifetime--
        x += dx * getLife()
        y += dy * getLife()
        rotation += drotation * getLife()
    }

    override fun isMarkedForRemoval(): Boolean {
        return lifetime < 0 || isDead
    }
}