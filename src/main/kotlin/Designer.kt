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
import java.awt.geom.Line2D
import java.io.*
import javax.swing.JPanel
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

    override fun mousePressed(e: MouseEvent) {
        if(e.button == MouseEvent.BUTTON1){
            val pos = getRoundedMousePos(e)

            //Check that the line isn't intersecting with any existing polygons
            var isSafe = true;
            if(currentShape.size > 0){
                val newLine = Line2D.Double(currentShape.last().x, currentShape.last().y, pos.x, pos.y)
                for (shape in shapes) {
                    for (i in 1..<shape.points.size){
                        val testLine = Line2D.Double(shape.points[i-1].x, shape.points[i-1].y, shape.points[i].x, shape.points[i].y)
                        if(newLine.intersectsLine(testLine)){
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
//        val fileContents = StringBuilder()
//        fileContents.appendLine(shapes.size)
//        for (shape in shapes) {
//            fileContents.appendLine(shape.type)
//            fileContents.appendLine(shape.points.size)
//            for (vector2 in shape.points) {
//                fileContents.appendLine("${vector2.x/spacing.toDouble()},${vector2.x/spacing.toDouble()}")
//            }
//        }
//        Files.writeString(Paths.get("target.txt"), fileContents.toString())
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

class Shape(@JsonProperty("points") var points : List<Vector2>, @JsonProperty("type") var type: Type = Type.BODY) {
}

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

class VectorDeserializer : StdDeserializer<Vector2>(Vector2::class.java){
    override fun deserialize(parser: JsonParser, p1: DeserializationContext?): Vector2 {
        val node: JsonNode = parser.codec.readTree(parser)
        val x: Double = node.get("x").asDouble()
        val y: Double = node.get("y").asDouble()
        return Vector2(x, y)
    }
}

fun main() {

    val mapper = ObjectMapper()
    val module = SimpleModule()
    module.addSerializer(Vector2::class.java, VectorSerializer())
    module.addDeserializer(Vector2::class.java, VectorDeserializer())
    mapper.registerModules(module)
    mapper.writeValue(File("target.json"), Shape(listOf(Vector2(), Vector2(1.0, 2.0), Vector2(3.0, -4.0)), Type.BODY))

    val obj = mapper.readValue<Shape>(File("target.json"), Shape::class.java)
    println(obj.type)
    println(obj.points)
//    val ui = DesignerUI(30)
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