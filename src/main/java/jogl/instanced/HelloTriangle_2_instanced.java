package jogl.instanced;

import com.jogamp.opengl.*;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import jogl.HelloTriangle_Base;
import world.rendering.Semantic;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL3.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL3.GL_FLOAT;

/**
 *  Render X instanced of one model in one draw call, passing in X vec2(x,y) to shaders
 */
public class HelloTriangle_2_instanced extends HelloTriangle_Base {

    public static void main(String[] args) {
        new HelloTriangle_2_instanced().setup();
    }

    private float[] vertexData1 = {
            -1, -1, 1, 0, 0,
            +0, +2, 0, 0, 1,
            +1, -1, 0, 1, 0
    };

    static final int TRIANGLE_COUNT = 10000;

    private short[] elementData1 = {0, 1, 2};

    private interface Buffer {

        int VERTEX1 = 0;
        int ELEMENT1 = 1;
        int INSTANCED_STUFF = 2;
        int GLOBAL_MATRICES = 3;
        int MAX = 4;
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
        float[] instanceData = new float[TRIANGLE_COUNT * 3];

        for (int i = 0; i < TRIANGLE_COUNT; i++) {
            float x = (float)(Math.random() - 0.5) * 18.0f;
            float y = (float)(Math.random() - 0.5) * 18.0f;
            float angle = (float)(Math.random() * Math.PI *2);
            instanceData[i*3]= x;
            instanceData[i*3+1] = y;
            instanceData[i*3+2] = angle;
        }

        FloatBuffer vertexBuffer1 = GLBuffers.newDirectFloatBuffer(vertexData1);
        ShortBuffer elementBuffer1 = GLBuffers.newDirectShortBuffer(elementData1);
        FloatBuffer instanceBuffer = GLBuffers.newDirectFloatBuffer(instanceData);

        gl.glGenBuffers(Buffer.MAX, VBOs); // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.VERTEX1));
        gl.glBufferData(GL_ARRAY_BUFFER, (long) vertexBuffer1.capacity() * Float.BYTES, vertexBuffer1, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBOs.get(Buffer.ELEMENT1));
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) elementBuffer1.capacity() * Short.BYTES, elementBuffer1, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.INSTANCED_STUFF));
        gl.glBufferData(GL_ARRAY_BUFFER, (long) instanceBuffer.capacity() * Float.BYTES, instanceBuffer, GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, VBOs.get(Buffer.GLOBAL_MATRICES));
        gl.glBufferData(GL_UNIFORM_BUFFER, 16 * Float.BYTES * 2, null, GL_STREAM_DRAW);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

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
            gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.VERTEX1));
            int stride = (2 + 3) * Float.BYTES;
            int offset = 0;

            gl.glEnableVertexAttribArray(POSITION_ATTRIB_INDICE);
            gl.glVertexAttribPointer(POSITION_ATTRIB_INDICE, 2, GL_FLOAT, false, stride, offset);

            offset = 2 * Float.BYTES;
            gl.glEnableVertexAttribArray(COLOR_ATTRIB_INDICE);
            gl.glVertexAttribPointer(COLOR_ATTRIB_INDICE, 3, GL_FLOAT, false, stride, offset);

            gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.INSTANCED_STUFF));
            gl.glEnableVertexAttribArray(INSTANCE_POSITION_ATTRIB_INDICE);
            gl.glVertexAttribPointer(INSTANCE_POSITION_ATTRIB_INDICE, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);

            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
            gl.glVertexAttribDivisor(INSTANCE_POSITION_ATTRIB_INDICE, 1);

            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, VBOs.get(Buffer.ELEMENT1));
        }
        gl.glBindVertexArray(0);

        checkError(gl, "initVao");
    }

    private void initProgram(GL3 gl) {

        program = new Program(gl, getClass(), "", "hello-triangle_2", "hello-triangle_2");

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

        gl.glDrawArraysInstanced(GL_TRIANGLES, 0,  elementData1.length, TRIANGLE_COUNT);
        gl.glUseProgram(0);
        gl.glBindVertexArray(0);

        checkError(gl, "display");
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