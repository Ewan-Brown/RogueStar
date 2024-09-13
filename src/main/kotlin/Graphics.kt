import com.jogamp.opengl.*
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.GLBuffers
import org.dyn4j.geometry.Vector2
import java.nio.FloatBuffer
import java.nio.IntBuffer

//TODO loadedModels shouldn't need to used like this, there should be a .loadModels() function that lets you pass in new ones/discard old?
class Graphics(val loadedModels: List<Model>) : GraphicsBase() {
    private val VBOFieldsCount = VBOFields.entries.size;
    private val VBOs: IntBuffer = GLBuffers.newDirectIntBuffer(VBOFieldsCount)
    private val VAOs: IntBuffer = GLBuffers.newDirectIntBuffer(1)

    private val clearColor: FloatBuffer = GLBuffers.newDirectFloatBuffer(4)
    private val clearDepth: FloatBuffer = GLBuffers.newDirectFloatBuffer(1)

    private val matBuffer: FloatBuffer = GLBuffers.newDirectFloatBuffer(16)

    private val modelData = mutableMapOf<Model, ModelData>()

    //The position to shift the background by
    var cameraPos: Vector2 = Vector2()
    var cameraTargetPos: Vector2 = Vector2()

    var entityProgram: EntityProgram? = null
    var backgroundProgram: BackgroundProgram? = null
    var uiProgram: UIProgram? = null

    //the time for the background
    var time: Float = 0f

    private inner class ModelData {
        var verticeIndex: Int = 0
        var instanceIndex: Int = 0
        var instanceData: List<Pair<Transformation, GraphicalData>> = ArrayList()
        val instanceCount: Int
            get() = instanceData.size
    }

    fun updateDrawables(data: Map<Model, List<Pair<Transformation, GraphicalData>>>, cameraTarget: Vector2) {
        synchronized(modelData) {
            for (loadedModel in loadedModels) {
                modelData.getValue(loadedModel).instanceData = data.getValue(loadedModel)
            }
            this.cameraTargetPos = cameraTarget
        }
    }

    class Model internal constructor(val vertexData: FloatArray, dMode: Int) {
        val points: Int = vertexData.size / 3 //Change if vertex data size changes!
        val drawMode: Int = dMode
        val asVectorData: List<Vector2> = List(points){
            Vector2(vertexData[it * 3].toDouble(), vertexData[it * 3 + 1].toDouble())
        }

        companion object {
            var TRIANGLE: Model = Model(
                floatArrayOf(
                    -1.0f, +1.0f, +0.1f,
                    -1.0f, -1.0f, +0.1f,
                    +1.0f, +0.0f, +0.1f
                ), GL.GL_TRIANGLES
            )
            var SQUARE1: Model = Model(
                floatArrayOf(
                    -0.5f, -0.5f, +0.1f,
                    +0.5f, -0.5f, +0.1f,
                    +0.5f, +0.5f, +0.1f,
                    -0.5f, +0.5f, +0.1f
                ), GL.GL_TRIANGLE_FAN
            )
            var BACKPLATE: Model = Model(
                floatArrayOf(
                    -1f, -1f, +0.4f,
                    -1f, +1f, +0.4f,
                    +1f, +1f, +0.4f,
                    +1f, -1f, +0.4f
                ), GL.GL_TRIANGLE_FAN
            )
        }
    }

    /**
     * Represents each of the VBOs that we use. Note that if 'instanced' is true, dataExtractor is expected to be on-null and vali
     * index value is shared for VBO index as well as the respective attribute index
     */
    enum class VBOFields(val index: Int, val size: Int, val instanced: Boolean, val dataExtractor: ((Pair<Transformation, GraphicalData>) -> List<Float>)?){
        VERTEX  (0,3,false, null),
        POSITION(1,3,true, { listOf(it.first.position.x.toFloat(), it.first.position.y.toFloat(), it.second.z.toFloat())}),
        ROTATION(2,1,true, {listOf(it.first.rotation.toRadians().toFloat())}),
        SCALE   (3,1,true, {listOf(it.first.scale.toFloat())}),
        COLOR   (4,3,true, { listOf(it.second.red, it.second.green, it.second.blue)}),
        HEALTH  (5,1,true, {listOf(it.second.health.toFloat())})
    }


    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

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

        gl.glGenBuffers(VBOFieldsCount, VBOs) // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[VBOFields.VERTEX.index])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertexBuffer.capacity().toLong() * java.lang.Float.BYTES,
            vertexBuffer,
            GL.GL_STATIC_DRAW
        )
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

        gl.glBindBuffer(GL2ES3.GL_UNIFORM_BUFFER, 0)

        checkError(gl, "initBuffers")
    }

    init {
        for (preloadedModel in loadedModels) {
            modelData[preloadedModel] = ModelData()
        }
    }

    private fun initVAOs(gl: GL3) {
        gl.glGenVertexArrays(1, VAOs) // Create VAO
        gl.glBindVertexArray(VAOs[0])

        for (entry in VBOFields.entries) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[entry.index])

            gl.glEnableVertexAttribArray(entry.index)
            gl.glVertexAttribPointer(
                entry.index,
                entry.size,
                GL.GL_FLOAT,
                false,
                entry.size * java.lang.Float.BYTES,
                0
            )

        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)
        for(entry in VBOFields.entries.filter { it.instanced }){
            gl.glVertexAttribDivisor(entry.index, 1)
        }
        gl.glBindVertexArray(0)

        checkError(gl, "initVao")
    }


    private fun updateInstanceData(gl: GL3) {
        cameraPos = cameraTargetPos

        val modelCount =
            modelData.values.stream().mapToInt { obj: ModelData -> obj.instanceCount }
                .sum()

        //TODO Clean this up.
        //TODO Maybe see if we can automate the insertion of new fields here this is getting silly

        val floatArrayMap = mutableMapOf<VBOFields, FloatArray>()
        for (entry in VBOFields.entries.filter { it.instanced }) {
            floatArrayMap[entry] = FloatArray(entry.size * modelCount)
        }

        var indexMarker = 0
        var indexCounter = 0
        for (modelGroup in modelData.values){
            for (model in modelGroup.instanceData){
                for(VBO in VBOFields.entries.filter{ it.instanced }){
                    val extractedFloats = VBO.dataExtractor!!(model) //TODO Can improve safety here? We just have to hope that we added a data extractor to every instanced field...
                    val targetFloatArray = floatArrayMap[VBO]
                    for ((floatIndex, extractedFloat) in extractedFloats.withIndex()) {
                        val index = indexMarker * VBO.size + floatIndex
                        targetFloatArray!![index] = extractedFloat
                    }
                }
                indexMarker++
            }
            modelGroup.instanceIndex = indexCounter
            indexCounter += modelGroup.instanceCount
        }

        for (entry in floatArrayMap.filter { it -> it.key.instanced }) {
            val buffer = GLBuffers.newDirectFloatBuffer(entry.value)
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[entry.key.index])
            gl.glBufferData(GL.GL_ARRAY_BUFFER,
                buffer.capacity().toLong() * java.lang.Float.BYTES,
                buffer,
                GL.GL_DYNAMIC_DRAW)
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
        checkError(gl, "initProgram: uiProgram")

    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        synchronized(modelData) {
            updateInstanceData(gl)
            // view matrix
            val view = FloatArray(16)
            FloatUtil.makeIdentity(view)

            val scale = FloatUtil.makeScale(FloatArray(16), true, 0.06f, 0.06f, 0.03f) //FIXME There's something weird about this - try increasing sz to above 0.06
            val translate =
                FloatUtil.makeTranslation(FloatArray(16), 0, true, -cameraPos.x.toFloat(), -cameraPos.y.toFloat(), 0f)
            val rotate = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0.0f, 0.0f , 0.0f)
            val viewMat = FloatUtil.multMatrix(FloatUtil.multMatrix(scale, rotate), translate)
            for (i in 0..15) {
                matBuffer.put(i, viewMat[i])
            }

            gl.glClearBufferfv(GL2ES3.GL_COLOR, 0, clearColor.put(0, 0f).put(1, .33f).put(2, 0.66f).put(3, 1f))
            gl.glClearBufferfv(GL2ES3.GL_DEPTH, 0, clearDepth.put(0, 1f))

            gl.glBindVertexArray(VAOs[0])

            gl.glUseProgram(backgroundProgram!!.name)
            gl.glUniform1f(backgroundProgram!!.time, time)
            gl.glUniformMatrix4fv(backgroundProgram!!.viewMat, 1, false, matBuffer)

            gl.glDrawArrays(
                Model.BACKPLATE.drawMode,
                modelData.getValue(Model.BACKPLATE).verticeIndex,
                Model.BACKPLATE.points
            )

            gl.glUseProgram(0)
            gl.glUseProgram(entityProgram!!.name)
            gl.glUniformMatrix4fv(entityProgram!!.viewMat, 1, false, matBuffer)
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

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        val gl = drawable.gl.gL3

        gl.glViewport(x, y, width, height)
    }

    override fun dispose(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL3

        gl.glDeleteProgram(entityProgram!!.name)
        gl.glDeleteProgram(backgroundProgram!!.name)
        gl.glDeleteVertexArrays(1, VAOs)
        gl.glDeleteBuffers(VBOFieldsCount, VBOs)
        checkError(gl, "dispose() : deleting resources")
    }

    inner class BackgroundProgram(gl: GL3,root: String,fragment: String) : WorldProgram(gl, root,"Game_Background", fragment){}

    inner class EntityProgram(gl: GL3,root: String, vertex: String,fragment: String) : WorldProgram(gl,root,vertex, fragment){}

    open inner class WorldProgram(gl: GL3,root: String, vertex: String,fragment: String) : Program(gl,root,vertex,fragment){
        val viewMat: Int = registerField(gl, "view")
    }

    inner class UIProgram(gl: GL3,root: String, vertex: String,fragment: String) : Program(gl,root,vertex,fragment){}
}