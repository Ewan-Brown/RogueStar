import org.dyn4j.geometry.Vector2
import java.awt.Color
import java.awt.Graphics
import java.awt.MouseInfo
import java.awt.Polygon
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Line2D
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.round

class DesignerUI(private val spacing: Int) : JPanel(), MouseListener, KeyListener {

    private var currentShape = mutableListOf<Vector2>()
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
            val currentMousePosRounded = getRoundedMousePos(getMousePos())
            g.drawLine(currentShape.last().x.toInt(), currentShape.last().y.toInt(), currentMousePosRounded.x.toInt(), currentMousePosRounded.y.toInt())
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
            Type.BODY -> Color.LIGHT_GRAY
            Type.COCKPIT -> Color.GREEN
            Type.THRUSTER -> Color.RED
        }
        val poly = Polygon(shape.points.map { it.x.toInt() }.toIntArray(), shape.points.map { it.y.toInt() }.toIntArray(), shape.points.size)
        g.fillPolygon(poly)
    }

    private fun getMousePos() : Vector2{
        val absoluteMousePos = MouseInfo.getPointerInfo().location.toVector()
        val componentPos = locationOnScreen.toVector()
        return absoluteMousePos - componentPos
    }

    private fun getRoundedMousePos(vec: Vector2) : Vector2{
        return getRoundedMousePos(vec.x, vec.y)
    }

    private fun getRoundedMousePos(x: Double, y: Double) : Vector2{
        val x2 = round(x / spacing) * spacing
        val y2 = round(y / spacing) * spacing
        return Vector2(x2, y2)
    }

    private fun getRoundedMousePos(m: MouseEvent) : Vector2{
        return getRoundedMousePos(m.x.toDouble(), m.y.toDouble())
    }

    override fun mouseClicked(e: MouseEvent) {
        if(e.button == MouseEvent.BUTTON1){
            val pos = getRoundedMousePos(e)

            //Check that the line isn't intersecting with any existing polygons
            var isSafe = true;
            if(currentShape.size > 0){
                val newLineLocalVector = (pos - currentShape.last()).normalized
                val lineX = newLineLocalVector.x
                val lineY = newLineLocalVector.y
                val newLineShortened = Line2D.Double(currentShape.last().x + lineX, currentShape.last().y + lineY, pos.x - lineX, pos.y - lineY)
                for (shape in shapes) {
                    for (i in 1..<shape.points.size){
                        val testLine = Line2D.Double(shape.points[i-1].x, shape.points[i-1].y, shape.points[i].x, shape.points[i].y)
                        val testLineVector = (shape.points[i] - shape.points[i-1]).normalized
                        val slope1 = newLineLocalVector.getSlope()
                        val slope2 = testLineVector.getSlope()
                        val intersect1 = (newLineShortened.y1 - newLineShortened.x1 * slope1)
                        val intersect2 = (testLine.y1 - testLine.x1 * slope1)
                        val areColinear = slope1 == slope2 && intersect1 == intersect2
                        if(newLineShortened.intersectsLine(testLine) && !areColinear){
                            System.err.println("Intersects with existing line!")
                            isSafe = false;
                        }
                    }
                }
            }
            if(isSafe){
                currentShape.add(pos)
            }
        }
    }



    private fun exportToFile(){
        val fileContents = StringBuilder()
        fileContents.appendLine(shapes.size)
        for (shape in shapes) {
            fileContents.appendLine(shape.type)
            fileContents.appendLine(shape.points.size)
            for (vector2 in shape.points) {
                fileContents.appendLine("${vector2.x/spacing.toDouble()},${vector2.x/spacing.toDouble()}")
            }
        }
        Files.writeString(Paths.get("target.txt"), fileContents.toString())
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyPressed(e: KeyEvent) {
        println("DesignerUI.keyPressed")
        if(e.keyCode == KeyEvent.VK_ENTER){
            exportToFile()
            println("exported!")
        }

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

class Shape(val points : List<Vector2>, val type: Type = Type.BODY)

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