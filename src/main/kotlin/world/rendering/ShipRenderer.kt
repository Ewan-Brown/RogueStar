package world.rendering

import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import org.dyn4j.geometry.Vector2
import world.entity.Ship
import javax.swing.JFrame

class ShipRenderer(val ship: Ship) : GLEventListener {

    override fun init(p0: GLAutoDrawable?) {
        TODO("Not yet implemented")
    }

    override fun dispose(p0: GLAutoDrawable?) {
        TODO("Not yet implemented")
    }

    override fun display(p0: GLAutoDrawable?) {
        TODO("Not yet implemented")
    }

    override fun reshape(p0: GLAutoDrawable?, p1: Int, p2: Int, p3: Int, p4: Int) {
        TODO("Not yet implemented")
    }
}

fun main(args: Array<String>) {
    println("ShipRenderer")

    val ship = Ship(listOf(Vector2(0.0, 0.0), Vector2(1.0, 0.0), Vector2(1.0, 1.0), Vector2(0.0, 1.0)))

    val frame = JFrame("Ship Renderer")
    val canvas = GLCanvas(frame)
    val glCaps = GLCapabilities(GLProfile.getDefault())
    val renderer = ShipRenderer(ship)

    frame.setSize(600, 600);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    glCaps.doubleBuffered = true
    glCaps.hardwareAccelerated = true


}