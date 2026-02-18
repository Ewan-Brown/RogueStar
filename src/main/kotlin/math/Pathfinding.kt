package math

import math.NaivePathProcessor.ProcessorUpdateResult
import kotlin.collections.filter

abstract class AbsPathProcessor<N, C>(val startNode: N, val endNode: N, val pathMap: Map<N, Collection<C>>){

    abstract fun update(): ProcessorUpdateResult<N>

    init{
        if(!pathMap.none{it == startNode}) throw IllegalArgumentException("startNode not found in pathMap")
        if(!pathMap.none(){it == endNode}) throw IllegalArgumentException("startNode not found in pathMap")
        if(startNode == endNode) throw IllegalArgumentException("startNode cannot be same as endNode")
    }
}

abstract class WeightedPathProcessor<N>(startNode: N, endNode: N, pathMap: Map<N, List<Pair<N, Double>>>) : AbsPathProcessor<N, Pair<N, Double>>(startNode, endNode, pathMap){

}

class NaivePathProcessor<N>(startNode: N, endNode: N, pathMap: Map<N, Collection<N>>) : AbsPathProcessor<N, N>(startNode, endNode, pathMap){

    val currentPath : MutableList<N> = mutableListOf(startNode)
    val nodesToBeChecked : MutableCollection<N> = mutableListOf()
    val excludedNodes : MutableCollection<N> = mutableSetOf(startNode)

    data class ProcessorUpdateResult<N>(val isDone: Boolean, val canContinue: Boolean, val currentPath: List<N>)

    private fun processOneStep(){
        println("Processing")
        val currentNode = currentPath.last()
        excludedNodes.add(currentNode)
        val currentNeighbors = pathMap[currentNode]
        val interestingNeighbors = currentNeighbors!!.filter { !nodesToBeChecked.contains(it) && !excludedNodes.contains(it) }
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

    override fun update(): ProcessorUpdateResult<N> {
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
}

