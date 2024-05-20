package world.rendering

import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.util.Animator
import org.dyn4j.geometry.Vector2
import world.entity.Ship
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame

class ShipRenderer(private val ship: Ship, private val width: Int, private val height: Int) : GLEventListener {

    override fun init(p0: GLAutoDrawable?) {
        println("ShipRenderer.init")
        val gl2 = p0?.gl?.gL2

        if(gl2 != null) {
            gl2.glMatrixMode(GL2.GL_PROJECTION)
            gl2.glLoadIdentity()
            gl2.glOrtho(-1.0, 1.0, -1.0, 1.0, 0.0, 1.0)
            gl2.glViewport(0, 0, width, height)

            gl2.glMatrixMode(GL2.GL_MODELVIEW)
            gl2.glLoadIdentity()

            gl2.glEnable(GL.GL_BLEND)
            gl2.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA)
            gl2.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

            gl2.swapInterval = 0


        }
    }

    override fun dispose(p0: GLAutoDrawable?) {
//        TODO("Not yet implemented")
    }

    override fun display(p0: GLAutoDrawable?) {


        println("ShipRenderer.display")
        val gl2 = p0?.gl?.gL2

        if(gl2 != null) {
            // clear the screen
            gl2.glClear(GL.GL_COLOR_BUFFER_BIT)

            // switch to the model view matrix
            gl2.glMatrixMode(GL2.GL_MODELVIEW)

            gl2.glLoadIdentity()

            gl2.glColor4f(0.0f, 1.0f, 0.0f, 1.0f)
            gl2.glScaled(0.1, 0.1, 1.0)
            gl2.glBegin(GL2.GL_POLYGON)

            for (normalizedPoint in ship.normalizedPoints) {
                gl2.glVertex2f(normalizedPoint.x.toFloat(), normalizedPoint.y.toFloat())
            }
            gl2.glEnd()
        }

}

    override fun reshape(p0: GLAutoDrawable?, p1: Int, p2: Int, p3: Int, p4: Int) {
//        TODO("Not yet implemented")
    }
}

fun main(args: Array<String>) {
    println("ShipRenderer")

    val ship = Ship(listOf(Vector2(1.0, 0.0), Vector2(0.0, 1.0), Vector2(1.0, 2.0), Vector2(2.0, 4.0), Vector2(3.0, 2.0), Vector2(4.0, 1.0), Vector2(3.0, 0.0)))
    val dim = Dimension(600,600)
    val renderer = ShipRenderer(ship, dim.width, dim.height)

    val glCaps = GLCapabilities(GLProfile.get(GLProfile.GL2))
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