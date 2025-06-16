import org.dyn4j.geometry.Rotation


/*
 * A nice place for things to be translated from 'game' to 'graphics'
 */

//TODO Create a base interface and some other implementations.
class GraphicsService {
    /**
     * Take a given entity and a fixtureSlot and determine what (if any) renderable should be extracted
     * This should be easily extensible for a single component to produce multiple renderables (e.g a gun turret with base + barrel)
     */
    fun <T : BasicFixture> componentToRenderable(entity : PhysicsEntity, fixtureSlot : FixtureSlot<T>) : Graphics.RenderableEntity? {

        entity.run {
            val fixture = entity.fixtureSlotFixtureMap[fixtureSlot]
            if(fixture != null){

                val transform = getFixtureSlotTransform(this, fixtureSlot)

                //TODO this is repetitive!
                return when(fixtureSlot){
                    is ThrusterFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            transform,
                            Graphics.ColorData(1.0f, 0.0f, 0.0f, 0.0f),
                            Graphics.MetaData(1.0f)
                        )
                    }
                    is BasicFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            transform,
                            Graphics.ColorData(0.0f, 0.0f, 1.0f, 0.0f),
                            Graphics.MetaData(1.0f)
                        )
                    } is GunFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            transform,
                            Graphics.ColorData(1.0f, 1.0f, 0.0f, 0.0f),
                            Graphics.MetaData(1.0f)
                        )
                    } is CockpitFixtureSlot -> {
                        Graphics.RenderableEntity(
                            fixtureSlot.model,
                            transform,
                            Graphics.ColorData(0.0f, 1.0f, 1.0f, 0.0f),
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
