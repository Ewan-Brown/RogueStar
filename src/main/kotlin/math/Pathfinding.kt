package math

import math.NaivePathProcessor.ProcessorUpdateResult
import java.awt.Color
import kotlin.collections.filter


interface ProcessorI<N>{

    data class Drawable<D>(val color: Color, val element: D)
    fun getDrawableNodes(): List<Drawable<N>>
    abstract fun update(): ProcessorUpdateResult<N>
}

abstract class AbsPathProcessor<N, C>(protected val startNode: N, protected val endNode: N, val pathMap: Map<N, Collection<C>>) : ProcessorI<N>{

    init{
        if(!pathMap.none{it == startNode}) throw IllegalArgumentException("startNode not found in pathMap")
        if(!pathMap.none(){it == endNode}) throw IllegalArgumentException("startNode not found in pathMap")
        if(startNode == endNode) throw IllegalArgumentException("startNode cannot be same as endNode")
    }
}

class NaivePathProcessor<N>(startNode: N, endNode: N, pathMap: Map<N, Collection<N>>) : AbsPathProcessor<N, N>(startNode, endNode, pathMap){

    private val currentPath : MutableList<N> = mutableListOf(startNode)
    private val nodesToBeChecked : MutableCollection<N> = mutableListOf()
    private val excludedNodes : MutableCollection<N> = mutableSetOf(startNode)

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

    override fun getDrawableNodes(): List<ProcessorI.Drawable<N>> {
        val drawables = mutableListOf<ProcessorI.Drawable<N>>()
        for (c in excludedNodes){
            drawables.add(ProcessorI.Drawable(Color.DARK_GRAY, c))
        }
        for (c in currentPath){
            drawables.add(ProcessorI.Drawable(Color.BLUE, c))
        }
        drawables.add(ProcessorI.Drawable(Color.BLUE, startNode))
        drawables.add(ProcessorI.Drawable(Color.MAGENTA, endNode))

        return drawables
    }
}

