package physics

import math.Coordinates
import math.PartSpace
import math.ShipSpace
import math.Vector2
import math.WorldSpace

/**
 * Represents the info associated with a npc - agnostic to its placement in the world!
 */
abstract class Pawn {
    abstract fun getPawnInfo()
}

/**
 * Collection of wrapper classes that associate a pawn with its coordinate system
 */
class PawnInPart<S : Pawn>(){
    val coordinates: Coordinates<PartSpace> = Coordinates(Vector2())
}
class PawnInShip<S : Pawn>(){
    val coordinates: Coordinates<ShipSpace> = Coordinates(Vector2())
}
class PawnInSpace<S : Pawn>(){
    val coordinates: Coordinates<WorldSpace> = Coordinates(Vector2())
}
