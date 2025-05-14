import org.dyn4j.geometry.Rotation


/*
 * A nice place for things to be translated from 'game' to 'graphics'
 */
class GraphicsService {
    /**
     * Take a given entity and a fixtureSlot and determine what (if any) renderable should be extracted
     * This should be easily extensible for a single component to produce multiple renderables (e.g a gun turret with base + barrel)
     */
    fun <T : BasicFixture> componentToRenderable(entity : PhysicsEntity, fixtureSlot : FixtureSlot<T>) : Graphics.RenderableEntity? {

        entity.run {
            val fixture = entity.fixtureSlotFixtureMap[fixtureSlot]
            if(fixture != null){
                val entityAngle = getTransform().rotation.toRadians()
                val entityPos = this.worldCenter
                val absolutePos = fixtureSlot.localTransform.translation.toVec2().subtract(this.getMass().center)
                    .rotate(entityAngle).add(entityPos)
                val newAngle =
                    Rotation(fixtureSlot.localTransform.rotation.toRadians() + this.transform.rotationAngle)
                val scale = fixtureSlot.localTransform.scale


                return when(fixtureSlot){
                    is ThrusterFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            Transformation(absolutePos.toVec3(), scale, newAngle),
                            Graphics.ColorData(1.0f, 1.0f, 0.0f, 0.0f),
                            Graphics.MetaData(1.0f)
                        )
                    }
                    is BasicFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            Transformation(absolutePos.toVec3(), scale, newAngle),
                            Graphics.ColorData(0.0f, 0.0f, 1.0f, 0.0f),
                            Graphics.MetaData(1.0f)
                        )
                    } is GunFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            Transformation(absolutePos.toVec3(), scale, newAngle),
                            Graphics.ColorData(1.0f, 0.0f, 0.0f, 0.0f),
                            Graphics.MetaData(1.0f)
                        )
                    } is CockpitFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            Transformation(absolutePos.toVec3(), scale, newAngle),
                            Graphics.ColorData(01.0f, 010f, 01.0f, 0.0f),
                            Graphics.MetaData(1.0f)
                        )
                    }
                }
            }else{
                return null;

            }
        }
    }
}
