package graphics

import com.jogamp.opengl.*
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import kotlin.jvm.javaClass

open class Program(gl: GL3,root: String,vertex: String,fragment: String) {
    protected fun registerField(gl: GL3, fieldName: String) : Int {
        val fieldAddress = gl.glGetUniformLocation(name, fieldName)
        if (fieldAddress == -1) {
            println("did NOT find uniform '{$fieldName}' for program : $javaClass - check that it's being used in shader!")
        }else{
            println("did find '{$fieldName}' in program : $javaClass, $fieldAddress")
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

class BackgroundProgram(gl: GL3,root: String,fragment: String) : WorldProgram(gl, root,"Game_Background", fragment){}

class EntityProgram(gl: GL3,root: String, vertex: String,fragment: String) : WorldProgram(gl,root,vertex, fragment){}

open class WorldProgram(gl: GL3, root: String, vertex: String, fragment: String) : Program(gl,root,vertex,fragment){
    val velocity: Int = registerField(gl, "velocity")
    val viewMat: Int = registerField(gl, "viewZ")
}

class UIProgram(gl: GL3,root: String, vertex: String,fragment: String) : Program(gl,root,vertex,fragment){}
