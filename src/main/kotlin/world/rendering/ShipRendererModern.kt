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

    private val program: Program? = null

    private val vao = IntArray(1)
    private val vbo = IntArray(2)

    override fun init(p0: GLAutoDrawable?) {
        println("ShipRenderer.init")
        val gl4 = p0?.gl?.gL4
        if (gl4 != null) {
            setupVertices(gl4)
            Program(gl4, "", "ship-modern", "ship-modern")
        }
    }

    override fun dispose(p0: GLAutoDrawable?) {
//        TODO("Not yet implemented")
    }

    override fun display(p0: GLAutoDrawable?) {

        println("ShipRenderer.display")
        val gl4 = p0?.gl?.gL4

        if (gl4 != null && program != null) {
            println("doing it...")
            gl4.glClear(GL_DEPTH_BUFFER_BIT)
            val bkg = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
            val bkgBuffer = Buffers.newDirectFloatBuffer(bkg)
            gl4.glClearBufferfv(GL2ES3.GL_COLOR, 0, bkgBuffer)
            gl4.glUseProgram(program.name)

//            val mat4 = Matrix4()
//            mat4.translate(ship.getWorldPos().x.toFloat(), ship.getWorldPos().y.toFloat(), 0.0f)
//            mat4.rotate(0.0f,0.0f,0.0f,0.0f)
//            mat4.scale(0.1f,0.1f,0.1f)

//            val mv_loc = gl4.glGetUniformLocation(program.name, "mv_loc")

//            gl4.glUniformMatrix4fv(mv_loc, 1, false, mat4.matrix, 0)

            gl4.glBindBuffer(GL_ARRAY_BUFFER, vbo[0])
            gl4.glBindVertexArray(vao[0])
//            gl4.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0)
//            gl4.glEnableVertexAttribArray(0)

            gl4.glEnable(GL_DEPTH_TEST)
            gl4.glEnable(GL4.GL_PROGRAM_POINT_SIZE)
            gl4.glPointSize(2f)
//            gl4.glDepthFunc(GL_LEQUAL)
            gl4.glDrawArrays(GL_POINTS, 0, 7)
        }

    }

    override fun reshape(p0: GLAutoDrawable?, p1: Int, p2: Int, p3: Int, p4: Int) {
//        TODO("Not yet implemented")
    }

    fun setupVertices(gl : GL4) {

        gl.glGenVertexArrays(vao.size, vao, 0)
        gl.glBindVertexArray(vao[0])
        gl.glGenBuffers(vbo.size, vbo, 0)

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0])
        val vertBuf = Buffers.newDirectFloatBuffer(ship.getPointsAsFloats())
        gl.glBufferData(GL_ARRAY_BUFFER, (vertBuf.limit() * 4).toLong(), vertBuf, GL_STATIC_DRAW)
    }

    private class Program(gl: GL4?, root: String?, vertex: String?, fragment: String?) {
        var name: Int = 0

        init {
            val vertShader = ShaderCode.create(
                gl, GL2ES2.GL_VERTEX_SHADER, this.javaClass, root, null, vertex,
                "vert", null, true
            )
            val fragShader = ShaderCode.create(
                gl, GL2ES2.GL_FRAGMENT_SHADER, this.javaClass, root, null, fragment,
                "frag", null, true
            )

            val shaderProgram = ShaderProgram()

            shaderProgram.add(vertShader)
            shaderProgram.add(fragShader)

            shaderProgram.init(gl)

            name = shaderProgram.program()

            shaderProgram.link(gl, System.err)
        }
    }
}

fun main(args: Array<String>) {
    println("ShipRenderer")

    val ship = Ship(listOf(Vector2(1.0, 0.0), Vector2(0.0, 1.0), Vector2(1.0, 2.0), Vector2(2.0, 4.0), Vector2(3.0, 2.0), Vector2(4.0, 1.0), Vector2(3.0, 0.0)))
    val dim = Dimension(600,600)
    val renderer = ShipRendererModern(ship, dim.width, dim.height)

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