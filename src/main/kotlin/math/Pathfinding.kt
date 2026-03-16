package math

import java.awt.Color
import kotlin.collections.filter

data class Drawable<D>(val color: Color, val element: D)
data class ProcessorUpdateResult<N>(val isDone: Boolean, val canContinue: Boolean, val currentPath: List<N>)

interface ProcessorI<N>{
    fun getDrawableNodes(): List<Drawable<N>>
    fun attemptUpdate()
}

open class Connection<N>(val node: N)
class WeightedConnection<N>(node: N, val weight: Double) : Connection<N>(node)

//Source: I made it up
class NaivePathProcessor<N>(val startNode: N, val endNode: N, val connectionMap: Map<N, Collection<Connection<N>>>) : ProcessorI<N>{

    private val currentPath : MutableList<N> = mutableListOf(startNode)
    private val nodesToBeChecked : MutableCollection<N> = mutableListOf()
    private val excludedNodes : MutableCollection<N> = mutableSetOf(startNode)

    private fun processOneStep(){
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

    override fun attemptUpdate(){
        if(!foundPath() && canContinue()){
            processOneStep()
        }
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

    private fun getGScore(node: N) : Double {
        if(gScore.contains(node)){
            return gScore[node]!!
        }else{
            return Double.POSITIVE_INFINITY
        }
    }

    private fun getFScore(node: N) : Double {
        if(fScore.contains(node)){
            return fScore[node]!!
        }else{
            return Double.POSITIVE_INFINITY
        }
    }

    private fun getCurrentNode() = openSet.minBy { getFScore(it) }

    private fun processOneStep(){
        println("====Processing====")
        if(openSet.isNotEmpty()){
            val currentNode: N = getCurrentNode()
            if(currentNode == endNode){
                println("Reached end!")
            }else{
                println("removing current node, $currentNode")
                openSet.remove(currentNode)
                println("inspecting ${connectionMap[currentNode]!!.size} neighbors")
                for(connection in connectionMap[currentNode]!!){
                    val tentativeScore = gScore[currentNode]!! + connection.weight
                    if(tentativeScore < getGScore(connection.node)){
                        cameFrom[connection.node] = currentNode
                        gScore[connection.node] = tentativeScore
                        val hScore = heuristicFunction(connection.node)
                        fScore[connection.node] = tentativeScore + hScore
                        if(!openSet.contains(connection.node)){
                            openSet.add(connection.node)
                        }
                    }
                }
            }
        }
        for (o in openSet) {
            println("${o} : ${getGScore(o)}, ${heuristicFunction(o)}")
        }
    }

    override fun attemptUpdate(){
        if(!foundPath() && canContinue()){
            processOneStep()
        }
    }

    fun canContinue() : Boolean{
        return openSet.isNotEmpty()
    }

    fun foundPath(): Boolean {
        return getCurrentNode() == endNode
    }

    override fun getDrawableNodes(): List<Drawable<N>> {
        val drawables = mutableListOf<Drawable<N>>()
        openSet.forEach { drawables.add(Drawable(Color.CYAN, it)) }
        drawables.add(Drawable(Color.BLUE, startNode))
        drawables.add(Drawable(Color.MAGENTA, endNode))

        if(foundPath()){
            drawables.add(Drawable(Color.GREEN, getCurrentNode()))
        }else{
            if(cameFrom[getCurrentNode()] != null){
                drawables.add(Drawable(Color.ORANGE, cameFrom[getCurrentNode()]!!))
            }
            drawables.add(Drawable(Color(127, 100, 0), getCurrentNode()))
        }

        return drawables
    }
}