package jogl

import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import world.rendering.Semantic
import java.nio.FloatBuffer
import java.nio.IntBuffer

fun main() {
    Instanced_Triangles().setup()
}

class Instanced_Triangles : GLEventListener, KeyListener {
    var window: GLWindow? = null
    var animator: Animator? = null

    private val vertexData1 = floatArrayOf(
        -1f, -1f, 1f, 0f, 0f,
        +0f, +2f, 0f, 0f, 1f,
        +1f, -1f, 0f, 1f, 0f
    )

    val TRIANGLE_COUNT: Int = 10000

    private val elementData1 = shortArrayOf(0, 1, 2)

    private interface Buffer {
        companion object {
            const val VERTEX1: Int = 0
            const val ELEMENT1: Int = 1
            const val INSTANCED_STUFF: Int = 2
            const val GLOBAL_MATRICES: Int = 3
            const val MAX: Int = 4
        }
    }

    private val VBOs: IntBuffer = GLBuffers.newDirectIntBuffer(Buffer.MAX)
    private val VAOs: IntBuffer = GLBuffers.newDirectIntBuffer(1)

    private val clearColor: FloatBuffer = GLBuffers.newDirectFloatBuffer(4)
    private val clearDepth: FloatBuffer = GLBuffers.newDirectFloatBuffer(1)

    private val matBuffer: FloatBuffer = GLBuffers.newDirectFloatBuffer(16)

    private var program: Program? = null

    private var start: Long = 0

    public fun setup() {
        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        var window = GLWindow.create(glCapabilities)

        window.setTitle("Hello Triangle (simple)")
        window.setSize(1024, 768)

        window.setVisible(true)

        window.addGLEventListener(this)
        window.addKeyListener(this)

        animator = Animator(window)
        animator!!.start()

        window.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent) {
                animator!!.stop()
                System.exit(1)
            }
        })
    }

    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        initVBOs(gl)

        initVAOs(gl)

        initProgram(gl)

        gl.glEnable(GL.GL_DEPTH_TEST)

        start = System.currentTimeMillis()
    }

    private fun initVBOs(gl: GL3) {
        //Generate Instance data

        val instanceData = FloatArray(TRIANGLE_COUNT * 3)

        for (i in 0 until TRIANGLE_COUNT) {
            val x = (Math.random() - 0.5).toFloat() * 18.0f
            val y = (Math.random() - 0.5).toFloat() * 18.0f
            val angle = (Math.random() * Math.PI * 2).toFloat()
            instanceData[i * 3] = x
            instanceData[i * 3 + 1] = y
            instanceData[i * 3 + 2] = angle
        }

        val vertexBuffer1 = GLBuffers.newDirectFloatBuffer(vertexData1)
        val elementBuffer1 = GLBuffers.newDirectShortBuffer(elementData1)
        val instanceBuffer = GLBuffers.newDirectFloatBuffer(instanceData)

        gl.glGenBuffers(Buffer.MAX, VBOs) // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.VERTEX1])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer1.capacity().toLong() * java.lang.Float.BYTES,
            vertexBuffer1,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOs[Buffer.ELEMENT1])
        gl.glBufferData(
            GL.GL_ELEMENT_ARRAY_BUFFER,
            elementBuffer1.capacity().toLong() * java.lang.Short.BYTES,
            elementBuffer1,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0)

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_STUFF])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            instanceBuffer.capacity().toLong() * java.lang.Float.BYTES,
            instanceBuffer,
            GL.GL_DYNAMIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, VBOs[Buffer.GLOBAL_MATRICES])
        gl.glBufferData(
            GL2ES3.GL_UNIFORM_BUFFER,
            (16 * java.lang.Float.BYTES * 2).toLong(),
            null,
            GL2ES2.GL_STREAM_DRAW
        )
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

        gl.glBindBufferBase(
            GL2ES3.GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES,
            VBOs[Buffer.GLOBAL_MATRICES]
        )

        checkError(gl, "initBuffers")
    }

    var POSITION_ATTRIB_INDICE: Int = 0
    var COLOR_ATTRIB_INDICE: Int = 1
    var INSTANCE_POSITION_ATTRIB_INDICE: Int = 2


    private fun initVAOs(gl: GL3) {
        gl.glGenVertexArrays(1, VAOs) // Create VAO
        gl.glBindVertexArray(VAOs[0])
        run {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.VERTEX1])
            val stride = (2 + 3) * java.lang.Float.BYTES
            var offset = 0

            gl.glEnableVertexAttribArray(POSITION_ATTRIB_INDICE)
            gl.glVertexAttribPointer(POSITION_ATTRIB_INDICE, 2, GL.GL_FLOAT, false, stride, offset.toLong())

            offset = 2 * java.lang.Float.BYTES
            gl.glEnableVertexAttribArray(COLOR_ATTRIB_INDICE)
            gl.glVertexAttribPointer(COLOR_ATTRIB_INDICE, 3, GL.GL_FLOAT, false, stride, offset.toLong())

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_STUFF])
            gl.glEnableVertexAttribArray(INSTANCE_POSITION_ATTRIB_INDICE)
            gl.glVertexAttribPointer(
                INSTANCE_POSITION_ATTRIB_INDICE,
                3,
                GL.GL_FLOAT,
                false,
                3 * java.lang.Float.BYTES,
                0
            )

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
            gl.glVertexAttribDivisor(INSTANCE_POSITION_ATTRIB_INDICE, 1)

            //TODO We can probably remove the element array buffer yeah? Others seem to not use it...
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, VBOs[Buffer.ELEMENT1])
        }
        gl.glBindVertexArray(0)

        checkError(gl, "initVao")
    }

    private fun initProgram(gl: GL3) {
        program = Program(gl, javaClass, "", "hello-triangle_2", "hello-triangle_2")

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
            gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, VBOs[Buffer.GLOBAL_MATRICES])
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

        gl.glUseProgram(program!!.name)
        gl.glBindVertexArray(VAOs[0])

        // model matrix
        run {
            val scale = FloatUtil.makeScale(FloatArray(16), true, 0.1f, 0.1f, 0.1f)
            val zRotation = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0f, 0f, 0.0f)
            val modelToWorldMat = FloatUtil.multMatrix(scale, zRotation)

            for (i in 0..15) {
                matBuffer.put(i, modelToWorldMat[i])
            }
            gl.glUniformMatrix4fv(program!!.modelToWorldMatUL, 1, false, matBuffer)
        }

        //        gl.glDrawElements(GL_TRIANGLES, elementData1.length, GL_UNSIGNED_SHORT, 0);
        gl.glDrawArraysInstanced(GL.GL_TRIANGLES, 0, elementData1.size, TRIANGLE_COUNT)
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
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, VBOs[Buffer.GLOBAL_MATRICES])
        gl.glBufferSubData(GL2ES3.GL_UNIFORM_BUFFER, 0, (16 * java.lang.Float.BYTES).toLong(), matBuffer)
        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

        gl.glViewport(x, y, width, height)
    }

    override fun dispose(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        gl.glDeleteProgram(program!!.name)
        gl.glDeleteVertexArrays(1, VAOs)
        gl.glDeleteBuffers(Buffer.MAX, VBOs)
    }


    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            Thread {
                window!!.destroy()
            }.start()
        }
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    private class Program(gl: GL3, context: Class<*>?, root: String?, vertex: String?, fragment: String?) {
        var name: Int
        var modelToWorldMatUL: Int

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


            modelToWorldMatUL = gl.glGetUniformLocation(name, "model")

            if (modelToWorldMatUL == -1) {
                System.err.println("uniform 'model' not found!")
            }


            val globalMatricesBI = gl.glGetUniformBlockIndex(name, "GlobalMatrices")

            if (globalMatricesBI == -1) {
                System.err.println("block index 'GlobalMatrices' not found!")
            }
            gl.glUniformBlockBinding(name, globalMatricesBI, Semantic.Uniform.GLOBAL_MATRICES)
        }
    }

    private fun checkError(gl: GL, location: String) {
        val error = gl.glGetError()
        if (error != GL.GL_NO_ERROR) {
            val errorString = when (error) {
                GL.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
                GL.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
                GL.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
                GL.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
                GL.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
                else -> "UNKNOWN"
            }
            throw Error("OpenGL Error($errorString): $location")
        }
    }
}