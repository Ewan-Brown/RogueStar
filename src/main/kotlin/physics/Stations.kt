package physics

import graphics.Renderer
import math.ComponentReferenceFrame
import math.Coordinates
import math.EntityReferenceFrame
import math.InReferenceFrame
import math.Orientation
import math.Vector2
import math.ZHeight

public class EntityStation(boundingBox: List<Vector2>, private val mass: Double, centerOfMass: Vector2) : Component(boundingBox, centerOfMass) {

    private var coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private var orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private var ZHeight: ZHeight<EntityReferenceFrame> = ZHeight(0.0)

    override fun getCoordinates(): Coordinates<EntityReferenceFrame> {
        return coordinates
    }

    override fun getOrientation(): Orientation<EntityReferenceFrame> {
        return orientation
    }

    override fun getZHeight(): ZHeight<EntityReferenceFrame> {
        return ZHeight
    }

    override fun getMass(): Double {
        return mass
    }

}