package physics

import graphics.Graphics
import graphics.WHITE
import math.Coordinates
import math.InReferenceFrame
import math.Orientation
import math.PartReferenceFrame
import math.PawnReferenceFrame
import math.EntityReferenceFrame
import math.HasReferenceFrame
import math.ReferenceFrame
import math.Vector2
import math.WorldReferenceFrame
import math.ZHeight
import models.Model

/**
 * Represents the info associated with a npc - agnostic to its placement in the world!
 */
abstract class AbstractPawn {
    abstract fun getPawnInfo()
    abstract fun getRenderables() : List<PawnRenderablePart>
}

abstract class PawnRenderablePart : InReferenceFrame<PawnReferenceFrame>{
    abstract fun getModel() : Model
    abstract fun getScale() : Double
    abstract fun getColor() : Graphics.ColorData
    abstract fun getMetadata() : Graphics.MetaData
}

/**
 * Collection of wrapper classes that associate a pawn with its coordinate system
 */

abstract class PawnWrapper<P: AbstractPawn, S: ReferenceFrame>(val pawn: P) : InReferenceFrame<S>,
    HasReferenceFrame<PawnReferenceFrame> {
    private var coordinates: Coordinates<S> = Coordinates(Vector2())
    private var orientation: Orientation<S> = Orientation(0.0)
    private var zHeight: ZHeight<S> = ZHeight(0.0)

    override fun getCoordinates() = coordinates
    override fun getOrientation() = orientation
    override fun getZHeight() = zHeight
}

class PawnInPart<S : AbstractPawn>(pawn: S) : PawnWrapper<S, PartReferenceFrame>(pawn){}
class PawnInShip<S : AbstractPawn>(pawn: S) : PawnWrapper<S, EntityReferenceFrame>(pawn){}
class PawnInSpace<S : AbstractPawn>(pawn: S) : PawnWrapper<S, WorldReferenceFrame>(pawn){}

class DumbPawn() : AbstractPawn(){
    override fun getPawnInfo() {

    }

    override fun getRenderables(): List<PawnRenderablePart> {
        return listOf(object : PawnRenderablePart() {
            override fun getModel(): Model {
                return Model.SQUARE
            }

            override fun getScale(): Double {
                return 1.0
            }

            override fun getColor(): Graphics.ColorData {
                return WHITE
            }

            override fun getMetadata(): Graphics.MetaData {
                return Graphics.MetaData(1.0f)
            }

            override fun getCoordinates(): Coordinates<PawnReferenceFrame> {
                return Coordinates(Vector2())
            }

            override fun getOrientation(): Orientation<PawnReferenceFrame> {
                return Orientation(0.0)
            }

            override fun getZHeight(): ZHeight<PawnReferenceFrame> {
                return ZHeight(0.0)
            }

        })
    }

}