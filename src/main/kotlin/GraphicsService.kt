import Graphics
import org.dyn4j.geometry.Rotation
import kotlin.text.toFloat


/*
 * A nice place for things to be translated from 'game' to 'graphics'
 */
class GraphicsService {
    /**
     * Take a given entity and a fixtureSlot and determine what (if any) renderable should be extracted
     */
    fun <T : PhysicsLayer.BasicFixture> componentToRenderable(entity : PhysicsLayer.PhysicsEntity, fixtureSlot : PhysicsLayer.PhysicsEntity.FixtureSlot<T>) : Graphics.RenderableEntity? {

        entity.run {
            val entityAngle = getTransform().rotation.toRadians()
            val entityPos = this.worldCenter
            val absolutePos = fixtureSlot.localTransform.translation.toVec2().subtract(this.getMass().center)
                .rotate(entityAngle).add(entityPos)
            val newAngle =
                Rotation(fixtureSlot.localTransform.rotation.toRadians() + this.transform.rotationAngle)
            val scale = fixtureSlot.localTransform.scale

            return when(fixtureSlot){
                is PhysicsLayer.PhysicsEntity.ThrusterFixtureSlot -> {
                    Graphics.RenderableEntity(
                        fixtureSlot.model,
                        Transformation(absolutePos.toVec3(), scale, newAngle),
                        Graphics.ColorData(1.0f, 1.0f, 0.0f, 0.0f),
                        Graphics.MetaData(1.0f)
                    )
                }
                is PhysicsLayer.PhysicsEntity.BasicFixtureSlot -> {
                    Graphics.RenderableEntity(
                        fixtureSlot.model,
                        Transformation(absolutePos.toVec3(), scale, newAngle),
                        Graphics.ColorData(0.0f, 0.0f, 1.0f, 0.0f),
                        Graphics.MetaData(1.0f)
                    )
                }
            }


        }
    }
}
