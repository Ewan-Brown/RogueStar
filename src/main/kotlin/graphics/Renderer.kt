package graphics

import codec.VectorDeserializer
import codec.VectorSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import models.Model
import math.Vector2
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.event.WindowAdapter
import com.jogamp.newt.event.WindowEvent
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.math.FloatUtil
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import designers.Shape
import graphics.Renderer.ColorData
import math.*
import java.awt.MouseInfo
import java.lang.Error
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.collections.associateWith
import kotlin.collections.forEach
import kotlin.collections.getValue
import kotlin.collections.indices
import kotlin.collections.set
import kotlin.collections.withIndex
import kotlin.system.exitProcess

data class DebugLineData(val p1: Coordinates<WorldReferenceFrame>, val p2: Coordinates<WorldReferenceFrame>, val colorData1: Renderer.ColorData, val colorData2: Renderer.ColorData)
data class CameraDetails(val targetPosition: Vector2, val targetScale: Double, val targetRotation: Double)
interface RendererI{
    fun getMousePositionInWorldCoordinates() : Vector2
    fun updateDrawables(data: Map<Model, List<Renderer.Renderable>>)
    fun updateDebug(debugLines: List<DebugLineData>)
    fun updateCamera(cameraDetails: CameraDetails)
    //TODO Genericize this!
    fun addListener(keyListener: KeyListener)
}


val RED = ColorData(1.0f, 0.0f, 0.0f, 1.0f)
val GREEN = ColorData(0.0f, 1.0f, 0.0f, 1.0f)
val BLUE = ColorData(0.0f, 0.0f, 1.0f, 1.0f)
val WHITE = ColorData(1.0f, 1.0f, 1.0f, 1.0f)
val BLACK = ColorData(0.0f, 0.0f, 0.0f, 1.0f)
val CYAN = ColorData(0.0f, 1.0f, 1.0f, 1.0f)
val PURPLE = ColorData(0.5f, 0.0f, 0.5f, 1.0f)

fun loadModels() : Map<Int, Model> {
    val mapper = ObjectMapper()
    val module = SimpleModule()
    module.addSerializer(Vector2::class.java, VectorSerializer())
    module.addDeserializer(Vector2::class.java, VectorDeserializer())
    mapper.registerModules(module)
    val stream = Renderer::class.java.getResourceAsStream("/entities/shapes.json")
    val shapes = mapper.readValue(stream, Array<Shape>::class.java).toList()
    return shapes.associate { shape ->
        val points = shape.points.map { listOf(it.getX().toFloat() / 30.0f, it.getY().toFloat() / 30.0f, 0.0f) }.flatten().toFloatArray()
        shape.ID to Model(points, GL.GL_TRIANGLE_FAN)
    }
}

class Renderer(val loadedModels: List<Model>) : RendererI, GLEventListener {

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

    private val modelDataMap = mutableMapOf<Model, ModelData>()
    private val debugLines = mutableListOf<DebugLineData>()

    var cameraPos: Vector2 = Vector2(0.0, 0.0)
    var cameraVelocity: Vector2 = Vector2(0.0, 0.0)
    var cameraScale: Float = 1.0f

    var entityProgram: EntityProgram? = null
    var backgroundProgram: BackgroundProgram? = null
    var uiProgram: UIProgram? = null
    var debugProgram: DebugLineProgram? = null

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
        synchronized(modelDataMap) {
            //Update graphics buffers
            for (loadedModel in loadedModels) {
                if(modelDataMap.contains(loadedModel)) {
                    modelDataMap.getValue(loadedModel).instanceData = data.getValue(loadedModel)
                }
            }
        }
    }

    override fun updateDebug(debugLines: List<DebugLineData>) {
        this.debugLines.clear()
        this.debugLines.addAll(debugLines)
    }

    override fun updateCamera(cameraDetails: CameraDetails) {
        val diff = cameraDetails.targetPosition - cameraPos
        println(cameraDetails.targetPosition)
        cameraVelocity = diff * 0.03
        cameraPos += cameraVelocity
    }

    override fun addListener(keyListener: KeyListener) {
        window.addKeyListener(keyListener)
    }

    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        try{
            for (preloadedModel in loadedModels) {
                modelDataMap[preloadedModel] = ModelData()
            }
        }catch (e: NullPointerException){
            System.err.println("modelDataMap was not correctly initialized before being referenced")
        }


        gl.glGenBuffers(VBONames.MAX, VBOs) // Create VBOs (n = Buffer.max)
        populateStaticVBOs(gl)
        populateDynamicVBOs(gl)
        initializeVertexAttributes(gl)
        initializePrograms(gl)

        gl.glEnable(GL.GL_DEPTH_TEST)
    }

    /**
     * Populate VBOs whos data never changes
     */
    private fun populateStaticVBOs(gl: GL3) {
        //Push Model Vertex Data
        val vertexBuffer = GLBuffers.newDirectFloatBuffer(getModelVertices())
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[VBONames.MODEL_VERTICES])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer.capacity().toLong() * java.lang.Float.BYTES,
            vertexBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        checkError(gl, "initBuffers")
    }

    /**
     * Populate VBOs whose data may change
     */
    private fun populateDynamicVBOs(gl: GL3) {

        val modelCount = modelDataMap.values.stream().mapToInt { obj: ModelData -> obj.instanceCount }.sum()

        val attributeMap : Map<InstancedAttributes, FloatArray> = InstancedAttributes.entries.associateWith {
            FloatArray(
                it.size * modelCount
            )
        }

        val attributeMarkerMap : MutableMap<InstancedAttributes, Int> = mutableMapOf()
        InstancedAttributes.entries.forEach {attributeMarkerMap[it] = 0}
        var indexCounter = 0

        for ((_, data) in modelDataMap) {
            //For each instance of that model
            for (instancedDatum in data.instanceData) {
                for(attribute in InstancedAttributes.entries){
                    val floats = attribute.dataExtractor(instancedDatum)
                    val floatBuffer = attributeMap[attribute]
                    for ((index, float) in floats.withIndex()) {
                        //TODO without this size check, intermittent outofbounds exceptions... Why?
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
        for (attribute in InstancedAttributes.entries) {
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

        //Push debug line vertex data
        val debugLineVertices = debugLines.flatMap{listOf(it.p1.getX().toFloat(), it.p1.getY().toFloat(), 0.0f, it.p2.getX().toFloat(), it.p2.getY().toFloat(), 0.0f)}.toList().toFloatArray()
        val debugLineVertexBuffer = GLBuffers.newDirectFloatBuffer(debugLineVertices)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[VBONames.DEBUG_VERTICES])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            debugLineVertexBuffer.capacity().toLong() * java.lang.Float.BYTES,
            debugLineVertexBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        //Push debug line vertex data
        val debugLineColors = debugLines.flatMap { listOf(it.colorData1.red, it.colorData1.green, it.colorData1.blue, it.colorData2.red, it.colorData2.green, it.colorData2.blue)}.toList().toFloatArray()
        val debugLineColorBuffer = GLBuffers.newDirectFloatBuffer(debugLineColors)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[VBONames.DEBUG_COLORS])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            debugLineColorBuffer.capacity().toLong() * java.lang.Float.BYTES,
            debugLineColorBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
    }

    /**
     * Set the vertex attributes for each VBO.
     * Vertex attributes tell openGL how a particular VBO's data is divided
     */
    private fun initializeVertexAttributes(gl: GL3) {
        gl.glGenVertexArrays(1, VAOs) // Create VAO
        gl.glBindVertexArray(VAOs[0])

        for (attribute in GeneralAttributes.entries){
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

        for (attribute in InstancedAttributes.entries) {
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

    private fun getModelVertices() : FloatArray{
        val verticeList: MutableList<Float> = ArrayList()

        var marker = 0
        for (value in loadedModels) {
            for (vertexDatum in value.vertexData) {
                verticeList.add(vertexDatum)
            }
            modelDataMap.getValue(value).verticeIndex = marker
            marker += value.points
        }

        val verticeArray = FloatArray(verticeList.size)
        for (i in verticeList.indices) {
            verticeArray[i] = verticeList[i]
        }

        return verticeArray
    }

    private fun initializePrograms(gl: GL3) {

        //TODO Figure out if uniforms can be shared across programs??
        backgroundProgram = BackgroundProgram(gl, "", "Game_Background_Custom_Stars")
        checkError(gl, "initProgram : backGroundProgram")

        entityProgram = EntityProgram(gl, "", "Game_Entity", "Game_Entity")
        checkError(gl, "initProgram : entityProgram")

        uiProgram = UIProgram(gl, "", "Game_UI", "Game_UI")
        checkError(gl, "initProgram : uiProgram")

        debugProgram = DebugLineProgram(gl, "", "Game_Debug", "Game_Debug")
        checkError(gl, "initProgram : debugProgram")

    }

    private fun calculateViewMat() : FloatArray {
        val scale = FloatUtil.makeScale(FloatArray(16), true, 0.06f * cameraScale, 0.06f * cameraScale, 0.03f) //FIXME There's something weird about this - try increasing sz to above 0.06
        val translate = FloatUtil.makeTranslation(FloatArray(16), true, -cameraPos.getX().toFloat(), -cameraPos.getY().toFloat(), 0f)
//        val rotate = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0.0f, 0.0f , 0.0f)
//        return FloatUtil.multMatrix(FloatUtil.multMatrix(scale, rotate), translate)
        return FloatUtil.multMatrix(scale, translate)
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3
        synchronized(modelDataMap) {
            populateDynamicVBOs(gl)
            // view matrix
            val view = FloatArray(16)
            FloatUtil.makeIdentity(view)

            val cameraViewMatrix = calculateViewMat()
            for (i in 0..15) {
                matBuffer.put(i, cameraViewMatrix[i])
            }

            gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, clearColor.put(0, 0f).put(1, .33f).put(2, 0.66f).put(3, 1f))
            gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, clearDepth.put(0, 1f))

            gl.glBindVertexArray(VAOs[0])

            gl.glUseProgram(backgroundProgram!!.name)
            gl.glUniformMatrix4fv(backgroundProgram!!.cameraViewMatrix, 1, false, matBuffer)
            gl.glUniform1f(backgroundProgram!!.time, 0.0f)
            gl.glUniform2f(backgroundProgram!!.velocity, 0.0f, 0.0f)

            gl.glDrawArrays(
                Model.BACKPLATE.drawMode,
                modelDataMap.getValue(Model.BACKPLATE).verticeIndex,
                Model.BACKPLATE.points
            )

            gl.glUseProgram(0)
            gl.glUseProgram(entityProgram!!.name)
            gl.glUniformMatrix4fv(entityProgram!!.cameraViewMatrix, 1, false, matBuffer)
            gl.glUniform2f(backgroundProgram!!.velocity, cameraVelocity.getX().toFloat(), cameraVelocity.getY().toFloat())
            gl.glUniform1f(entityProgram!!.time, time)

            for ((model, data) in modelDataMap) {
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

            gl.glUseProgram(0)
            gl.glUseProgram(debugProgram!!.name)

            gl.glUniformMatrix4fv(entityProgram!!.cameraViewMatrix, 1, false, matBuffer)
            gl.glUniform1f(entityProgram!!.time, time)

            gl.glDrawArrays(GL.GL_LINES, 0, debugLines.size*2)
        }

        gl.glUseProgram(0)
        gl.glBindVertexArray(0)

        checkError(gl, "display")

        time += 1f
    }

    private fun transformScreenPosToGamePos(screenPos : Vector2) : Vector2 {
        val adjustedScreenPos =
            Vector2((screenPos.getX() / width.toDouble()) * 2 - 1, -(screenPos.getY() / height.toDouble()) * 2 + 1)
        val cameraViewMatrix4x4Flattened = FloatUtil.invertMatrix(calculateViewMat(), FloatArray(16))
        val vec4 = FloatUtil.multMatrixVec(cameraViewMatrix4x4Flattened, floatArrayOf(adjustedScreenPos.getX().toFloat(), adjustedScreenPos.getY().toFloat(), 0.0f, 1.0f),
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
     * TODO Alpha is currently unused
     */
    //TODO generalize vector math so it can be reused on things like colors?
    data class ColorData(val red: Float, val green: Float, val blue: Float, val alpha: Float)
    data class MetaData(val health: Float ) //TODO this could vary across entities - Maybe make this... a builder?

    data class IntermediaryRenderable<R : ReferenceFrame>(val model: Model, var pose: Pose<R>, val scale : Double, val colorData: ColorData, val metaData: MetaData){}

    data class Renderable(val model: Model, val coordinates: Vector2, val orientation: Double, val zHeight: Double, val scale : Double, val colorData: ColorData, val metaData: MetaData)


    //TODO Clean this up... DO we need separate VBONames and Attributes classes? Why is this not an enum? Should it start at zero?
    private interface VBONames {
        companion object {
            const val MODEL_VERTICES: Int = 1
            // TODO Could/Should these instanced VBOs be interleaved?
            const val INSTANCED_POSITIONS: Int = 2
            const val INSTANCED_ROTATIONS: Int = 3
            const val INSTANCED_SCALES: Int = 4
            const val INSTANCED_COLORS: Int = 5
            const val INSTANCED_HEALTHS: Int = 6
            const val DEBUG_VERTICES: Int = 7
            const val DEBUG_COLORS: Int = 8
            const val MAX: Int = 9
        }
    }

    enum class GeneralAttributes(val index: Int, val size: Int, val VBOBuffer: Int){
        POSITION(0, 3, VBONames.MODEL_VERTICES),
        DEBUG_POSITION(6, 3, VBONames.DEBUG_VERTICES),
        DEBUG_COLOR(7, 3, VBONames.DEBUG_COLORS),
    }

    enum class InstancedAttributes(val index: Int, val size: Int, val dataExtractor: (Renderable) -> List<Float>, val VBOBuffer: Int){
        POSITION(1, 3, {listOf(it.coordinates.getX().toFloat(), it.coordinates.getY().toFloat(), it.zHeight.toFloat())},
            VBONames.INSTANCED_POSITIONS
        ),
        ROTATION(2, 1, {listOf(it.orientation.toFloat())}, VBONames.INSTANCED_ROTATIONS),
        SCALE(3, 1, {listOf(it.scale.toFloat())}, VBONames.INSTANCED_SCALES),
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

}