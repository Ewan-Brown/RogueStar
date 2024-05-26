package world.rendering

import com.jogamp.common.nio.Buffers
import com.jogamp.opengl.*
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.math.Matrix4
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import org.dyn4j.geometry.Vector2
import world.entity.Ship
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame


class ShipRendererModern(private val ship: Ship, private val width: Int, private val height: Int) : GLEventListener {

    override fun init(p0: GLAutoDrawable?) {
        println("ShipRenderer.init")
    }

    override fun dispose(p0: GLAutoDrawable?) {
//        TODO("Not yet implemented")
    }

    override fun display(p0: GLAutoDrawable?) {
        println("ShipRenderer.display")
        val gl4 = p0?.gl?.gL4

    }

    override fun reshape(p0: GLAutoDrawable?, p1: Int, p2: Int, p3: Int, p4: Int) {
//        TODO("Not yet implemented")
    }
}

fun main(args: Array<String>) {
    println("ShipRenderer")

    val ship = Ship(listOf(Vector2(1.0, 0.0), Vector2(0.0, 1.0), Vector2(1.0, 2.0), Vector2(2.0, 4.0), Vector2(3.0, 2.0), Vector2(4.0, 1.0), Vector2(3.0, 0.0)))
    val dim = Dimension(600,600)
    val renderer = ShipRendererModern(ship, dim.width, dim.height)

    val glCaps = GLCapabilities(GLProfile.get(GLProfile.GL3))
    glCaps.doubleBuffered = true
    glCaps.hardwareAccelerated = true

    val canvas = GLCanvas(glCaps)
    canvas.ignoreRepaint = true;
    canvas.setPreferredSize(dim)
    canvas.setMinimumSize(dim)
    canvas.setMaximumSize(dim)
    canvas.ignoreRepaint = true
    canvas.addGLEventListener(renderer)

    val frame = JFrame("Ship Renderer")
    frame.layout = BorderLayout()
    frame.setSize(600, 600)
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    frame.isResizable = false
    frame.add(canvas)
    frame.isVisible = true

    println(frame.size)
    println(canvas.size)

    frame.pack()

    val animator = Animator(canvas)
    animator.setRunAsFastAsPossible(true)
    animator.start()
}