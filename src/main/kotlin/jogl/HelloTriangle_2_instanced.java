package jogl;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import world.rendering.Semantic;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL3.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL3.GL_FLOAT;
import static com.jogamp.opengl.GL3.GL_INVALID_ENUM;
import static com.jogamp.opengl.GL3.GL_INVALID_FRAMEBUFFER_OPERATION;
import static com.jogamp.opengl.GL3.GL_INVALID_OPERATION;
import static com.jogamp.opengl.GL3.GL_INVALID_VALUE;
import static com.jogamp.opengl.GL3.GL_NO_ERROR;
import static com.jogamp.opengl.GL3.GL_OUT_OF_MEMORY;

/**
 * @author gbarbieri
 */
public class HelloTriangle_2_instanced implements GLEventListener, KeyListener {

    private static GLWindow window;
    private static Animator animator;

    public static void main(String[] args) {
        new HelloTriangle_2_instanced().setup();
    }

    private float[] vertexData1 = {
            -1, -1, 1, 0, 0,
            +0, +2, 0, 0, 1,
            +1, -1, 0, 1, 0
    };

    static final int TRIANGLE_COUNT = 5;

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

    private Program program;

    private long start;

    private void setup() {

        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setTitle("Hello Triangle (simple)");
        window.setSize(1024, 768);

        window.setVisible(true);

        window.addGLEventListener(this);
        window.addKeyListener(this);

        animator = new Animator(window);
        animator.start();

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                animator.stop();
                System.exit(1);
            }
        });

    }

    @Override
    public void init(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        initVBOs(gl);

        initVAOs(gl);

        initProgram(gl);

        gl.glEnable(GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initVBOs(GL3 gl) {

        //Generate Instance data
        float[] instanceData = new float[TRIANGLE_COUNT * 2];

        for (int i = 0; i < TRIANGLE_COUNT; i++) {
            instanceData[i*2]= (float)Math.random() - 0.5f;
            instanceData[i*2+1] = (float)Math.random() - 0.5f;
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
        gl.glBufferData(GL_ARRAY_BUFFER, (long) instanceBuffer.capacity() * Short.BYTES, instanceBuffer, GL_STATIC_DRAW);
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
            {
                int stride = (2 + 3) * Float.BYTES;
                int offset = 0;

                gl.glEnableVertexAttribArray(POSITION_ATTRIB_INDICE);
                gl.glVertexAttribPointer(POSITION_ATTRIB_INDICE, 2, GL_FLOAT, false, stride, offset);

                offset = 2 * Float.BYTES;
                gl.glEnableVertexAttribArray(COLOR_ATTRIB_INDICE);
                gl.glVertexAttribPointer(COLOR_ATTRIB_INDICE, 3, GL_FLOAT, false, stride, offset);

                offset = 0;
                gl.glEnableVertexAttribArray(INSTANCE_POSITION_ATTRIB_INDICE);
                gl.glVertexAttribPointer(INSTANCE_POSITION_ATTRIB_INDICE, 2, GL_FLOAT, false, stride, offset);

            }
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
            gl.glVertexAttribDivisor(INSTANCE_POSITION_ATTRIB_INDICE, 1);

            //TODO We can probably remove the element array buffer yeah? Others seem to not use it...
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
            float[] zRotation = FloatUtil.makeRotationEuler(new float[16], 0, 0, 0, 0.5f);
            float[] modelToWorldMat = FloatUtil.multMatrix(scale, zRotation);

            for (int i = 0; i < 16; i++) {
                matBuffer.put(i, modelToWorldMat[i]);
            }
            gl.glUniformMatrix4fv(program.modelToWorldMatUL, 1, false, matBuffer);
        }

        gl.glDrawElements(GL_TRIANGLES, elementData1.length, GL_UNSIGNED_SHORT, 0);

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


    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            new Thread(() -> {
                window.destroy();
            }).start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private class Program {

        int name, modelToWorldMatUL;

        Program(GL3 gl, Class context, String root, String vertex, String fragment) {

            ShaderCode vertShader = ShaderCode.create(gl, GL_VERTEX_SHADER, this.getClass(), root, null, vertex,
                    "vert", null, true);
            ShaderCode fragShader = ShaderCode.create(gl, GL_FRAGMENT_SHADER, this.getClass(), root, null, fragment,
                    "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();

            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.init(gl);

            name = shaderProgram.program();

            shaderProgram.link(gl, System.err);


            modelToWorldMatUL = gl.glGetUniformLocation(name, "model");

            if (modelToWorldMatUL == -1) {
                System.err.println("uniform 'model' not found!");
            }


            int globalMatricesBI = gl.glGetUniformBlockIndex(name, "GlobalMatrices");

            if (globalMatricesBI == -1) {
                System.err.println("block index 'GlobalMatrices' not found!");
            }
            gl.glUniformBlockBinding(name, globalMatricesBI, Semantic.Uniform.GLOBAL_MATRICES);
        }
    }

    private void checkError(GL gl, String location) {

        int error = gl.glGetError();
        if (error != GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GL_INVALID_FRAMEBUFFER_OPERATION:
                    errorString = "GL_INVALID_FRAMEBUFFER_OPERATION";
                    break;
                case GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "UNKNOWN";
                    break;
            }
            throw new Error("OpenGL Error(" + errorString + "): " + location);
        }
    }
}