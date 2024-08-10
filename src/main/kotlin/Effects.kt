import org.dyn4j.geometry.Vector2
import Graphics.Model

class EffectsUtils{
    companion object {
        fun emitThrustParticles(entity: PhysicsEntity, thrust: Vector2) {
            if (thrust.magnitude > 0) {
                val adjustedThrust = thrust.product(-0.01).rotate((Math.random() - 0.5) / 3)
                effectsLayer.addEntity(
                    ParticleEntity(
                        Model.SQUARE1,
                        entity.worldCenter.x,
                        entity.worldCenter.y,
                        Math.random(),
                        graphicsDataProvider = {it -> GraphicalData(it.getLife(),0.0f,0.0f,-10.0f)},
                                dx = entity.changeInPosition.x + adjustedThrust.x
                        ,dy = entity.changeInPosition.y + adjustedThrust.y,
                        //drotation = (Math.random() - 0.5) * 10
                    )
                )
            }
        }
    }
}

class EffectsLayer : Layer{
    private val entities = mutableListOf<EffectsEntity>();

    override fun update() {
        for (entity in entities) {
            entity.update();
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
        entities.add(entity);
    }
}

abstract class EffectsEntity{
    abstract fun getComponents(): List<RenderableComponent>
    abstract fun update(): Unit
    abstract fun isMarkedForRemoval(): Boolean
}

class ParticleEntity(val model: Model, var x : Double, var y : Double, var rotation : Double,
                     val graphicsDataProvider: (ParticleEntity) -> GraphicalData, var dx : Double = 0.0, var dy : Double = 0.0, var drotation : Double = 0.0, var scale: Double = 1.0) : EffectsEntity() {

    private val MAX_LIFE: Int = 1000
    var lifetime: Int = MAX_LIFE
    var isDead = false;

    fun getLife() : Float{
        return (lifetime.toFloat()/MAX_LIFE.toFloat())
    }

    override fun getComponents(): List<RenderableComponent>{
        val components = mutableListOf<RenderableComponent>()

        val entityPos = Vector2(x, y)
        val variableScale = Math.min(scale, Vector2(dx, dy).magnitude*10);
        if(variableScale < 0.01){
            isDead = true
        }
        return listOf(RenderableComponent(
            model,
            Transformation(entityPos, variableScale, rotation),
            graphicsDataProvider(this)
        ))

        return components
    }
    override fun update() {
        lifetime--
        x += dx// * (lifetime/MAX_LIFE)
        y += dy// * (lifetime/MAX_LIFE)
        rotation += drotation//  * (lifetime/MAX_LIFE)
    }
    override fun isMarkedForRemoval(): Boolean{
        return lifetime < 0 || isDead
    }
}