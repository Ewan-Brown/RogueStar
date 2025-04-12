package client

/*
 * A gross place for things to be translated from 'game' to 'graphics'
 */
class GraphicsService {
    /**
     *
     */
    fun <T : PhysicsLayer.BasicFixture> componentToRenderable(entity : PhysicsLayer.PhysicsEntity, fixtureSlot : PhysicsLayer.PhysicsEntity.FixtureSlot<T>) : Graphics.RenderableEntity {
        return Graphics.RenderableEntity(fixtureSlot.model, fixtureSlot.localTransform, Graphics.ColorData(1.0f, 1.0f, 1.0f, 1.0f), Graphics.MetaData(1.0f))
    }
}
