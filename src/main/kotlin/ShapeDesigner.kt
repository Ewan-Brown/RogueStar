import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
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
import java.awt.geom.Line2D
import java.io.*
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.round


/**
 * Allows the creation of base shapes which will make up Ships in the ShipDesigner. Each Shape will become a Model in terms of the actual game.
 * Shapes are SHARED instances as they are immutable and constant.
 * Draw them here, then export to the shapes file, which will then allow usage in the ShipDesigner
 */
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
        g.color = Color.GREEN
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

    override fun mousePressed(e: MouseEvent) {
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

        //Localize all shapes (make it so their top left corners are all at 0,0

        val localizedShapes: List<Shape> = shapes.map {
            val upperLeft = Vector2(it.points.minOf { it.x }, it.points.minOf { it.y })
            return@map Shape(it.points - upperLeft, it.ID)
        }

        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addSerializer(Vector2::class.java, VectorSerializer())
        mapper.registerModules(module)
        mapper.writeValue(File("shapes.json"), localizedShapes)
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mouseClicked(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyPressed(e: KeyEvent) {
        println("DesignerUI.keyPressed")
        if(e.keyCode == KeyEvent.VK_ENTER){
            exportToFile()
            println("exported!")
        }else{
            if(currentShape.size < 3){
                System.err.println("You need 3 points for a shape and you have ${currentShape.size}")
            }else {
                shapes.add(Shape(currentShape, shapes.size))
                currentShape = mutableListOf()
            }
        }
    }
    override fun keyReleased(e: KeyEvent) {
        println("DesignerUI.keyReleased")
    }
}

class Shape(@JsonProperty("points") var points : List<Vector2>, @JsonProperty("id") val ID : Int) {}

//Jackson shits the bed when it hits Vector2, so I wrote a custom codec here for it as it's trivial.
// My assumption is some internal fields used for caching are throwing it off

class VectorSerializer : StdSerializer<Vector2>(Vector2::class.java){
    override fun serialize(vector: Vector2, jgen: JsonGenerator, p2: SerializerProvider) {
        jgen.writeStartObject()
        jgen.writeNumberField("x", vector.x)
        jgen.writeNumberField("y", vector.y)
        jgen.writeEndObject()
    }
}

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