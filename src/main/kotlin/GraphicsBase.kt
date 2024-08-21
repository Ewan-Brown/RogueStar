import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import kotlin.system.exitProcess

abstract class GraphicsBase : GLEventListener {
    fun setup(keyListener: KeyListener?) {
        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        window = GLWindow.create(glCapabilities)

        window!!.title = "Rogue Star"
        window!!.setSize(1024, 768)


        window!!.isVisible = true

        window!!.addGLEventListener(this)
        window!!.addKeyListener(keyListener)

        //        window.setAut
        animator = Animator(window)
        animator!!.start()

        window!!.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent) {
                animator!!.stop()
                exitProcess(1)
            }
        })
    }

    protected var EntityProgram: Program? = null
    protected var BackgroundProgram: Program? = null

    inner class Program(
        gl: GL3,
        root: String?,
        vertex: String?,
        fragment: String?,
        linkUniforms: Boolean
    ) {
        //TODO Make the 'program' class extendable it's being overused and overburdened!
        var name: Int
        var modelToWorldMatUL: Int = 0
        var positionInSpace: Int = 0
        var time: Int = 0

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

            //TODO This is overburdened
            if (linkUniforms) {
                modelToWorldMatUL = gl.glGetUniformLocation(name, "model")

                if (modelToWorldMatUL == -1) {
                    System.err.println("uniform 'model' not found!")
                }


                val globalMatricesBI = gl.glGetUniformBlockIndex(name, "GlobalMatrices")

                if (globalMatricesBI == -1) {
                    System.err.println("block index 'GlobalMatrices' not found!")
                }

                gl.glUniformBlockBinding(name, globalMatricesBI, 4)
            } else {
                positionInSpace = gl.glGetUniformLocation(name, "position")
                if (positionInSpace == -1) {
                    System.err.println("uniform 'position' not found!")
                }
                time = gl.glGetUniformLocation(name, "time")
                if (time == -1) {
                    System.err.println("uniform 'time' not found!")
                }
            }
        }
    }


    fun checkError(gl: GL, location: String) {
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

    companion object {
        private var window: GLWindow? = null
        private var animator: Animator? = null
    }
}
