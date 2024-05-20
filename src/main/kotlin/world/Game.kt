package world

import org.dyn4j.world.World
import world.entity.Entity

class Game {
    private val entityList : List<Entity> = emptyList()
    private val world : World<Entity> = World();
    fun update() {
        for (entity in entityList) {

        }
    }

}