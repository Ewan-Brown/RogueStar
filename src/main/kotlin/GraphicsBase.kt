import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.MouseListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import kotlin.system.exitProcess

abstract class GraphicsBase : GLEventListener {
    fun setup(keyListener: KeyListener, mouseListener: MouseListener) {
        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        val window = GLWindow.create(glCapabilities)

        window.title = "Rogue Star"
        window.setSize(600,600)


        window.isVisible = true

        window.addGLEventListener(this)
        window.addKeyListener(keyListener)
        window.addMouseListener(mouseListener)

        //        window.setAut
        val animator = Animator(window)
        animator.start()

        window.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent) {
                animator.stop()
                exitProcess(1)
            }
        })
    }

    open inner class Program(gl: GL3,root: String,vertex: String,fragment: String) {
        protected fun registerField(gl: GL3, fieldName: String) : Int {
            val fieldAddress = gl.glGetUniformLocation(name, fieldName)
            if (fieldAddress == -1) {
                System.err.println("uniform '{$fieldName}' not found for program : " + javaClass)
            }
            return fieldAddress
        }
        //TODO Make the 'program' class extendable it's being overused and overburdened!
        val name: Int
        val time: Int

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
            time = registerField(gl, "time")

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
}
