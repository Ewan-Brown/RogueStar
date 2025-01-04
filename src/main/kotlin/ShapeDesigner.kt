import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
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
import java.awt.geom.Line2D
import java.io.*
import java.net.Socket
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.round


/**
 * Allows the creation of base shapes which will make up Ships in the ShipDesigner. Each Shape will become a Model in terms of the actual game.
 * Shapes are SHARED instances as they are immutable and constant.
 * Draw them here, then export to the shapes file, which will then allow usage in the ShipDesigner
 */
class DesignerUI(private val spacing: Int) : JPanel(), MouseListener, KeyListener {

    private var currentPoints = mutableListOf<Vector2>()
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
            g.color = Color.RED
            for(s in shape.sockets){
                g.drawRect(s.x.toInt() - 2, s.y.toInt() - 2, 5, 5)
            }
        }
        //Draw outline of current shape in progress
        if(currentPoints.size > 1){
            g.color = Color.blue
            for(i in 1..< currentPoints.size){
                g.drawLine(currentPoints[i-1].x.toInt(), currentPoints[i-1].y.toInt(), currentPoints[i].x.toInt(), currentPoints[i].y.toInt())
            }
            g.color = Color.RED

        }

        //Draw next line in progress
        g.color = Color.cyan
        if(currentPoints.size > 0){
            val currentMousePosRounded = getRoundedMousePos(getMousePos())
            g.drawLine(currentPoints.last().x.toInt(), currentPoints.last().y.toInt(), currentMousePosRounded.x.toInt(), currentMousePosRounded.y.toInt())
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
            if(currentPoints.size > 0){
                val newLineLocalVector = (pos - currentPoints.last()).normalized
                val lineX = newLineLocalVector.x
                val lineY = newLineLocalVector.y
                val newLineShortened = Line2D.Double(currentPoints.last().x + lineX, currentPoints.last().y + lineY, pos.x - lineX, pos.y - lineY)
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
                if(currentPoints.isNotEmpty() && pos.equals(currentPoints[0])){
                    if(currentPoints.size > 1){
                        val sockets = mutableListOf<Vector2>()
                        for(i in 0..< currentPoints.size -1){
                            sockets.add((currentPoints[i] + currentPoints[i+1]) / 2.0)
                        }
                        sockets.add((currentPoints.last() + currentPoints.first()) / 2.0)
                        shapes.add(Shape(currentPoints, shapes.size, sockets, Vector2())) //Offset isn't bothered to be calculated until export
                        currentPoints = mutableListOf()
                    }
                }else{
                    currentPoints.add(pos)
                }
            }
        }
    }

    private fun exportToFile(){
        println("exporting ${shapes.size} shapes")
        //Localize all shapes (make it so their center is congruent with grid )
        // This means finding the 'grid-center', essentially find the bounding box of this shape (aligned with grid coords, so the bounding box should have only integer values for corners)
        // Then taking the center of that bounding box and centering this shape around it. This might work
        val localizedShapes: List<Shape> = shapes.map {
            val min = Vector2(it.points.minOf { it.x }, it.points.minOf { it.y })
            val max = Vector2(it.points.maxOf { it.x }, it.points.maxOf { it.y })
            val midpoint = (min + max) / 2.0;
            val offset = Vector2(midpoint.x % spacing, midpoint.y % spacing)
            println("midpoint = ${midpoint}")
            println("offset = ${offset}")
            return@map Shape(it.points - midpoint, it.ID, it.sockets - midpoint, offset)
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
        if(e.keyCode == KeyEvent.VK_ENTER){
            exportToFile()
        }
    }
    override fun keyReleased(e: KeyEvent) {
    }
}

class Shape(@JsonProperty("points") var points: List<Vector2>, @JsonProperty("id") val ID : Int, @JsonProperty("sockets") var sockets : List<Vector2>, @JsonProperty("placementOffset") var placementOffset : Vector2)

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