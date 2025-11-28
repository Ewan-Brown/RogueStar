package graphics

import models.Model
import math.Transformation3
import math.Vector2
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import graphics.Graphics.Renderable
import java.awt.MouseInfo
import java.lang.Error
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.ArrayList
import kotlin.collections.associateWith
import kotlin.collections.forEach
import kotlin.collections.getValue
import kotlin.collections.indices
import kotlin.collections.set
import kotlin.collections.withIndex
import kotlin.system.exitProcess

data class CameraDetails(val targetPosition: Vector2, val targetScale: Double, val targetRotation: Double)
interface GraphicsI{
    fun getMousePositionInWorldCoordinates() : Vector2
    fun updateDrawables(data: Map<Model, List<Renderable>>)
    fun updateCamera(cameraDetails: CameraDetails)
    //TODO Genericize this!
    fun addListener(keyListener: KeyListener)
}

class Graphics(val loadedModels: List<Model>) : GraphicsI, GLEventListener {

    val width: Int = 600
    val height: Int = 600
    val window: GLWindow

    init {
        val glProfile = GLProfile.get(GLProfile.GL3)
        val glCapabilities = GLCapabilities(glProfile)

        window = GLWindow.create(glCapabilities)
        window.title = "Rogue Star"
        window.setSize(width,height)
        window.isVisible = true
        window.addGLEventListener(this)

        val animator = Animator(window)
        animator.start()

        window.addWindowListener(object : WindowAdapter() {
            override fun windowDestroyed(e: WindowEvent) {
                animator.stop()
                exitProcess(1)
            }
        })
    }

    private val VBOs: IntBuffer = GLBuffers.newDirectIntBuffer(VBONames.MAX)
    private val VAOs: IntBuffer = GLBuffers.newDirectIntBuffer(1)

    private val clearColor: FloatBuffer = GLBuffers.newDirectFloatBuffer(4)
    private val clearDepth: FloatBuffer = GLBuffers.newDirectFloatBuffer(1)

    private val matBuffer: FloatBuffer = GLBuffers.newDirectFloatBuffer(16)

    private val modelData = mutableMapOf<Model, ModelData>()

    var cameraPos: Vector2 = Vector2(0.0, 0.0)
    var cameraVelocity: Vector2 = Vector2(0.0, 0.0)
    var cameraScale: Float = 1.0f

    var entityProgram: EntityProgram? = null
    var backgroundProgram: BackgroundProgram? = null
    var uiProgram: UIProgram? = null

    //the time for the background
    var time: Float = 0f

    private inner class ModelData {
        var verticeIndex: Int = 0
        var instanceIndex: Int = 0
        var instanceData: List<Renderable> = ArrayList()
        val instanceCount: Int
            get() = instanceData.size
    }

    override fun getMousePositionInWorldCoordinates(): Vector2 {
        return transformScreenPosToGamePos(
            Vector2(MouseInfo.getPointerInfo()!!.location) - Vector2(this.window.getLocationOnScreen(null)!!)
        )
    }

    override fun updateDrawables(data: Map<Model, List<Renderable>>) {
        synchronized(modelData) {

            //Update graphics buffers
            for (loadedModel in loadedModels) {
                modelData.getValue(loadedModel).instanceData = data.getValue(loadedModel)
            }
        }
    }

    override fun updateCamera(cameraDetails: CameraDetails) {
        val diff = cameraDetails.targetPosition - cameraPos
        cameraVelocity = diff * 0.3
        cameraPos += cameraVelocity
    }

    override fun addListener(keyListener: KeyListener) {
        window.addKeyListener(keyListener)
    }

    private interface VBONames {
        companion object {
            const val VERTEX: Int = 1
            const val INSTANCED_POSITIONS: Int = 2
            const val INSTANCED_ROTATIONS: Int = 3
            const val INSTANCED_SCALES: Int = 4
            const val INSTANCED_COLORS: Int = 5
            const val INSTANCED_HEALTHS: Int = 6
            const val MAX: Int = 7
        }
    }


    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        for (preloadedModel in loadedModels) {
            modelData[preloadedModel] = ModelData()
        }

        initVBOs(gl)

        updateInstanceData(gl)

        initVAOs(gl)

        initProgram(gl)

        gl.glEnable(GL.GL_DEPTH_TEST)
    }

    private fun initVBOs(gl: GL3) {
        //Generate vertex data and store offsets for models

        val verticeList: MutableList<Float> = ArrayList()

        var marker = 0
        for (value in loadedModels) {
            for (vertexDatum in value.vertexData) {
                verticeList.add(vertexDatum)
            }
            modelData.getValue(value).verticeIndex = marker
            marker += value.points
        }

        val verticeArray = FloatArray(verticeList.size)
        for (i in verticeList.indices) {
            verticeArray[i] = verticeList[i]
        }

        val vertexBuffer = GLBuffers.newDirectFloatBuffer(verticeArray)

        gl.glGenBuffers(VBONames.MAX, VBOs) // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[VBONames.VERTEX])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer.capacity().toLong() * java.lang.Float.BYTES,
            vertexBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        checkError(gl, "initBuffers")
    }

    private fun initVAOs(gl: GL3) {
        gl.glGenVertexArrays(1, VAOs) // Create VAO
        gl.glBindVertexArray(VAOs[0])

        for (attribute in GENERAL_ATTRIBUTES.entries){
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[attribute.VBOBuffer])
            gl.glEnableVertexAttribArray(attribute.index)
            gl.glVertexAttribPointer(
                attribute.index,
                attribute.size,
                GL.GL_FLOAT,
                false,
                attribute.size * java.lang.Float.BYTES,
                0
            )
        }

        for (attribute in INSTANCED_ATTRIBUTE.entries) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[attribute.VBOBuffer])
            gl.glEnableVertexAttribArray(attribute.index)
            gl.glVertexAttribPointer(
                attribute.index,
                attribute.size,
                GL.GL_FLOAT,
                false,
                attribute.size * java.lang.Float.BYTES,
                0
            )
            gl.glVertexAttribDivisor(attribute.index, 1)
        }

        gl.glBindVertexArray(0)

        checkError(gl, "initVao")
    }

    private fun updateInstanceData(gl: GL3) {
        val modelCount =
            modelData.values.stream().mapToInt { obj: ModelData -> obj.instanceCount }
                .sum()

        //TODO Comment this better before i forget what's going on
        val attributeMap : Map<INSTANCED_ATTRIBUTE, FloatArray> = INSTANCED_ATTRIBUTE.entries.associateWith {
            FloatArray(
                it.size * modelCount
            )
        }
        val attributeMarkerMap : MutableMap<INSTANCED_ATTRIBUTE, Int> = mutableMapOf()
        INSTANCED_ATTRIBUTE.entries.forEach {attributeMarkerMap[it] = 0}
        var indexCounter = 0
        for ((_, data) in modelData) {
            //For each instance of that motel
            for (instancedDatum in data.instanceData) {
                for(attribute in INSTANCED_ATTRIBUTE.entries){
                    val floats = attribute.dataExtractor(instancedDatum)
                    val floatBuffer = attributeMap[attribute]
                    for ((index, float) in floats.withIndex()) {
                        //TODO without this, intermittent crashes. Why
                        if((attributeMarkerMap[attribute]!! + index) < floatBuffer!!.size){
                            floatBuffer!![attributeMarkerMap[attribute]!! + index] = float
                        }

                    }
                    attributeMarkerMap[attribute] = attributeMarkerMap[attribute]!! + floats.size
                }
            }
            data.instanceIndex = indexCounter
            indexCounter += data.instanceCount
        }
        //TODO We might be able to replace some glBufferData with glBufferSubData (avoiding unnecessary re-allocation)
        for (attribute in INSTANCED_ATTRIBUTE.entries) {
            val buffer = GLBuffers.newDirectFloatBuffer(attributeMap[attribute])
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[attribute.VBOBuffer])
            gl.glBufferData(
                GL.GL_ARRAY_BUFFER,
                buffer.capacity().toLong() * java.lang.Float.BYTES,
                buffer,
                GL.GL_DYNAMIC_DRAW
            )
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
    }

    private fun initProgram(gl: GL3) {

        //TODO Figure out if uniforms can be shared across programs??
        backgroundProgram = BackgroundProgram(gl, "", "Game_Background_Custom_Stars")
        checkError(gl, "initProgram : backGroundProgram")

        entityProgram = EntityProgram(gl, "", "Game_Entity", "Game_Entity")
        checkError(gl, "initProgram : entityProgram")

        uiProgram = UIProgram(gl, "", "Game_UI", "Game_UI")
        checkError(gl, "initProgram : uiProgram")

    }

    private fun calculateViewMat() : FloatArray {
        val scale = FloatUtil.makeScale(FloatArray(16), true, 0.06f * cameraScale, 0.06f * cameraScale, 0.03f) //FIXME There's something weird about this - try increasing sz to above 0.06
        val translate = FloatUtil.makeTranslation(FloatArray(16), 0, true, -cameraPos.getX().toFloat(), -cameraPos.getY().toFloat(), 0f)
        val rotate = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0.0f, 0.0f , 0.0f)
        return FloatUtil.multMatrix(FloatUtil.multMatrix(scale, rotate), translate)
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3
        synchronized(modelData) {
            updateInstanceData(gl)
            // view matrix
            val view = FloatArray(16)
            FloatUtil.makeIdentity(view)

            val viewMat = calculateViewMat()
            for (i in 0..15) {
                matBuffer.put(i, viewMat[i])
            }

            gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, clearColor.put(0, 0f).put(1, .33f).put(2, 0.66f).put(3, 1f))
            gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, clearDepth.put(0, 1f))

            gl.glBindVertexArray(VAOs[0])

            gl.glUseProgram(backgroundProgram!!.name)
            gl.glUniformMatrix4fv(backgroundProgram!!.viewMat, 1, false, matBuffer)
//            gl.glUniform1f(backgroundProgram!!.time, time)
//            gl.glUniform2f(backgroundProgram!!.velocity, cameraVelocity.x.toFloat(), cameraVelocity.y.toFloat())
            gl.glUniform1f(backgroundProgram!!.time, 0.0f)
            gl.glUniform2f(backgroundProgram!!.velocity, 0.0f, 0.0f)

            gl.glDrawArrays(
                Model.BACKPLATE.drawMode,
                modelData.getValue(Model.BACKPLATE).verticeIndex,
                Model.BACKPLATE.points
            )

            gl.glUseProgram(0)
            gl.glUseProgram(entityProgram!!.name)
            gl.glUniformMatrix4fv(entityProgram!!.viewMat, 1, false, matBuffer)
            gl.glUniform2f(backgroundProgram!!.velocity, cameraVelocity.getX().toFloat(), cameraVelocity.getY().toFloat())
            gl.glUniform1f(entityProgram!!.time, time)

            for ((model, data) in modelData) {
                if (data.instanceCount > 0) {
                    gl.glDrawArraysInstancedBaseInstance(
                        model.drawMode,
                        data.verticeIndex,
                        model.points,
                        data.instanceCount,
                        data.instanceIndex
                    )
                }
            }
        }

        gl.glUseProgram(0)
        gl.glBindVertexArray(0)

        checkError(gl, "display")

        time += 1f
    }

    private fun transformScreenPosToGamePos(screenPos : Vector2) : Vector2 {
        val adjustedScreenPos =
            Vector2((screenPos.getX() / width.toDouble()) * 2 - 1, -(screenPos.getY() / height.toDouble()) * 2 + 1)
        val viewMat4x4Flattened = FloatUtil.invertMatrix(calculateViewMat(), FloatArray(16))
        val vec4 = FloatUtil.multMatrixVec(viewMat4x4Flattened, floatArrayOf(adjustedScreenPos.getX().toFloat(), adjustedScreenPos.getY().toFloat(), 0.0f, 1.0f),
            FloatArray(16)
        )
        return Vector2(vec4[0].toDouble(), vec4[1].toDouble())
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.gl.gL3

        gl.glViewport(x, y, width, height)
    }

    override fun dispose(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        gl.glDeleteProgram(entityProgram!!.name)
        gl.glDeleteProgram(backgroundProgram!!.name)
        gl.glDeleteVertexArrays(1, VAOs)
        gl.glDeleteBuffers(VBONames.MAX, VBOs)
        checkError(gl, "dispose() : deleting resources")
    }

    /**
     * Stores RGBA, each from 0.0 - 1.0
     */
    //TODO generalize vector math so it can be reused here?
    data class ColorData(val red: Float, val green: Float, val blue: Float, val alpha: Float)
    class MetaData(val health: Float ) //TODO this could vary across entities - Maybe make this... a builder?
    class Renderable(val model: Model, val transform: Transformation3, val colorData: ColorData, val metaData: MetaData)

    enum class INSTANCED_ATTRIBUTE(val index: Int, val size: Int, val dataExtractor: (Renderable) -> List<Float>, val VBOBuffer: Int){
        POSITION(1, 3, {listOf(it.transform.translation.getX().toFloat(), it.transform.translation.getY().toFloat(), it.transform.translation.getZ().toFloat())},
            VBONames.INSTANCED_POSITIONS
        ),
        ROTATION(2, 1, {listOf(it.transform.rotation.toFloat())}, VBONames.INSTANCED_ROTATIONS),
        SCALE(3, 1, {listOf(it.transform.scale.toFloat())}, VBONames.INSTANCED_SCALES),
        COLOR(4, 3, {listOf(it.colorData.red, it.colorData.green, it.colorData.blue)}, VBONames.INSTANCED_COLORS),
        HEALTH(5, 1, {listOf(it.metaData.health)}, VBONames.INSTANCED_HEALTHS)
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

    //TODO If you add to this what happens to the indices...?
    enum class GENERAL_ATTRIBUTES(val index: Int, val size: Int, val VBOBuffer: Int){
        POSITION(0, 3, VBONames.VERTEX)
    }

}