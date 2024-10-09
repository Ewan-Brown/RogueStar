import com.jogamp.opengl.*
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.GLBuffers
import org.dyn4j.geometry.Vector2
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.math.cos
import kotlin.math.exp
import kotlin.time.times

class Graphics(val loadedModels: List<Model>) : GraphicsBase() {
    private val VBOs: IntBuffer = GLBuffers.newDirectIntBuffer(Buffer.MAX)
    private val VAOs: IntBuffer = GLBuffers.newDirectIntBuffer(1)

    private val clearColor: FloatBuffer = GLBuffers.newDirectFloatBuffer(4)
    private val clearDepth: FloatBuffer = GLBuffers.newDirectFloatBuffer(1)

    private val matBuffer: FloatBuffer = GLBuffers.newDirectFloatBuffer(16)

    private val modelData = mutableMapOf<Model, ModelData>()

    var cameraPos: Vector2 = Vector2()
    var cameraVelocity: Vector2 = Vector2()
    var cameraScale: Float = 1.0f

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

    data class CameraDetails(val targetPosition: Vector2, val targetScale: Double, val targetRotation: Double)
    fun updateDrawables(data: Map<Model, List<Pair<Transformation, GraphicalData>>>, cameraDetails: CameraDetails) {
        synchronized(modelData) {
            //Update camera
            val diff = cameraPos.to(cameraDetails.targetPosition)
            cameraVelocity = diff.multiply(0.3)
            cameraScale = (cameraDetails.targetScale * exp(-cameraVelocity.magnitude)).toFloat()
            cameraPos.add(cameraVelocity)
            //Update graphics buffers
            for (loadedModel in loadedModels) {
                modelData.getValue(loadedModel).instanceData = data.getValue(loadedModel)
            }
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

    private interface Buffer {
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

        gl.glGenBuffers(Buffer.MAX, VBOs) // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.VERTEX])
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
        run {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.VERTEX])
            val stride = 3 * java.lang.Float.BYTES
            var offset = 0

            gl.glEnableVertexAttribArray(POSITION_ATTRIB_INDICE)
            gl.glVertexAttribPointer(POSITION_ATTRIB_INDICE, 3, GL.GL_FLOAT, false, stride, offset.toLong())

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_POSITIONS])
            gl.glEnableVertexAttribArray(INSTANCE_POSITION_ATTRIB_INDICE)
            gl.glVertexAttribPointer(
                INSTANCE_POSITION_ATTRIB_INDICE,
                3,
                GL.GL_FLOAT,
                false,
                3 * java.lang.Float.BYTES,
                0
            )

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_ROTATIONS])
            gl.glEnableVertexAttribArray(INSTANCE_ROTATION_ATTRIB_INDICE)
            gl.glVertexAttribPointer(
                INSTANCE_ROTATION_ATTRIB_INDICE,
                1,
                GL.GL_FLOAT,
                false,
                1 * java.lang.Float.BYTES,
                0
            )

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_SCALES])
            gl.glEnableVertexAttribArray(INSTANCE_SCALE_ATTRIB_INCIDE)
            gl.glVertexAttribPointer(
                INSTANCE_SCALE_ATTRIB_INCIDE,
                1,
                GL.GL_FLOAT,
                false,
                1 * java.lang.Float.BYTES,
                0
            )

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_COLORS])
            gl.glEnableVertexAttribArray(INSTANCE_COLOR_ATTRIB_INDICE)
            gl.glVertexAttribPointer(
                INSTANCE_COLOR_ATTRIB_INDICE,
                3,
                GL.GL_FLOAT,
                false,
                3 * java.lang.Float.BYTES,
                0
            )

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_HEALTHS])
            gl.glEnableVertexAttribArray(INSTANCE_HEALTH_ATTRIB_INDICE)
            gl.glVertexAttribPointer(
                INSTANCE_HEALTH_ATTRIB_INDICE,
                1,
                GL.GL_FLOAT,
                false,
                1 * java.lang.Float.BYTES,
                0
            )

            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0)

            gl.glVertexAttribDivisor(INSTANCE_COLOR_ATTRIB_INDICE, 1)
            gl.glVertexAttribDivisor(INSTANCE_POSITION_ATTRIB_INDICE, 1)
            gl.glVertexAttribDivisor(INSTANCE_ROTATION_ATTRIB_INDICE, 1)
            gl.glVertexAttribDivisor(INSTANCE_SCALE_ATTRIB_INCIDE, 1)
            gl.glVertexAttribDivisor(INSTANCE_HEALTH_ATTRIB_INDICE, 1)
        }
        gl.glBindVertexArray(0)

        checkError(gl, "initVao")
    }

    private fun updateInstanceData(gl: GL3) {
        val modelCount =
            modelData.values.stream().mapToInt { obj: ModelData -> obj.instanceCount }
                .sum()

        //TODO Clean this up.
        //TODO Maybe see if we can automate the insertion of new fields here this is getting silly
        var indexCounter = 0
        var instancePositionDataIndex = 0
        var instanceColorDataIndex = 0
        var instanceRotationDataIndex = 0
        var instanceScaleDataIndex = 0
        var instancedHealthDataIndex = 0
        val instancePositionData = FloatArray(modelCount * 4)
        val instanceColorData = FloatArray(modelCount * 3)
        val instanceRotationData = FloatArray(modelCount * 1)
        val instanceScaleData = FloatArray(modelCount * 1)
        val instanceHealthData = FloatArray(modelCount * 1)
        for (data in modelData.values) {
            for ((transform, graphicalData) in data.instanceData) {
                val x = transform.position.x
                val y = transform.position.y
                val z = graphicalData.z
                val angle = transform.rotation.toRadians().toFloat()
                val r = graphicalData.red
                val g = graphicalData.green
                val b = graphicalData.blue
                val scale = transform.scale
                val health = graphicalData.health
                instancePositionData[instancePositionDataIndex++] = x.toFloat()
                instancePositionData[instancePositionDataIndex++] = y.toFloat()
                instancePositionData[instancePositionDataIndex++] = z
                instanceColorData[instanceColorDataIndex++] = r
                instanceColorData[instanceColorDataIndex++] = g
                instanceColorData[instanceColorDataIndex++] = b
                instanceRotationData[instanceRotationDataIndex++] = angle
                instanceScaleData[instanceScaleDataIndex++] = scale.toFloat()
                instanceHealthData[instancedHealthDataIndex++] = health
            }
            data.instanceIndex = indexCounter
            indexCounter += data.instanceCount
        }

        //TODO We might be able to replace some glBufferData with glBufferSubData (avoiding unnecessary re-allocation)
        val positionBuffer = GLBuffers.newDirectFloatBuffer(instancePositionData)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_POSITIONS])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            positionBuffer.capacity().toLong() * java.lang.Float.BYTES,
            positionBuffer,
            GL.GL_DYNAMIC_DRAW
        )

        val colorBuffer = GLBuffers.newDirectFloatBuffer(instanceColorData)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_COLORS])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            colorBuffer.capacity().toLong() * java.lang.Float.BYTES,
            colorBuffer,
            GL.GL_DYNAMIC_DRAW
        )

        val rotationBuffer = GLBuffers.newDirectFloatBuffer(instanceRotationData)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_ROTATIONS])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            rotationBuffer.capacity().toLong() * java.lang.Float.BYTES,
            rotationBuffer,
            GL.GL_DYNAMIC_DRAW
        )

        val scaleBuffer = GLBuffers.newDirectFloatBuffer(instanceScaleData)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_SCALES])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            scaleBuffer.capacity().toLong() * java.lang.Float.BYTES,
            scaleBuffer,
            GL.GL_DYNAMIC_DRAW
        )

        val healthBuffer = GLBuffers.newDirectFloatBuffer(instanceHealthData)
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBOs[Buffer.INSTANCED_HEALTHS])
        gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            healthBuffer.capacity().toLong() * java.lang.Float.BYTES,
            healthBuffer,
            GL.GL_DYNAMIC_DRAW
        )

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

            val scale = FloatUtil.makeScale(FloatArray(16), true, 0.06f * cameraScale, 0.06f * cameraScale, 0.03f) //FIXME There's something weird about this - try increasing sz to above 0.06
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
        gl.glDeleteBuffers(Buffer.MAX, VBOs)
        checkError(gl, "dispose() : deleting resources")
    }

    inner class BackgroundProgram(gl: GL3,root: String,fragment: String) : WorldProgram(gl, root,"Game_Background", fragment){}

    inner class EntityProgram(gl: GL3,root: String, vertex: String,fragment: String) : WorldProgram(gl,root,vertex, fragment){}

    open inner class WorldProgram(gl: GL3,root: String, vertex: String,fragment: String) : Program(gl,root,vertex,fragment){
        val viewMat: Int = registerField(gl, "view")
    }

    inner class UIProgram(gl: GL3,root: String, vertex: String,fragment: String) : Program(gl,root,vertex,fragment){}

    companion object {
        var POSITION_ATTRIB_INDICE: Int = 0
        var INSTANCE_POSITION_ATTRIB_INDICE: Int = 1
        var INSTANCE_ROTATION_ATTRIB_INDICE: Int = 2
        var INSTANCE_SCALE_ATTRIB_INCIDE: Int = 3
        var INSTANCE_COLOR_ATTRIB_INDICE: Int = 4
        var INSTANCE_HEALTH_ATTRIB_INDICE: Int = 5;
    }
}