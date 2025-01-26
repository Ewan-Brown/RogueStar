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
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import kotlin.math.PI
import kotlin.math.round

/**
 * UI creation + export for a ship.
 */
private class ShipDesignerUI(private val spacing: Int) : JPanel(), MouseListener, KeyListener {

    val shapes: List<Shape>

    init {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addSerializer(Vector2::class.java, VectorSerializer())
        module.addDeserializer(Vector2::class.java, VectorDeserializer())
        mapper.registerModules(module)
        shapes = mapper.readValue(File("shapes.json"), Array<Shape>::class.java).toList()
        println("Shapes found : ${shapes.size}")
    }

    val components = mutableListOf<ComponentBlueprint>()

    private var selectedShape: Shape = shapes[0]
    var selectedColor: Color = Color.WHITE
    var selectedQuarterRotations: Int = 0
    var selectedType: Type = Type.BODY

    override fun paint(g: Graphics) {
        super.paint(g)

        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)

        //Draw background grid

        paintGrid(g)

        //Draw existing shapes
        for (component in components){
            g.color = component.type.c
            val transformedShape = transformShape(shapes.first { it.ID == component.shape }, component.position, component.rotation, component.scale)
            paintPolygon(g, transformedShape.points, true)
            g.color = Color.RED
            paintSockets(g, transformedShape.sockets)
        }

        //Draw outline of current component in progress
        g.color = selectedColor ?: Color.CYAN
        paintPolygon(g, getTransformedShapeAtMouse(), false)
    }

    private fun getTransformedShapeAtMouse(): List<Vector2>{
        val vec = getMousePos() + selectedShape.placementOffset
        val x = round(vec.x / spacing) * spacing
        val y = round(vec.y / spacing) * spacing
        val position = Vector2(x, y) - selectedShape.placementOffset
        val transformedShape = transformShape(selectedShape, position, selectedQuarterRotations, 1.0)
        return transformedShape.points
    }

    private val quarterPI = PI/2.0

    private fun transformShape(shape: Shape, position: Vector2, rotations: Int, scale: Double): Shape{
        val points = shape.points.map { point -> transformPoint(point, rotations, scale, position) }
        val sockets = shape.sockets.map { point -> transformPoint(point, rotations, scale, position) }
        return Shape(points, shape.ID, sockets, shape.placementOffset)
    }

    private fun transformPoint(point: Vector2, rotations: Int, scale: Double, position: Vector2): Vector2{
        val rotation = rotations * quarterPI
        return point.copy().rotate(rotation).round() * scale + position
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

    private fun paintSockets(g: Graphics, points: List<Vector2>){
        for(point in points){
            g.drawRect((point.x - 2).toInt(), (point.y - 2).toInt(), 5, 5)
        }
    }

    private fun paintPolygon(g: Graphics, points: List<Vector2>, fill: Boolean){
        val poly = vectorListToPolygon(points)
        if(fill){
            g.fillPolygon(poly)
            g.color = Color.BLACK
        }
        g.drawPolygon(poly)
    }

    private fun getMousePos() : Vector2{
        val absoluteMousePos = MouseInfo.getPointerInfo().location.toVector()
        val componentPos = locationOnScreen.toVector()
        return absoluteMousePos - componentPos
    }

    private fun getRoundedMousePos() : Vector2{
        val vec = getMousePos()
        val x = round(vec.x / spacing) * spacing
        val y = round(vec.y / spacing) * spacing
        return Vector2(x, y)
    }

    private fun getRoundedMousePos(m: MouseEvent) : Vector2{
        val x = round(m.x.toDouble() / spacing) * spacing
        val y = round(m.y.toDouble() / spacing) * spacing
        return Vector2(x, y)
    }

    override fun mouseClicked(e: MouseEvent) {
        if(e.button == MouseEvent.BUTTON1){
            val vec = getMousePos() + selectedShape.placementOffset
            val x = round(vec.x / spacing) * spacing
            val y = round(vec.y / spacing) * spacing
            val position = Vector2(x, y) - selectedShape.placementOffset
            components.add(ComponentBlueprint(selectedShape.ID,1.0, position, selectedQuarterRotations, selectedType))
        }
    }

    /**
     * Custom codec for Components. We need export TWO files - a list of models with IDs attached, and the actual components themselves.
     */

    private fun exportToFile(){

        val connectionMap: Map<Int, MutableList<Int>> = components.associate{components.indexOf(it) to mutableListOf()}
        for (component : ComponentBlueprint in components) {
            for(otherComponent in components){
                if(component != otherComponent){
                    val sockets = transformShape(shapes[component.shape], component.position, component.rotation, component.scale).sockets
                    val otherSockets = transformShape(shapes[otherComponent.shape], otherComponent.position, otherComponent.rotation, otherComponent.scale).sockets

                    var isConnected = sockets.any {
                        socket1 -> otherSockets.any{
                            socket2 -> socket1.equals(socket2)
                        }
                    }
                    if(isConnected){
                        connectionMap[components.indexOf(component)]!!.add(components.indexOf(otherComponent))
                    }
                }
            }
        }
        println(connectionMap)
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addSerializer(ComponentBlueprint::class.java, ComponentSerializer())
        module.addSerializer(Vector2::class.java, VectorSerializer())
        module.addDeserializer(Vector2::class.java, VectorDeserializer())
        mapper.registerModules(module)
        val shipName = JOptionPane.showInputDialog("give your ship a name!")
        mapper.writeValue(File("entity_$shipName.json"), PhysicsLayer.EntityBlueprint(components, connectionMap))
        println("exported!")
    }

    override fun mouseEntered(e: MouseEvent) {}
    override fun mouseExited(e: MouseEvent) {}
    override fun mousePressed(e: MouseEvent) {}
    override fun mouseReleased(e: MouseEvent) {}
    override fun keyTyped(e: KeyEvent) {}
    override fun keyPressed(e: KeyEvent) {
        if(e.keyCode == KeyEvent.VK_ENTER){
            exportToFile()
        }

        if(e.keyCode == KeyEvent.VK_SPACE){
            selectedShape = shapes[(shapes.indexOf(selectedShape) + 1) % shapes.size]
            println("switching shape")
        }

        when(e.keyCode){
            KeyEvent.VK_Z -> {
                selectedColor = Color.BLUE
                selectedType = Type.BODY
            }
            KeyEvent.VK_X -> {
                selectedColor = Color.CYAN
                selectedType = Type.ROOT
            }
            KeyEvent.VK_C -> {
                selectedColor = Color.YELLOW
                selectedType = Type.THRUSTER
            }
            KeyEvent.VK_V -> {
                selectedColor = Color.RED
                selectedType = Type.GUN
            }
        }

        if(e.keyCode == KeyEvent.VK_R){
            selectedQuarterRotations += 1
            println("Rotating")
        }
    }
    override fun keyReleased(e: KeyEvent) {
    }
}

enum class Type(val c : Color){
    THRUSTER(Color.ORANGE),
    ROOT(Color.CYAN),
    GUN(Color.YELLOW),
    BODY(Color.BLUE);
}

data class ComponentBlueprint(val shape: Int, var scale: Double, var position: Vector2, var rotation: Int, var type: Type = Type.BODY)

fun main() {
    val ui = ShipDesignerUI(30)
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


class VectorDeserializer : StdDeserializer<Vector2>(Vector2::class.java){
    override fun deserialize(parser: JsonParser, p1: DeserializationContext?): Vector2 {
        val node: JsonNode = parser.codec.readTree(parser)
        val x: Double = node.get("x").asDouble()
        val y: Double = node.get("y").asDouble()
        return Vector2(x, y)
    }
}

class ComponentSerializer : StdSerializer<ComponentBlueprint>(ComponentBlueprint::class.java){
    override fun serialize(component: ComponentBlueprint, jgen: JsonGenerator, provider: SerializerProvider?) {
        jgen.writeStartObject()
        jgen.writeNumberField("shape", component.shape)
        jgen.writeNumberField("scale", component.scale)
        jgen.writeObjectField("position", component.position)
        jgen.writeNumberField("rotation", component.rotation)
        jgen.writeStringField("type", component.type.toString())
        jgen.writeEndObject()
    }
}
