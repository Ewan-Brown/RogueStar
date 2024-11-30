import org.dyn4j.geometry.Vector2
import java.awt.Color
import java.awt.Graphics
import java.awt.MouseInfo
import java.awt.Polygon
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.round

class DesignerUI(val spacing: Int) : JPanel(), MouseListener, KeyListener {

    var currentShape = mutableListOf<Vector2>()
    val lastMousePoint: Vector2 = Vector2()
    val shapes = mutableListOf<Shape>()

    override fun paint(g: Graphics) {
        super.paint(g)

        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)

        //Draw background grid
        paintGrid(g)

        //Draw existing shapes
        for (shape in shapes) {
            paintShape(g, shape)
        }

        //Draw outline of current shape in progress
        g.color = Color.blue
        if(currentShape.size > 1){
            for(i in 1..< currentShape.size){
                g.drawLine(currentShape[i-1].x.toInt(), currentShape[i-1].y.toInt(), currentShape[i].x.toInt(), currentShape[i].y.toInt())
            }
        }

        //Draw current line in progress
        g.color = Color.cyan
        if(currentShape.size > 0){
            g.drawLine(currentShape.last().x.toInt(), currentShape.last().y.toInt(), getMousePos().x.toInt(), getMousePos().y.toInt())
        }
    }

    private fun paintGrid(g: Graphics){
        g.color = Color.GRAY
        for(x in 0..width step spacing){
            g.drawLine(x, 0, x, height)
        }
        for(y in 0..height step spacing){
            g.drawLine(0, y, width, y)
        }
    }

    private fun paintShape(g: Graphics, shape : Shape){
        g.color = when(shape.type){
            Type.BODY -> Color.GRAY
            Type.COCKPIT -> Color.GREEN
            Type.THRUSTER -> Color.RED
        }
        val poly = Polygon(shape.points.map { it -> it.x.toInt() }.toIntArray(), shape.points.map { it -> it.y.toInt() }.toIntArray(), shape.points.size)
        g.drawPolygon(poly)
    }

    private fun getMousePos() : Vector2{
        val absoluteMousePos = MouseInfo.getPointerInfo().location.toVector()
        val componentPos = locationOnScreen.toVector()
        return absoluteMousePos - componentPos
    }

    private fun getRoundedMousePos(m: MouseEvent) : Vector2{
        val x = round(m.x.toDouble() / spacing) * spacing
        val y = round(m.y.toDouble() / spacing) * spacing
        return Vector2(x, y)
    }

    override fun mouseClicked(e: MouseEvent) {
        if(e.button == MouseEvent.BUTTON1){
            val pos = getRoundedMousePos(e)
            currentShape.add(pos)
        }
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyPressed(e: KeyEvent) {
        println("DesignerUI.keyPressed")
        val type: Type? = when(e.keyCode){
            KeyEvent.VK_SPACE -> Type.BODY
            KeyEvent.VK_SHIFT -> Type.THRUSTER
            KeyEvent.VK_CONTROL -> Type.COCKPIT
            else -> {null}
        }
        if(currentShape.size < 3){
            System.err.println("You need 3 points for a shape and you have ${currentShape.size}")
        }else if(type != null){
            shapes.add(Shape(currentShape, type))
            currentShape = mutableListOf()
        }
    }
    override fun keyReleased(e: KeyEvent) {
        println("DesignerUI.keyReleased")
    }
}

enum class Type{
    THRUSTER,
    COCKPIT,
    BODY
}

data class Shape(val points : List<Vector2>, val type: Type = Type.BODY)

fun main() {
    val ui = DesignerUI(30)
    ui.addMouseListener(ui)
    val frame = JFrame()
    frame.addKeyListener(ui)
    frame.add(ui)
    frame.setSize(600,600)
    frame.isVisible = true
    frame.isFocusable = true

    while(true){
        Thread.sleep(16)
        frame.repaint()
    }
}