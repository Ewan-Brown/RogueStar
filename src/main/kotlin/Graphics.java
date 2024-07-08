import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import jogl.Semantic;
import lombok.Getter;
import lombok.Setter;
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
public class Graphics extends GraphicsBase {

    private IntBuffer VBOs = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer VAOs = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16);

    HashMap<Model, ModelData> modelData = new HashMap<>();

    //The position to shift the background by
    float x = 0;
    float y = 0;

    //the time for the background
    float time = 0;

    @Getter
    @Setter
    private class ModelData{
        int verticeIndex;
        int instanceIndex;
        List<Transform> instanceData = new ArrayList<>();
        public int getInstanceCount(){
            return instanceData.size();
        }
    }

    final List<Model> loadedModels;

    public Graphics(List<Model> preloadedModels) {
        this.loadedModels = preloadedModels;
        for (Model preloadedModel : preloadedModels) {
            modelData.put(preloadedModel, new ModelData());
        }
    }

    public void updateDrawables(HashMap<Model, List<Transform>> data){
        for (Model loadedModel : loadedModels) {
            modelData.get(loadedModel).setInstanceData(data.get(loadedModel));
        }
    }

    public static class Transform{
        private final Vector2 position;
        private final float angle;

        public Transform(Vector2 pos, float a){
            this.position = pos.copy();
            this.angle = a;
        }

        public Vector2 getPosition() {
            return position;
        }

        public float getAngle() {
            return angle;
        }
    }

    public static class Model {

        static Model TRIANGLE = new Model(new float[]{
                +0.0f, +2.0f, +0.1F, 1, 0, 0,
                -1.0f, -1.0f, +0.1F, 1, 0, 0,
                +1.0f, -1.0f, +0.1F, 1, 0, 0}, GL_TRIANGLES);
        static Model SQUARE1 = new Model(new float[]{
                -0.5f, -0.5f, +0.1F, 0, 1, 0,
                +0.5f, -0.5f, +0.1F, 0, 1, 0,
                +0.5f, +0.5f, +0.1F, 0, 1, 0,
                -0.5f, +0.5f, +0.1F, 0, 1, 0},GL_TRIANGLE_FAN);
        static Model SQUARE2 = new Model(new float[]{
                -0.5f, -0.5f, 3, 0, 1, 0,
                +0.5f, -0.5f, 3, 0, 0, 1,
                +0.5f, +0.5f, 3, 0, 1, 0,
                -0.5f, +0.5f, 3, 1, 0, 0}, GL_TRIANGLE_FAN);
        static Model BACKPLATE = new Model(new float[]{
                -1, -1, +0.4f, 0, 1, 1,
                -1, +1, +0.4f, 0, 1, 1,
                +1, +1, +0.4f, 0, 1, 1,
                +1, -1, +0.4f, 0, 1, 1}, GL_TRIANGLE_FAN);

        final int points;
        final float[] vertexData;
        final int drawMode;
        final Vector2[] asVectorData;

        Model(float[] vertexData, int dMode) {
            this.points = vertexData.length/6; //Change if vertex data size changes!
            asVectorData = new Vector2[points];
            for (int i = 0; i < points; i++) {
                asVectorData[i] = new Vector2(vertexData[i*6], vertexData[i*6+1]);
            }
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

        updateInstanceData(gl);

        initVAOs(gl);

        initProgram(gl);

        gl.glEnable(GL_DEPTH_TEST);

    }

    private void initVBOs(GL3 gl) {



        //Generate vertex data and store offsets for models
        List<Float> verticeList = new ArrayList<>();

        int marker = 0;
        for (Model value : loadedModels) {
            for (float vertexDatum : value.vertexData) {
                verticeList.add(vertexDatum);
            }
            modelData.get(value).setVerticeIndex(marker);
            marker += value.points;
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

    int ticks = 0;
    List<Long> timeBuckets = new ArrayList<>();

    private void updateInstanceData(GL3 gl){

        List<Long> timestamps = new ArrayList<>();

        timestamps.add(System.nanoTime());
        int modelCount = modelData.values().stream().mapToInt(ModelData::getInstanceCount).sum();

        //TODO this should come pre-sorted...
//        HashMap<Model, List<Transform>> sortedComponents = new HashMap<>();
//        for (DrawableInstance instance : drawableThings) {
//            sortedComponents.putIfAbsent(instance.model, new ArrayList<>());
//            sortedComponents.get(instance.model).add(instance.transform);
//            modelCount++;
//        }
        timestamps.add(System.nanoTime());

        int indexCounter = 0;
        int instanceDataIndex = 0;
        float[] instanceData = new float[modelCount * 3];
        for (ModelData data : modelData.values()) {
            for (Transform transform : data.getInstanceData()) {

                double x = transform.position.x;
                double y = transform.position.y;
                double angle = transform.angle;

//                System.out.println("x, y, angle : " + x + ", " + y + ", " + angle);

                instanceData[instanceDataIndex++] = (float)x;
                instanceData[instanceDataIndex++] = (float)y;
                instanceData[instanceDataIndex++] = (float)angle;

            }
            data.setInstanceIndex(indexCounter);
            indexCounter += data.getInstanceCount();
        }
        timestamps.add(System.nanoTime());

        FloatBuffer instanceBuffer = GLBuffers.newDirectFloatBuffer(instanceData);
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.INSTANCED_STUFF));
        gl.glBufferData(GL_ARRAY_BUFFER, (long) instanceBuffer.capacity() * Float.BYTES, instanceBuffer, GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);
        timestamps.add(System.nanoTime());

        timestamps.add(System.nanoTime());
        int bucketsMissing = (timestamps.size()-1) - timeBuckets.size();
        if(bucketsMissing > 0){
            for (int j = 0; j < bucketsMissing; j++) {
                timeBuckets.add(0L);
            }
        }
        for (int i = 0; i < timestamps.size()-1; i++) {
            long tDiff = timestamps.get(i+1) - timestamps.get(i);

            timeBuckets.set(i, timeBuckets.get(i)+tDiff);
        }
        ticks++;

//        System.out.println();
//        if(ticks % 10 == 0){
//            for (Long timeBucket : timeBuckets) {
//                System.out.println(timeBucket);
//            }
//        }

    }

    private void initProgram(GL3 gl) {

        EntityProgram = new Program(gl, getClass(), "", "Game_Entity", "Game_Entity", true);
        checkError(gl, "initProgram : Entity");

        BackgroundProgram = new Program(gl, getClass(), "", "Game_Background", "Game_Background", false);
        checkError(gl, "initProgram : Background");
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

        gl.glBindVertexArray(VAOs.get(0));

        gl.glUseProgram(BackgroundProgram.name);

        gl.glUniform2f(BackgroundProgram.positionInSpace, x, y);
        gl.glUniform1f(BackgroundProgram.time, time);

        gl.glDrawArrays(Model.BACKPLATE.drawMode, modelData.get(Model.BACKPLATE).getVerticeIndex(), Model.BACKPLATE.points);

        gl.glUseProgram(0);

        gl.glUseProgram(EntityProgram.name);

        // model matrix
        {
            float[] scale = FloatUtil.makeScale(new float[16], true, 0.1f, 0.1f, 1f);
            float[] zRotation = FloatUtil.makeRotationEuler(new float[16], 0, 0, 0, 0.0f);
            float[] modelToWorldMat = FloatUtil.multMatrix(scale, zRotation);

            for (int i = 0; i < 16; i++) {
                matBuffer.put(i, modelToWorldMat[i]);
            }
            gl.glUniformMatrix4fv(EntityProgram.modelToWorldMatUL, 1, false, matBuffer);
        }

        for (Map.Entry<Model, ModelData> value : modelData.entrySet()) {
            Model model = value.getKey();
            ModelData data = value.getValue();
            if(data.getInstanceCount() > 0) {
                gl.glDrawArraysInstancedBaseInstance(model.drawMode, data.getVerticeIndex(), model.points, data.getInstanceCount(), data.getInstanceIndex());
            }
        }

        gl.glUseProgram(0);
        gl.glBindVertexArray(0);

        checkError(gl, "display");

        x = (float)Math.cos(time);
        y = (float)Math.sin(time);
        time += 0.1f;
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

        gl.glDeleteProgram(EntityProgram.name);
        gl.glDeleteVertexArrays(1, VAOs);
        gl.glDeleteBuffers(Buffer.MAX, VBOs);
    }
}