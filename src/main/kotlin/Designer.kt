import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.MouseButton
import javafx.scene.layout.Pane
import javafx.stage.Stage
import org.dyn4j.geometry.Vector2
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document

class PolygonDrawerApp : Application() {

    private val polygons = mutableListOf<List<Vector2>>()

    override fun start(stage: Stage) {
        val pane = Pane()
        val canvas = Canvas(800.0, 600.0)
        val gc = canvas.graphicsContext2D
        drawGrid(gc)

        var currentPolygon = mutableListOf<Vector2>()

        canvas.setOnMouseClicked { event ->
            val pos = Vector2(event.x, event.y)
            if (event.button == MouseButton.PRIMARY) {
                currentPolygon.add(pos)
                redraw(gc)
                drawPolygon(gc, currentPolygon)
            } else if (event.button == MouseButton.SECONDARY) {
                if (currentPolygon.size >= 3) {
                    polygons.add(currentPolygon)
                    //TODO Open properties tab
//                    showPropertyMenu(pos) { properties ->
//                        polygons[polygons.lastIndex] = currentPolygon
//                    }
                }
                currentPolygon = mutableListOf()
            }
        }

        pane.children.add(canvas)
        stage.scene = Scene(pane)
        stage.title = "Polygon Drawer"
        stage.show()
    }

    private fun drawGrid(gc: GraphicsContext) {
        gc.clearRect(0.0, 0.0, 800.0, 600.0)
        for (x in 0..800 step 20) {
            gc.strokeLine(x.toDouble(), 0.0, x.toDouble(), 600.0)
        }
        for (y in 0..600 step 20) {
            gc.strokeLine(0.0, y.toDouble(), 800.0, y.toDouble())
        }
    }

    private fun drawPolygon(gc: GraphicsContext, points: List<Vector2>) {
        if (points.size > 1) {
            for (i in 1 until points.size) {
                val (x1, y1) = points[i - 1]
                val (x2, y2) = points[i]
                gc.strokeLine(x1, y1, x2, y2)
            }
        }
    }

    private fun redraw(gc: GraphicsContext) {
        drawGrid(gc)
        for (points in polygons) {
            drawPolygon(gc, points)
        }
    }

    private fun showPropertyMenu(x: Double, y: Double, onPropertiesSet: (Map<String, String>) -> Unit) {
        val menu = ContextMenu()
        val setProperties = MenuItem("Set Properties")
        setProperties.setOnAction {
            val properties = mapOf("color" to "red", "type" to "obstacle") // Replace with real dialog input
            onPropertiesSet(properties)
        }
        menu.items.add(setProperties)
//        menu.show(stage, x, y)
    }

    private fun exportToXml(file: File) {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.newDocument()
        val root = doc.createElement("Polygons")
        doc.appendChild(root)

        for (polygon in polygons) {
            val polygonElement = doc.createElement("Polygon")
            val pointsElement = doc.createElement("Points")
            polygon.forEach { (x, y) ->
                val pointElement = doc.createElement("Point")
                pointElement.setAttribute("x", x.toString())
                pointElement.setAttribute("y", y.toString())
                pointsElement.appendChild(pointElement)
            }
            polygonElement.appendChild(pointsElement)

//            properties.forEach { (key, value) ->
//                val propertyElement = doc.createElement("Property")
//                propertyElement.setAttribute("name", key)
//                propertyElement.textContent = value
//                polygonElement.appendChild(propertyElement)
//            }

            root.appendChild(polygonElement)
        }

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.transform(DOMSource(doc), StreamResult(file))
    }
}

fun main() {
    Application.launch(PolygonDrawerApp::class.java)
}
