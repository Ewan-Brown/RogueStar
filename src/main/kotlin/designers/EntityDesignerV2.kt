package designers

import math.Vector2
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane

fun main() {
    val frame = JFrame()
    val palette = object : JPanel(){
        override fun paint(g: Graphics) {
            super.paint(g)
            g.color = Color.GREEN
            g.fillRect(0, 0, width, height)
        }
    }
    val canvas = object : JPanel(){
        override fun paint(g: Graphics) {
            super.paint(g)
            g.color = Color.RED
            g.fillRect(0, 0, width, height)
        }
    }

    palette.minimumSize = Dimension(250, 100)
//    canvas.minimumSize = Dimension(250, 100)

    val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, palette, canvas)
    frame.add(splitPane)

    frame.pack()
    frame.isVisible = true

    frame.setSize(500, 500)

    canvas.repaint()
    palette.repaint()
}

enum class DraftPartFunction{
    THRUSTER
}
class DraftPart(var position: Vector2, var orientation: Double, var size: Double){}

class DraftEntity{
    val draftParts = mutableListOf<DraftPart>()
}