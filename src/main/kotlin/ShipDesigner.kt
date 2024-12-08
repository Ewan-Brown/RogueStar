import org.dyn4j.geometry.Vector2
import java.awt.Color
import java.awt.Graphics
import java.awt.MouseInfo
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Line2D
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.round

//class ShipDesignerUI(private val spacing: Int) : JPanel(), MouseListener, KeyListener {
//
//    private var currentShape = mutableListOf<Vector2>()
//    val shapes = mutableListOf<Shape>()
//    val components = mutableListOf<Component>()
//
//    override fun paint(g: Graphics) {
//        super.paint(g)
//
//        g.color = Color.BLACK
//        g.fillRect(0, 0, width, height)
//
//        //Draw background grid
//        paintGrid(g)
//
//        //Draw existing shapes
////        for (shape in shapes) {
////            paintPolygon(g, shape.points)
////        }
//
//        //Draw outline of current shape in progress
//        g.color = Color.blue
//        if(currentShape.size > 1){
//            for(i in 1..< currentShape.size){
//                g.drawLine(currentShape[i-1].x.toInt(), currentShape[i-1].y.toInt(), currentShape[i].x.toInt(), currentShape[i].y.toInt())
//            }
//        }
//
//        //Draw current line in progress
//        g.color = Color.cyan
//        if(currentShape.size > 0){
//            g.drawLine(currentShape.last().x.toInt(), currentShape.last().y.toInt(), getMousePos().x.toInt(), getMousePos().y.toInt())
//        }
//    }
//
//    private fun paintGrid(g: Graphics){
//        g.color = Color.GRAY
//        for(x in 0..width step spacing){
//            g.drawLine(x, 0, x, height)
//        }
//        for(y in 0..height step spacing){
//            g.drawLine(0, y, width, y)
//        }
//    }
//
//    private fun paintPolygon(g: Graphics, points: List<Vector2>, position: Vector2){
//        val transformedPoints = points + position
//        val poly = vectorListToPolygon(transformedPoints)
//        g.drawPolygon(poly)
//    }
//
//    private fun getMousePos() : Vector2{
//        val absoluteMousePos = MouseInfo.getPointerInfo().location.toVector()
//        val componentPos = locationOnScreen.toVector()
//        return absoluteMousePos - componentPos
//    }
//
//    private fun getRoundedMousePos(m: MouseEvent) : Vector2{
//        val x = round(m.x.toDouble() / spacing) * spacing
//        val y = round(m.y.toDouble() / spacing) * spacing
//        return Vector2(x, y)
//    }
//
//    override fun mouseClicked(e: MouseEvent) {
//        if(e.button == MouseEvent.BUTTON1){
//            val pos = getRoundedMousePos(e)
//
//            //Check that the line isn't intersecting with any existing polygons
//            var isSafe = true;
//            if(currentShape.size > 0){
//                val newLine = Line2D.Double(currentShape.last().x, currentShape.last().y, pos.x, pos.y)
//                for (shape in shapes) {
//                    for (i in 1..<shape.points.size){
//                        val testLine = Line2D.Double(shape.points[i-1].x, shape.points[i-1].y, shape.points[i].x, shape.points[i].y)
//                        if(newLine.intersectsLine(testLine)){
//                            System.err.println("Intersects with existing line!")
//                            isSafe = false;
//                        }
//                    }
//                }
//            }
//            if(isSafe){
//                currentShape.add(pos)
//            }
//        }
//    }
//
//    private fun exportToFile(){
//    }
//
//    override fun mouseEntered(e: MouseEvent) {}
//    override fun mouseExited(e: MouseEvent) {}
//    override fun mousePressed(e: MouseEvent) {}
//    override fun mouseReleased(e: MouseEvent) {}
//    override fun keyTyped(e: KeyEvent) {}
//    override fun keyPressed(e: KeyEvent) {
//        println("DesignerUI.keyPressed")
//        if(e.keyCode == KeyEvent.VK_ENTER){
//            exportToFile()
//            println("exported!")
//        }
//
//        val type: Type? = when(e.keyCode){
//            KeyEvent.VK_SPACE -> Type.BODY
//            KeyEvent.VK_SHIFT -> Type.THRUSTER
//            KeyEvent.VK_CONTROL -> Type.COCKPIT
//            else -> {null}
//        }
//        if(currentShape.size < 3){
//            System.err.println("You need 3 points for a shape and you have ${currentShape.size}")
//        }else if(type != null){
////            shapes.add(Shape(currentShape, type))
//            currentShape = mutableListOf()
//        }
//    }
//    override fun keyReleased(e: KeyEvent) {
//        println("DesignerUI.keyReleased")
//    }
//}
//
//enum class Type{
//    THRUSTER,
//    COCKPIT,
//    BODY
//}
//
//data class Component(val shape: Shape, var scale: Double, var position: Vector2, var rotation: Double, var type: Type = Type.BODY)

fun main() {
//    val ui = ShipDesignerUI(30)
//    ui.addMouseListener(ui)
//    val frame = JFrame()
//    frame.addKeyListener(ui)
//    frame.add(ui)
//    frame.setSize(600,600)
//    frame.isVisible = true
//    frame.isFocusable = true
//
//    while(true){
//        Thread.sleep(16)
//        frame.repaint()
//    }
}