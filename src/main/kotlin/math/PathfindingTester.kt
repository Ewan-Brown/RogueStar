package math

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.sqrt

fun main(){

    var startNode: TestNode? = null
    var endNode: TestNode? = null

    var processor: ProcessorI<TestNode>? = null
    val cells = mutableListOf<TestNode>()

    var isButton1Pressed = false;

    //Test Data
    for (y in 0..2){
        for(x in 0..6){
            println("$x, $y")
            val node = TestNode(Vector2(x.toDouble(), y.toDouble()))
            cells.add(node)
            if(x == 0 && y == 0){
                startNode = node
            }else if(x == 6 && y == 2){
                endNode = node
            }
        }
    }


    fun createNaiveProcessor() {
        val nodeMap = mutableMapOf<TestNode, List<Connection<TestNode>>>()

        for(cell in cells){
            val neighborCells = cell.vector.let { cellV ->
               cells.filter {(it.vector - cellV).getMagnitude() == 1.0}
            }
            nodeMap[cell] = neighborCells.map { Connection(it) }
        }

        if(startNode == null){
            println("startNode is null, cannot create a pathProcessor")
        }else if(endNode == null){
            println("endNode is null, cannot create a pathProcessor")
        }else if(startNode == endNode){
            println("startNode and endNode are same, cannot create a pathProcessor")
        }else{
            processor = NaivePathProcessor(startNode!!, endNode!!, nodeMap)
        }
    }

    fun createAStarProcessor() {
        val nodeMap = mutableMapOf<TestNode, List<WeightedConnection<TestNode>>>()

        for(cell in cells){
            val neighborCells = cell.vector.let { cellV ->
                cells.filter {
                    val diff = (it.vector - cellV)
                    it != cell && abs(diff.getX()) <= 1.0 && abs(diff.getY()) <= 1.0
                }
            }
            nodeMap[cell] = neighborCells.map { WeightedConnection(it, (it.vector - cell.vector).getMagnitude()) }
        }

        if(startNode == null){
            println("startNode is null, cannot create a pathProcessor")
        }else if(endNode == null){
            println("endNode is null, cannot create a pathProcessor")
        }else if(startNode == endNode){
            println("startNode and endNode are same, cannot create a pathProcessor")
        }else{
            processor = AStarProcessor(startNode!!, endNode!!, nodeMap, {(it.vector - endNode!!.vector).getMagnitude()})
        }
    }

    var cellSize = 15
    val frame = JFrame()
    val panel = object : JPanel(), MouseListener, MouseWheelListener, KeyListener{
        override fun paint(g: Graphics) {
            super.paint(g)
            g.color = Color.BLACK


            var x = 0
            do{
                g.drawLine(x, 0, x, height)
                x += cellSize
            }while(x <= width)

            var y = 0
            do{
                g.drawLine(0, y, width, y)
                y += cellSize
            }while(y <= height)

            cells.forEach {
                square(it.vector * cellSize.toDouble(), cellSize, g)
            }

            if(processor != null){
                renderDrawableProcessor(processor!!, g, cellSize)
            }
        }

        override fun mousePressed(e: MouseEvent) {
            isButton1Pressed = true

            var x = floor(e.x / cellSize.toDouble())
            var y = floor(e.y / cellSize.toDouble())

            when(e.button){
                MouseEvent.BUTTON1 -> {
                    val node = TestNode(Vector2(x, y))
                    if(cells.none{(it.vector - Vector2(x, y)).getMagnitude() < 0.01}){
                        cells.add(node)
                        println("added node: $node")
                    }
                }
                MouseEvent.BUTTON2 -> {
                    startNode = cells.firstOrNull{(it.vector - Vector2(x, y)).getMagnitude() < 0.01}
                    println("set start node: $startNode")
                }
                MouseEvent.BUTTON3 -> {
                    endNode = cells.firstOrNull{(it.vector - Vector2(x, y)).getMagnitude() < 0.01}
                    println("set end node: $endNode")
                }
                else -> {}
            }

        }

        override fun mouseClicked(e: MouseEvent?) {}
        override fun mouseReleased(e: MouseEvent?) {
            isButton1Pressed = false
        }
        override fun mouseEntered(e: MouseEvent?) {}
        override fun mouseExited(e: MouseEvent?) {}
        override fun mouseWheelMoved(e: MouseWheelEvent?) {
            cellSize += e!!.wheelRotation
        }

        override fun keyTyped(e: KeyEvent?) {}

        override fun keyPressed(e: KeyEvent) {
            if(e.keyCode == KeyEvent.VK_ENTER){
                createAStarProcessor()
            }
            if(e.keyCode == KeyEvent.VK_SPACE){
                processor?.attemptUpdate()
            }
        }

        override fun keyReleased(e: KeyEvent?) {

        }

    }

    frame.add(panel)
    panel.addMouseListener(panel)
    panel.addMouseWheelListener (panel )
    panel.addKeyListener(panel)

    panel.isFocusable = true
    frame.isVisible = true
    frame.setSize(600, 600)

    Thread {
        while(true){
            panel.repaint()
            frame.repaint()
            Thread.sleep(16)
        }
    }.start()

}

private fun square(pos: Vector2, s: Int, g: Graphics){
    (g as Graphics2D).fillRect(pos.getX().toInt(), pos.getY().toInt(), s, s)
}

private fun renderDrawableProcessor(processorI: ProcessorI<TestNode>, g: Graphics, cellSize: Int){

    for (rect in processorI.getDrawableNodes()) {
        g.color = rect.color
        square(rect.element.vector * cellSize.toDouble(), cellSize, g)
    }
}


private data class TestNode(val vector: Vector2) {
    override fun toString(): String {
        return vector.toString()
    }
}

