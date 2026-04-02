package physics

import graphics.Graphics
import graphics.HasNestedRenderables
import math.ComponentReferenceFrame
import math.Coordinates
import math.Orientation
import math.Pose
import math.Vector2
import math.ZHeight
import models.Model

class DummyHull : EntityHull(Model.SQUARE.asVectors(), Coordinates(Vector2()), Orientation(0.0), ZHeight(0.0)){
    override fun getMass(): Double {
        TODO("Not yet implemented")
    }

    override fun getCenterOfMass(): Coordinates<ComponentReferenceFrame> {
        TODO("Not yet implemented")
    }

    override fun getImmediateRenderables(): List<Graphics.IntermediaryRenderable<ComponentReferenceFrame>> {
        return listOf(
            Graphics.IntermediaryRenderable(
                Model.SQUARE,
                Pose(),
                1.0,
                Graphics.ColorData(1.0f, 1.0f, 1.0f, 1.0f),
                Graphics.MetaData(1.0f)
            )
        )
    }

    override fun getChildren(): List<HasNestedRenderables<ComponentReferenceFrame, *>> {
        return emptyList()
    }
}