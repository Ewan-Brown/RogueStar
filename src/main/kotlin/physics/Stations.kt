package physics

import math.Coordinates
import math.EntityReferenceFrame
import math.InReferenceFrame
import math.Orientation
import math.Vector2
import math.ZHeight

public class EntityStation : InReferenceFrame<EntityReferenceFrame> {

    private val coordinates: Coordinates<EntityReferenceFrame> = Coordinates(Vector2())
    private val orientation: Orientation<EntityReferenceFrame> = Orientation(0.0)
    private val ZHeight: ZHeight<EntityReferenceFrame> = ZHeight(0.0)

    override fun getCoordinates(): Coordinates<EntityReferenceFrame> {
        return coordinates
    }

    override fun getOrientation(): Orientation<EntityReferenceFrame> {
        return orientation
    }

    override fun getZHeight(): ZHeight<EntityReferenceFrame> {
        return ZHeight
    }
}