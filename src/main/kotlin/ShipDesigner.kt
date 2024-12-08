import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
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

    private var selectedShape: Shape? = shapes[0]
    var selectedColor: Color? = null
    var selectedQuarterRotations: Int = 0
    var selectedType: Type? = null

    override fun paint(g: Graphics) {
        super.paint(g)

        g.color = Color.BLACK
        g.fillRect(0, 0, width, height)

        //Draw background grid
        paintGrid(g)

        //Draw existing shapes
        for (component in components){
//            point
        }
        println(selectedQuarterRotations)
        //Draw outline of current shape in progress
        selectedShape?.let { shape ->
            g.color = selectedColor ?: Color.CYAN
            val position = getRoundedMousePos()
            val transformedPoints = transformPolygon(shape, position, selectedQuarterRotations, 1.0)
            paintPolygon(g, transformedPoints)
        }
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

    private fun paintPolygon(g: Graphics, points: List<Vector2>){
        val poly = vectorListToPolygon(points)
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
            val pos = getRoundedMousePos(e)
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
        println("DesignerUI.keyPressed")
        if(e.keyCode == KeyEvent.VK_ENTER){
            exportToFile()
        }

        if(e.keyCode == KeyEvent.VK_SPACE){
            selectedShape = shapes[(shapes.indexOf(selectedShape) + 1) % shapes.size]
            println("switching shape")
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
    BODY
}

data class SimpleComponent(val shape: Shape, var scale: Double, var position: Vector2, var rotation: Double, var type: Type = Type.BODY)

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