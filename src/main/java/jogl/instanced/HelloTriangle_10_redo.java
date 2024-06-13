package jogl.instanced;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import jogl.HelloTriangle_Base;
import jogl.Semantic;
import org.dyn4j.geometry.Vector2;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL3.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL3.GL_FLOAT;

/**
 * Render stuff things
 */
public class HelloTriangle_10_redo extends HelloTriangle_Base {

    private IntBuffer VBOs = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer VAOs = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16);

    public static void main(String[] args) {
        HelloTriangle_10_redo gui = new HelloTriangle_10_redo();
        gui.setup();
    }

    /*
              /\
             / _\__
       0 -> / |__\|
           /__^___\
              0
     */

    List<Entity> entities = new ArrayList<>();
    {
        entities.add(new Entity(Model.TRIANGLE, new Object()));
        entities.add(new Entity(Model.SQUARE1, new Object()));
    }

    private static class Entity{
        Model m;
        Object someData;
        Vector2 pos = new Vector2(Math.random() + 0.5, Math.random() + 0.5);
        float angle = (float)Math.random();

        public Entity(Model m, Object someData) {
            this.m = m;
            this.someData = someData;
        }
    }

    //Points to where the start of each model's vertex data is in the VERTEX buffer
    final HashMap<Model, Integer> verticeIndexes = new HashMap<>(); //Can probably be attached directly to a model object (wrap the Enum in an object to make mutable fields)
    //Points to where each grouping of model starts in the INSTANCE buffer
    // (InstanceData is sorted by model!)
    final HashMap<Model, Integer> instanceIndexes = new HashMap<>();

    final HashMap<Model, Integer> sortedEntityCounts = new HashMap<>();

    private enum Model {
        TRIANGLE(new float[]{
                -1.0f, -1.0f, 2, 1, 0, 0,
                +0.0f, +2.0f, 2, 1, 0, 0,
                +1.0f, -1.0f, 2, 1, 0, 0
        }, GL_TRIANGLES),
        SQUARE1(new float[]{
                -0.0f, -0.0f, 1, 0, 1, 0,
                +0.0f, +1.0f, 1, 0, 1, 0,
                +1.0f, +1.0f, 1, 0, 1, 0,
                +1.0f, 0.0f, 1, 0, 1, 0
        }, GL_TRIANGLE_FAN),
        SQUARE2(new float[]{
            -1.0f, -0.0f, 0, 1, 1, 0,
            -1.0f, +1.0f, 0, 1, 1, 0,
            +0.0f, +1.0f, 0, 1, 1, 0,
            +0.0f, 0.0f, 0, 1, 1, 0
        }, GL_TRIANGLE_FAN);
        final int points;
        final float[] vertexData;
        final int drawMode;

        Model(float[] vertexData, int dMode) {
            this.points = vertexData.length/6; //Change if vertex data size changes!
            this.vertexData = vertexData;
            this.drawMode = dMode;
        }
    };

    private interface Buffer {

        int VERTEX = 1;
        int INSTANCED_STUFF = 2;
        int GLOBAL_MATRICES = 3;
        int MAX = 4;
    }


    @Override
    public void init(GLAutoDrawable drawable) {

        GL3 gl = drawable.getGL().getGL3();

        initVBOs(gl);

        initVAOs(gl);

        initProgram(gl);

        gl.glEnable(GL_DEPTH_TEST);

    }

    private void initVBOs(GL3 gl) {

        for (Map.Entry<Model, Integer> modelIntegerEntry : verticeIndexes.entrySet()) {
            System.out.println(modelIntegerEntry.getKey().toString());
            System.out.println("\t" + modelIntegerEntry.getValue());
        }

        //Generate vertex data and store offsets for models
        List<Float> verticeList = new ArrayList<>();
        int marker = 0;
        for (Model value : Model.values()) {
            System.out.println("model loaded : " + value +", size : " + value.points);
            for (float vertexDatum : value.vertexData) {
                verticeList.add(vertexDatum);
            }
            verticeIndexes.put(value, marker);
            marker += value.points;
        }

        for (Map.Entry<Model, Integer> modelIntegerEntry : verticeIndexes.entrySet()) {
            System.out.println("model : " + modelIntegerEntry.getKey());
            System.out.println("\t" + modelIntegerEntry.getValue());
        }

        float[] verticeArray = new float[verticeList.size()];
        for (int i = 0; i < verticeList.size(); i++) {
            verticeArray[i] = verticeList.get(i);
        }

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(verticeArray);

        gl.glGenBuffers(Buffer.MAX, VBOs); // Create VBOs (n = Buffer.max)

        //Bind Vertex data
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, (long) vertexBuffer.capacity() * Float.BYTES, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

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
            gl.glVertexAttribPointer(INSTANCE_POSITION_ATTRIB_INDICE,
                    3,
                    GL_FLOAT,
                    false,
                    3 * Float.BYTES,
                    0);
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl.glVertexAttribDivisor(INSTANCE_POSITION_ATTRIB_INDICE, 1);
        }
        gl.glBindVertexArray(0);

        checkError(gl, "initVao");
    }

    private void updateInstanceData(GL3 gl){

        int entityCount = entities.size();

        HashMap<Model, List<Entity>> sortedEntities = new HashMap<>();
        for (Entity entity : entities) {
            if(!sortedEntities.containsKey(entity.m)){
                sortedEntities.put(entity.m, new ArrayList<>());
            }
            sortedEntities.get(entity.m).add(entity);
        }

        int indexCounter = 0;
        int instanceDataIndex = 0;
        float[] instanceData = new float[entityCount * 3];
        for (Model model : sortedEntities.keySet()) {
            for (Entity entity : sortedEntities.get(model)) {
                instanceData[instanceDataIndex++] = (float)entity.pos.x;
                instanceData[instanceDataIndex++] = (float)entity.pos.y;
                instanceData[instanceDataIndex++] = (float)entity.angle;

                entity.pos.x += Math.cos(entity.angle + Math.PI/2.0)/10.0;
                entity.pos.y += Math.sin(entity.angle + Math.PI/2.0)/10.0;
                entity.angle += 0.2/(double)instanceDataIndex;

            }
            instanceIndexes.put(model, indexCounter);
            sortedEntityCounts.put(model, sortedEntities.get(model).size());
            indexCounter += sortedEntities.get(model).size();
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
//        gl.glDrawArraysInstancedBaseInstance(GL_TRIANGLE_FAN, 3,  4, SQUARE_COUNT, 50);



        int instanceDataOffset = 0; //TODO changeme this should be more correcter
        for (Model value : Model.values()) {
            int entityCount = (int) entities.stream().filter(e -> e.m == value).count();
            gl.glDrawArraysInstancedBaseInstance(value.drawMode, verticeIndexes.get(value), value.points, 50, instanceDataOffset);
//            instanceDataOffset += 1;
        }


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