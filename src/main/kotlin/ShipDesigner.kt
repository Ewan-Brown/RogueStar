import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.dyn4j.dynamics.Body
import org.dyn4j.geometry.Vector2
import java.awt.Color
import java.awt.Graphics
import java.awt.MouseInfo
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.geom.Line2D
import java.io.File
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.PI
import kotlin.math.round

private class ShipDesignerUI(private val spacing: Int) : JPanel(), MouseListener, KeyListener {

    val shapes: List<Shape>

    init {
        val mapper = ObjectMapper()
        val module = SimpleModule()
        module.addSerializer(Vector2::class.java, VectorSerializer())
        module.addDeserializer(Vector2::class.java, VectorDeserializer())
        mapper.registerModules(module)
        shapes = mapper.readValue(File("target.json"), Array<Shape>::class.java).toList()
    }

    val components = mutableListOf<SimpleComponent>()

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
            g.color = component.color
            paintPolygon(g, transformPolygon(component.shape, component.position, component.rotation, component.scale), true)
        }

        //Draw outline of current component in progress
        g.color = selectedColor ?: Color.CYAN
        paintPolygon(g, getTransformedShapeAtMouse(), false)
    }

    private fun getTransformedShapeAtMouse(): List<Vector2>{
        val position = getRoundedMousePos()
        val transformedPoints = transformPolygon(selectedShape, position, selectedQuarterRotations, 1.0)
        return transformedPoints
    }

    private val quarterPI = PI/2.0

    private fun transformPolygon(shape: Shape, position: Vector2, rotations: Int, scale: Double): List<Vector2>{
        val rotation = rotations * quarterPI
        return shape.points.map { point ->
            return@map (point.copy().rotate(rotation).round() * scale) + position
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
            components.add(SimpleComponent(selectedShape, selectedColor,1.0, getRoundedMousePos(), selectedQuarterRotations, selectedType))
        }
    }

    private fun exportToFile(){
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
                selectedType = Type.COCKPIT
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

enum class Type{
    THRUSTER,
    COCKPIT,
    GUN,
    BODY
}

data class SimpleComponent(val shape: Shape, var color: Color, var scale: Double, var position: Vector2, var rotation: Int, var type: Type = Type.BODY)

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