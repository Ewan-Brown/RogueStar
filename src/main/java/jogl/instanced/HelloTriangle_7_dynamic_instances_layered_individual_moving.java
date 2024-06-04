package jogl.instanced;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import jogl.HelloTriangle_Base;
import org.dyn4j.geometry.Vector2;
import jogl.Semantic;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL3.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL3.GL_FLOAT;

/**
 * Render X instances of two models in shared buffers, in 2 draw calls, passing in X vec2(x,y) to shaders
 *
 * Think of X spacesships, each with 1 turret on them that sits relative to the spaceship's main hull
 */
public class HelloTriangle_7_dynamic_instances_layered_individual_moving extends HelloTriangle_Base {

    public static void main(String[] args) {
        HelloTriangle_7_dynamic_instances_layered_individual_moving gui = new HelloTriangle_7_dynamic_instances_layered_individual_moving();
        gui.setup();
    }


    /*
              /\
             / _\__
       0 -> / |__\|
           /__^___\
              0
     */

    private float[] vertexData = {
            -1.0f, -1.0f, 1, 1, 0, 0,
            +0.0f, +2.0f, 1, 1, 0, 0,
            +1.0f, -1.0f, 1, 1, 0, 0,
            -0.0f, -0.0f, 0, 0, 1, 0,
            +0.0f, +1.0f, 0, 0, 1, 0,
            +1.0f, +1.0f, 0, 0, 1, 0,
            +1.0f, 0.0f, 0, 0, 1, 0
    };

    static final int TRIANGLE_COUNT = 100;

    private short[] elementData = {0, 1, 2, 3, 4, 5, 6};

    private interface Buffer {

        int VERTEX = 1;
        int ELEMENT = 2;
        int INSTANCED_STUFF = 3;
        int GLOBAL_MATRICES = 4;
        int MAX = 5;
    }

    private IntBuffer VBOs = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer VAOs = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16);

    @Override
    public void init(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        initVBOs(gl);

        initVAOs(gl);

        initProgram(gl);

        gl.glEnable(GL_DEPTH_TEST);

    }

    private void initVBOs(GL3 gl) {

        //Generate Instance data

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl.glGenBuffers(Buffer.MAX, VBOs); // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, (long) vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBOs.get(Buffer.ELEMENT));
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) elementBuffer.capacity() * Short.BYTES, elementBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, VBOs.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferData(GL_UNIFORM_BUFFER, 16 * Float.BYTES * 2, null, GL_STREAM_DRAW);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        updateInstanceData(gl);

        gl.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.GLOBAL_MATRICES, VBOs.get(Buffer.GLOBAL_MATRICES));

        checkError(gl, "initBuffers");
    }

    static int POSITION_ATTRIB_INDICE = 0;
    static int COLOR_ATTRIB_INDICE = 1;
    static int INSTANCE_POSITION_ATTRIB_INDICE = 2;


    private void initVAOs(GL3 gl) {
        gl.glGenVertexArrays(1, VAOs); // Create VAO
        gl.glBindVertexArray(VAOs.get(0));
        {
            gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.VERTEX));
            int stride = (3 + 3) * Float.BYTES;
            int offset = 0;

            gl.glEnableVertexAttribArray(POSITION_ATTRIB_INDICE);
            gl.glVertexAttribPointer(POSITION_ATTRIB_INDICE, 3, GL_FLOAT, false, stride, offset);

            offset = 3 * Float.BYTES;
            gl.glEnableVertexAttribArray(COLOR_ATTRIB_INDICE);
            gl.glVertexAttribPointer(COLOR_ATTRIB_INDICE, 3, GL_FLOAT, false, stride, offset);

            gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.INSTANCED_STUFF));
            gl.glEnableVertexAttribArray(INSTANCE_POSITION_ATTRIB_INDICE);
            gl.glVertexAttribPointer(INSTANCE_POSITION_ATTRIB_INDICE, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl.glVertexAttribDivisor(INSTANCE_POSITION_ATTRIB_INDICE, 1);

            //TODO We can probably remove the element array buffer yeah? Others seem to not use it...
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBOs.get(Buffer.ELEMENT));
        }
        gl.glBindVertexArray(0);

        checkError(gl, "initVao");
    }

    Vector2[] positions = new Vector2[TRIANGLE_COUNT];
    float[] angles = new float[TRIANGLE_COUNT];

    {
        for (int i = 0; i < TRIANGLE_COUNT; i++) {
            float x = (float) (Math.random() - 0.5) * 18.0f;
            float y = (float) (Math.random() - 0.5) * 18.0f;
            float angle = (float) (Math.random() * Math.PI * 2);

            positions[i] = new Vector2(x, y);
            angles[i] = angle;
        }
    }

    private void updateInstanceData(GL3 gl){
        float[] instanceData = new float[TRIANGLE_COUNT * 3];

        for (int i = 0; i < TRIANGLE_COUNT; i++) {
            float currentAngle = angles[i];
            float x = (float)positions[i].x;
            float y = (float)positions[i].y;

            instanceData[i * 3] = x;
            instanceData[i * 3 + 1] = y;
            instanceData[i * 3 + 2] = currentAngle;

            positions[i].x += Math.cos(currentAngle + Math.PI/2)/10;
            positions[i].y += Math.sin(currentAngle + Math.PI/2)/10;
            angles[i] += (i % 2 == 0) ? -(0.3f/i) : (0.3f/i);
        }
        FloatBuffer instanceBuffer = GLBuffers.newDirectFloatBuffer(instanceData);
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.INSTANCED_STUFF));
        gl.glBufferData(GL_ARRAY_BUFFER, (long) instanceBuffer.capacity() * Float.BYTES, instanceBuffer, GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

    }


    private void initProgram(GL3 gl) {

        program = new Program(gl, getClass(), "", "hello-triangle_7", "hello-triangle_7");

        checkError(gl, "initProgram");
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        // view matrix
        {
            float[] view = new float[16];
            FloatUtil.makeIdentity(view);

            for (int i = 0; i < 16; i++) {
                matBuffer.put(i, view[i]);
            }
            gl.glBindBuffer(GL_UNIFORM_BUFFER, VBOs.get(Buffer.GLOBAL_MATRICES));
            gl.glBufferSubData(GL_UNIFORM_BUFFER, 16 * Float.BYTES, 16 * Float.BYTES, matBuffer);
            gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0f).put(1, .33f).put(2, 0.66f).put(3, 1f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f));

        gl.glUseProgram(program.name);
        gl.glBindVertexArray(VAOs.get(0));

        // model matrix
        {
            float[] scale = FloatUtil.makeScale(new float[16], true, 0.1f, 0.1f, 0.1f);
            float[] zRotation = FloatUtil.makeRotationEuler(new float[16], 0, 0, 0, 0.0f);
            float[] modelToWorldMat = FloatUtil.multMatrix(scale, zRotation);

            for (int i = 0; i < 16; i++) {
                matBuffer.put(i, modelToWorldMat[i]);
            }
            gl.glUniformMatrix4fv(program.modelToWorldMatUL, 1, false, matBuffer);
        }

        //CONTINUE - It looks like we can draw both, but the larger triangles are covering up the smaller ones.
        // Need to do two things:
        // - Ensure proper layering of things
        // - Ensure that the second set of models can be passed an angle relative to the larger one, or absolute. Not sure which is more fitting
//        gl.glDrawArraysInstanced(GL_TRIANGLES, 1,  3, TRIANGLE_COUNT);
//        gl.glEnable(GL_POINT);
//        gl.glPointSize(10f);
        gl.glDrawArraysInstanced(GL_TRIANGLES, 0,  3, TRIANGLE_COUNT);
        gl.glDrawArraysInstanced(GL_TRIANGLE_FAN, 3,  4, TRIANGLE_COUNT);
        gl.glUseProgram(0);
        gl.glBindVertexArray(0);

        checkError(gl, "display");

        updateInstanceData(gl);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL3 gl = drawable.getGL().getGL3();

        float[] ortho = new float[16];
        FloatUtil.makeOrtho(ortho, 0, false, -1, 1, -1, 1, 1, -1);
        for (int i = 0; i < 16; i++) {
            matBuffer.put(i, ortho[i]);
        }
        gl.glBindBuffer(GL_UNIFORM_BUFFER, VBOs.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferSubData(GL_UNIFORM_BUFFER, 0, 16 * Float.BYTES, matBuffer);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glViewport(x, y, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        gl.glDeleteProgram(program.name);
        gl.glDeleteVertexArrays(1, VAOs);
        gl.glDeleteBuffers(Buffer.MAX, VBOs);
    }
}