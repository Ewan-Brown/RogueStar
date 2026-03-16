package math

import java.awt.Color
import kotlin.collections.filter

data class Drawable<D>(val color: Color, val element: D)
data class ProcessorUpdateResult<N>(val isDone: Boolean, val canContinue: Boolean, val currentPath: List<N>)

interface ProcessorI<N>{
    fun getDrawableNodes(): List<Drawable<N>>
    fun attemptUpdate(): ProcessorUpdateResult<N>
}

open class Connection<N>(val node: N)
class WeightedConnection<N>(node: N, val weight: Double) : Connection<N>(node)

//Source: I made it up
class NaivePathProcessor<N>(val startNode: N, val endNode: N, val connectionMap: Map<N, Collection<Connection<N>>>) : ProcessorI<N>{

    private val currentPath : MutableList<N> = mutableListOf(startNode)
    private val nodesToBeChecked : MutableCollection<N> = mutableListOf()
    private val excludedNodes : MutableCollection<N> = mutableSetOf(startNode)

    private fun processOneStep(){
        println("Processing")
        val currentNode = currentPath.last()
        excludedNodes.add(currentNode)
        val currentNeighbors = connectionMap[currentNode]!!.map { it.node }
        val interestingNeighbors = currentNeighbors.filter { it: N ->
            !nodesToBeChecked.contains(it) && !excludedNodes.contains(it)
        }
        if(interestingNeighbors.isEmpty()){
            currentPath.remove(currentPath.last())
        }else{
            if(interestingNeighbors.contains(endNode)){
                currentPath.add(endNode)
                println("Found end of path!")
                //Done!
            }else{
                currentPath.add(interestingNeighbors[0])
                excludedNodes.add(interestingNeighbors[0])
            }
        }
    }

    override fun attemptUpdate(): ProcessorUpdateResult<N> {
        if(!foundPath() && canContinue()){
            processOneStep()
        }

        return ProcessorUpdateResult(foundPath() , canContinue(), currentPath)
    }

    fun canContinue() : Boolean{
        return nodesToBeChecked.isEmpty()
    }

    fun foundPath(): Boolean {
        return currentPath.last() == endNode
    }

    override fun getDrawableNodes(): List<Drawable<N>> {
        val drawables = mutableListOf<Drawable<N>>()
        for (c in excludedNodes){
            drawables.add(Drawable(Color.DARK_GRAY, c))
        }
        for (c in currentPath){
            drawables.add(Drawable(Color.BLUE, c))
        }
        drawables.add(Drawable(Color.BLUE, startNode))
        drawables.add(Drawable(Color.MAGENTA, endNode))

        return drawables
    }
}

//https://en.wikipedia.org/wiki/A*_search_algorithm
class AStarProcessor<N>(val startNode: N, val endNode: N, val connectionMap: Map<N, Collection<WeightedConnection<N>>>, val heuristicFunction: (N) -> Double) : ProcessorI<N>{

    private val openSet: MutableList<N> = mutableListOf(startNode)
    private val cameFrom: MutableMap<N, N> = mutableMapOf()
    private val gScore: MutableMap<N, Double> = mutableMapOf(startNode to 0.0)
    private val fScore: MutableMap<N, Double> = mutableMapOf(startNode to heuristicFunction(startNode))

    private fun processOneStep(){
        println("Processing")
        if(openSet.isNotEmpty()){
            val current: N = fScore.minBy { it.value}.key
            if(current == endNode){
                println("Reached end!")
                TODO("Reconstruct Path...")
            }else{
                openSet.remove(current)
                for(connection in connectionMap[current]!!){
                    val tentativeScore = gScore[current]!! + connection.weight
                }
            }
        }
    }

    override fun attemptUpdate(): ProcessorUpdateResult<N> {
        TODO()
    }

    fun canContinue() : Boolean{
        TODO()
    }

    fun foundPath(): Boolean {
        TODO()
    }

    override fun getDrawableNodes(): List<Drawable<N>> {
        TODO()
    }
}