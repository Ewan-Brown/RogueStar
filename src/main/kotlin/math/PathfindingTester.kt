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
import kotlin.math.floor

fun main(){

    var startNode: TestNode? = null
    var endNode: TestNode? = null

    var naivePathProcessor: NaivePathProcessor<TestNode>? = null
    val cells = mutableListOf<TestNode>()

    var isButton1Pressed = false;

    fun resetProcessor() {
        val nodeMap = mutableMapOf<TestNode, List<TestNode>>()

        for(cell in cells){
            val neighborCells = cell.vector.let { cellV ->
               cells.filter {(it.vector - cellV).getMagnitude() == 1.0}
            }
            println(neighborCells.size)
            nodeMap[cell] = neighborCells
        }

        if(startNode == null){
            println("startNode is null, cannot create a pathProcessor")
        }else if(endNode == null){
            println("endNode is null, cannot create a pathProcessor")
        }else if(startNode == endNode){
            println("startNode and endNode are same, cannot create a pathProcessor")
        }else{
            naivePathProcessor = NaivePathProcessor(startNode!!, endNode!!, nodeMap)
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
                square(it.vector.getX().toInt() * cellSize, it.vector.getY().toInt() * cellSize, cellSize, g)
            }
            if(naivePathProcessor != null){
                renderNaive(naivePathProcessor!!, g, cellSize)
            }
        }

        override fun mousePressed(e: MouseEvent) {
            isButton1Pressed = true

            var x = floor(e.x / cellSize.toDouble())
            var y = floor(e.y / cellSize.toDouble())


            println(e.button)
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
                resetProcessor()
            }
            if(e.keyCode == KeyEvent.VK_SPACE){
                if(naivePathProcessor != null){
                    naivePathProcessor.update()
                }
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

private fun square(x: Int, y: Int, s: Int, g: Graphics){
    (g as Graphics2D).fillRect(x, y, s, s)
}

private fun renderNaive(naivePathProcessor: NaivePathProcessor<TestNode>, g: Graphics, cellSize: Int){

    val startVec = naivePathProcessor.startNode.vector
    val endVec = naivePathProcessor.endNode.vector

    g.color = Color.DARK_GRAY
    for (c in naivePathProcessor.excludedNodes){
        square(c.vector.getX().toInt() * cellSize, c.vector.getY().toInt() * cellSize, cellSize, g)
    }

    g.color = Color.BLUE
    for (c in naivePathProcessor.currentPath){
        square(c.vector.getX().toInt() * cellSize, c.vector.getY().toInt() * cellSize, cellSize, g)
    }

    g.color = Color.GREEN
    square(startVec.getX().toInt() * cellSize, startVec.getY().toInt() * cellSize, cellSize, g)
    g.color = Color.RED
    square(endVec.getX().toInt() * cellSize, endVec.getY().toInt() * cellSize, cellSize, g)
}


private data class TestNode(val vector: Vector2) {
    override fun toString(): String {
        return this.hashCode().toString()
    }
}

