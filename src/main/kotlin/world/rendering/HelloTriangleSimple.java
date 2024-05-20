package world.rendering;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL4.GL_MAP_COHERENT_BIT;
import static com.jogamp.opengl.GL4.GL_MAP_PERSISTENT_BIT;

/**
 * Created by GBarbieri on 16.03.2017.
 */
public class HelloTriangleSimple implements GLEventListener, KeyListener {

    private static GLWindow window;
    private static Animator animator;

    public static void main(String[] args) {
        new HelloTriangleSimple().setup();
    }

    private float[] vertexData = {
            -1, -1, 1, 0, 0,
            +0, +2, 0, 0, 1,
            +1, -1, 0, 1, 0};

    private short[] elementData = {2, 1, 0};

    private interface BufferIndices {

        int VERTEX = 0;
        int ELEMENT = 1;
        int GLOBAL_MATRICES = 2;
        int MODEL_MATRIX = 3;
        int NUM_BUFFERS = 4;
    }

    private IntBuffer bufferIndices = GLBuffers.newDirectIntBuffer(BufferIndices.NUM_BUFFERS);
    private IntBuffer vertexArray = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private ByteBuffer globalMatricesPointer, modelMatrixPointer;
    // https://jogamp.org/bugzilla/show_bug.cgi?id=1287
    private boolean bug1287 = true;

    private Program program;

    private long start;


    private void setup() {

        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setTitle("Hello Triangle (enhanced)");
        window.setSize(1024, 768);

        window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
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

        GL4 gl = drawable.getGL().getGL4();

        initDebug(gl);

        initBuffers(gl);

        initVertexArray(gl);

        program = new Program(gl, "", "hello-triangle", "hello-triangle");

        gl.glEnable(GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }

    private void initDebug(GL4 gl) {

        window.getContext().addGLDebugListener(new GLDebugListener() {
            @Override
            public void messageSent(GLDebugMessage event) {
                System.out.println(event);
            }
        });

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DONT_CARE,
                0,
                null,
                false);

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_HIGH,
                0,
                null,
                true);

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_MEDIUM,
                0,
                null,
                true);
    }

    private void initBuffers(GL4 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl.glCreateBuffers(BufferIndices.NUM_BUFFERS, bufferIndices);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferIndices.get(BufferIndices.VERTEX));
        gl.glBufferStorage(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, 0);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferIndices.get(BufferIndices.ELEMENT));
        gl.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.capacity() * Short.BYTES, elementBuffer, 0);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);


        IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);
        gl.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);
        int globalBlockSize = Math.max(16 * 4 * 2, uniformBufferOffset.get(0));
        int modelBlockSize = Math.max(16 * 4, uniformBufferOffset.get(0));

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferIndices.get(BufferIndices.GLOBAL_MATRICES));
        gl.glBufferStorage(GL_UNIFORM_BUFFER, globalBlockSize, null, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl.glBindBuffer(GL_UNIFORM_BUFFER, bufferIndices.get(BufferIndices.MODEL_MATRIX));
        gl.glBufferStorage(GL_UNIFORM_BUFFER, modelBlockSize, null, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT);
        gl.glBindBuffer(GL_UNIFORM_BUFFER, 0);


        // map the transform buffers and keep them mapped
        globalMatricesPointer = gl.glMapNamedBufferRange(
                bufferIndices.get(BufferIndices.GLOBAL_MATRICES),
                0,
                16 * 4 * 2,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT); // flags

        modelMatrixPointer = gl.glMapNamedBufferRange(
                bufferIndices.get(BufferIndices.MODEL_MATRIX),
                0,
                16 * 4,
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
    }

    private void initVertexArray(GL4 gl) {

        gl.glCreateVertexArrays(1, vertexArray);

        gl.glVertexArrayAttribBinding(vertexArray.get(0), Semantic.Attr.POSITION, Semantic.Stream.A);
        gl.glVertexArrayAttribBinding(vertexArray.get(0), Semantic.Attr.COLOR, Semantic.Stream.A);

        gl.glVertexArrayAttribFormat(vertexArray.get(0), Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0);
        gl.glVertexArrayAttribFormat(vertexArray.get(0), Semantic.Attr.COLOR, 3, GL_FLOAT, false, 2 * 4);

        gl.glEnableVertexArrayAttrib(vertexArray.get(0), Semantic.Attr.POSITION);
        gl.glEnableVertexArrayAttrib(vertexArray.get(0), Semantic.Attr.COLOR);

        gl.glVertexArrayElementBuffer(vertexArray.get(0), bufferIndices.get(BufferIndices.ELEMENT));
        gl.glVertexArrayVertexBuffer(vertexArray.get(0), Semantic.Stream.A, bufferIndices.get(BufferIndices.VERTEX), 0, (2 + 3) * 4);
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL4 gl = drawable.getGL().getGL4();

        // view matrix
        {
            float[] view = FloatUtil.makeIdentity(new float[16]);
            for (int i = 0; i < 16; i++)
                globalMatricesPointer.putFloat(16 * 4 + i * 4, view[i]);
        }


        gl.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1f).put(1, .5f).put(2, 0f).put(3, 1f));
        gl.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1f));

        // model matrix
        {
            float[] scale = FloatUtil.makeScale(new float[16], true, 0.5f, 0.5f, 0.5f);
            float[] rotateZ = FloatUtil.makeRotationAxis(new float[16], 0, 0, 0f, 0f, 1f, new float[3]);
            float[] model = FloatUtil.multMatrix(scale, rotateZ);
            modelMatrixPointer.asFloatBuffer().put(model);
        }

        gl.glUseProgram(program.name);
        gl.glBindVertexArray(vertexArray.get(0));

        gl.glBindBufferBase(
                GL_UNIFORM_BUFFER,
                Semantic.Uniform.TRANSFORM0,
                bufferIndices.get(BufferIndices.GLOBAL_MATRICES));

        gl.glBindBufferBase(
                GL_UNIFORM_BUFFER,
                Semantic.Uniform.TRANSFORM1,
                bufferIndices.get(BufferIndices.MODEL_MATRIX));

        gl.glDrawElements(
                GL_TRIANGLES,
                elementData.length,
                GL_UNSIGNED_SHORT,
                0);

        gl.glUseProgram(0);
        gl.glBindVertexArray(0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL4 gl = drawable.getGL().getGL4();

        // ortho matrix
        float[] ortho = FloatUtil.makeOrtho(new float[16], 0, false, -1f, 1f, -1f, 1f, 1f, -1f);
        globalMatricesPointer.asFloatBuffer().put(ortho);

        gl.glViewport(x, y, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL4 gl = drawable.getGL().getGL4();

        gl.glUnmapNamedBuffer(bufferIndices.get(BufferIndices.GLOBAL_MATRICES));
        gl.glUnmapNamedBuffer(bufferIndices.get(BufferIndices.MODEL_MATRIX));

        gl.glDeleteProgram(program.name);
        gl.glDeleteVertexArrays(1, vertexArray);
        gl.glDeleteBuffers(BufferIndices.NUM_BUFFERS, bufferIndices);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            new Thread(() -> window.destroy()).start();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private class Program {

        public int name = 0;

        public Program(GL4 gl, String root, String vertex, String fragment) {

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
        }
    }

    private class GlDebugOutput implements GLDebugListener {

        private int source = 0;
        private int type = 0;
        private int id = 0;
        private int severity = 0;
        private int length = 0;
        private String message = null;
        private boolean received = false;

        public GlDebugOutput() {
        }

        public GlDebugOutput(int source, int type, int severity) {
            this.source = source;
            this.type = type;
            this.severity = severity;
            this.message = null;
            this.id = -1;
        }

        public GlDebugOutput(String message, int id) {
            this.source = -1;
            this.type = -1;
            this.severity = -1;
            this.message = message;
            this.id = id;
        }

        @Override
        public void messageSent(GLDebugMessage event) {

            if (event.getDbgSeverity() == GL_DEBUG_SEVERITY_LOW || event.getDbgSeverity() == GL_DEBUG_SEVERITY_NOTIFICATION)
                System.out.println("GlDebugOutput.messageSent(): " + event);
            else
                System.err.println("GlDebugOutput.messageSent(): " + event);

            if (null != message && message == event.getDbgMsg() && id == event.getDbgId())
                received = true;
            else if (0 <= source && source == event.getDbgSource() && type == event.getDbgType() && severity == event.getDbgSeverity())
                received = true;
        }
    }
}