package world.rendering

import com.jogamp.opengl.*
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import jogl.HelloTriangle_Base
import jogl.Semantic
import jogl.separate.HelloTriangle_5_shared_vbo_one_call
import org.dyn4j.geometry.Vector2
import world.entity.Component
import world.entity.ComponentProperties
import world.entity.Entity
import world.entity.StaticUniqueComponentEntity
import java.awt.BorderLayout
import java.awt.Dimension
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import javax.swing.JFrame


class EntityRenderer(private val entities: List<Entity>, private val width: Int, private val height: Int) : HelloTriangle_Base() {

    private val registeredModels = mutableSetOf<Model>()

    private val VBOs: IntBuffer = GLBuffers.newDirectIntBuffer(Buffers.MAX)
    private val VAOs: IntBuffer = GLBuffers.newDirectIntBuffer(1)

    private val clearColor: FloatBuffer = GLBuffers.newDirectFloatBuffer(4)
    private val clearDepth: FloatBuffer = GLBuffers.newDirectFloatBuffer(1)
    private val matBuffer: FloatBuffer = GLBuffers.newDirectFloatBuffer(16)

    override fun init(p0: GLAutoDrawable?) {
        val gl = p0?.gl?.gL3 ?: throw Exception("gl null!")

        //Move this to a re-callable spot when new entities are added!
        for (entity in entities) {
            registeredModels.addAll(entity.getModels())
        }

        val vertexList: MutableList<Float> = mutableListOf()
        val elementList: MutableList<Short> = mutableListOf()
        for (registeredModel in registeredModels) {
            //Inject colors... for now
            for (point in registeredModel.points) {
                vertexList.add(point.x.toFloat())
                vertexList.add(point.y.toFloat())
                vertexList.addAll(listOf(1.0f,0.0f,0.0f))
            }
        }

        val shortList: List<Short> = (0..<(vertexList.size/5)).map { it.toShort() }

        val vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexList.toFloatArray())
        val elementBuffer = GLBuffers.newDirectShortBuffer(shortList.toShortArray())

        println(vertexBuffer)
        println(elementBuffer)
        initVBOs(gl, vertexBuffer, elementBuffer)
        initVAOs(gl)
        initProgram(gl)

        gl.glEnable(GL.GL_DEPTH_TEST)

    }

    private fun initVBOs(gl: GL3, vertexBuffer: FloatBuffer, elementBuffer: ShortBuffer) {

        gl.glGenBuffers(Buffers.MAX, VBOs) // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffers.VERTEX])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            (vertexBuffer.capacity() * java.lang.Float.BYTES).toLong(),
            vertexBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOs[Buffers.ELEMENT])
        gl.glBufferData(
            GL.GL_ELEMENT_ARRAY_BUFFER,
            (elementBuffer.capacity() * java.lang.Short.BYTES).toLong(),
            elementBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0)

        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, VBOs[Buffers.GLOBAL_MATRICES])
        gl.glBufferData(
            GL2ES3.GL_UNIFORM_BUFFER,
            (16 * java.lang.Float.BYTES * 2).toLong(),
            null,
            GL2ES2.GL_STREAM_DRAW
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

        gl.glBindBufferBase(
            GL2ES3.GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES,
            VBOs[Buffers.GLOBAL_MATRICES]
        )

        checkError(gl, "initBuffers")
    }

    private fun initVAOs(gl: GL3) {
        gl.glGenVertexArrays(1, VAOs) // Create VAO
        gl.glBindVertexArray(VAOs[0])
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffers.VERTEX])
        val stride = (2 + 3) * java.lang.Float.BYTES
        var offset = 0

        gl.glEnableVertexAttribArray(Semantic.Attr.POSITION)
        gl.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL.GL_FLOAT, false, stride, offset.toLong())

        offset = 2 * java.lang.Float.BYTES
        gl.glEnableVertexAttribArray(Semantic.Attr.COLOR)
        gl.glVertexAttribPointer(Semantic.Attr.COLOR, 3, GL.GL_FLOAT, false, stride, offset.toLong())
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOs[Buffers.ELEMENT])
        gl.glBindVertexArray(0)

        checkError(gl, "initVao")
    }

    private fun initProgram(gl: GL3) {
        program = Program(gl, javaClass, "", "entity_renderer", "entity_renderer")

        checkError(gl, "initProgram")
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        // view matrix
        run {
            val view = FloatArray(16)
            FloatUtil.makeIdentity(view)

            for (i in 0..15) {
                matBuffer.put(i, view[i])
            }
            gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, VBOs[Buffers.GLOBAL_MATRICES])
            gl.glBufferSubData(
                GL2ES3.GL_UNIFORM_BUFFER,
                (16 * java.lang.Float.BYTES).toLong(),
                (16 * java.lang.Float.BYTES).toLong(),
                matBuffer
            )
            gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)
        }

        gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, clearColor.put(0, 0f).put(1, .33f).put(2, 0.66f).put(3, 1f))
        gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, clearDepth.put(0, 1f))

        gl.glUseProgram(program.name)
        gl.glBindVertexArray(VAOs[0])

        // model matrix
        run {
            val scale = FloatUtil.makeScale(FloatArray(16), true, 0.5f, 0.5f, 0.5f)
            val zRotation = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0f, 0f, 0.0f)
            val modelToWorldMat = FloatUtil.multMatrix(scale, zRotation)

            for (i in 0..15) {
                matBuffer.put(i, modelToWorldMat[i])
            }
            gl.glUniformMatrix4fv(program.modelToWorldMatUL, 1, false, matBuffer)
        }

        gl.glDrawElements(GL.GL_TRIANGLES, 6, GL.GL_UNSIGNED_SHORT, 0)

        gl.glUseProgram(0)
        gl.glBindVertexArray(0)

        checkError(gl, "display")
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.gl.gL3

        val ortho = FloatArray(16)
        FloatUtil.makeOrtho(ortho, 0, false, -1f, 1f, -1f, 1f, 1f, -1f)
        for (i in 0..15) {
            matBuffer.put(i, ortho[i])
        }
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, VBOs[Buffers.GLOBAL_MATRICES])
        gl.glBufferSubData(GL2ES3.GL_UNIFORM_BUFFER, 0, (16 * java.lang.Float.BYTES).toLong(), matBuffer)
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

        gl.glViewport(x, y, width, height)
    }

    override fun dispose(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        gl.glDeleteProgram(program.name)
        gl.glDeleteVertexArrays(1, VAOs)
        gl.glDeleteBuffers(Buffers.MAX, VBOs)
    }

}

object Buffers {
    const val VERTEX: Int = 0
    const val ELEMENT: Int = 1
    const val GLOBAL_MATRICES: Int = 2
    const val MAX: Int = 3
}

fun main(args: Array<String>) {
    println("ShipRenderer")

    val dim = Dimension(600,600)
    val renderer = EntityRenderer(listOf(
        StaticUniqueComponentEntity(setOf(Component(Models.triangle, ComponentProperties(Vector2(0.0, 0.0), 1.0))))
    ), dim.width, dim.height)

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