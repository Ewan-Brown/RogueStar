import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import jogl.Semantic;
import kotlin.Pair;
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

    //Points to where the start of each model's vertex data is in the VERTEX buffer
    final HashMap<Model, Integer> verticeIndexes = new HashMap<>(); //Can probably be attached directly to a model object (wrap the Enum in an object to make mutable fields)

    //Points to where each grouping of model starts in the INSTANCE buffer
    // (InstanceData is sorted by model!)
    final HashMap<Model, Integer> instanceIndexes = new HashMap<>();

    //List of the counts of instances sorted by model type
    //Source of truth for populating the other indexes ! This must be updated before the others when adding/removing instances/models
    final HashMap<Model, Integer> sortedModelCounts = new HashMap<>();

    boolean firstModelUpdate = true;

    public static void main(String[] args) {
        Graphics gui = new Graphics();

        List<DrawableThing> drawables = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            DrawableThing e = new Entity(getRandomTransform(10), List.of(
                    new Pair<>(Model.TRIANGLE, buildZeroTransform()),
                    new Pair<>(Model.SQUARE1, buildTransform(-0.5d, 0.0d, 0)),
                    new Pair<>(Model.SQUARE2, buildTransform(0.5d, 0.0d, 0))));
            drawables.add(e);
//            thingsToDraw.add(e);
//            for (Model m : e.getRequiredModels()) {
//                sortedModelCounts.putIfAbsent(m, 0);
//                sortedModelCounts.put(m, sortedModelCounts.get(m) + 1);
//            }
        }

        gui.updateDrawables(drawables);
        gui.setup();
    }

    public void updateDrawables(List<DrawableThing> drawables){
        this.thingsToDraw = drawables;
        Set<Model> currentModels = verticeIndexes.keySet();
        for (DrawableThing drawable : drawables) {
            List<Model> models = drawable.getRequiredModels();
            for (Model model : models) {
                if(!firstModelUpdate && !currentModels.contains(model)) {
                    shouldReloadModels = true;
                    throw new RuntimeException("Attempted to update drawables with models that are not yet loaded, this is not yet supported");
                }
                sortedModelCounts.putIfAbsent(model, 0);
                sortedModelCounts.put(model, sortedModelCounts.get(model) + 1);
            }
        }
        firstModelUpdate = false;
    }


    private boolean shouldReloadModels = false;

    private List<DrawableThing> thingsToDraw = new ArrayList<>();

    private static Transform getRandomTransform(int mult){
        return buildTransform((Math.random() - 0.5) * mult, (Math.random() - 0.5) * mult, (float)(Math.random()*2.0*Math.PI));
    }

    private static Transform getRandomTransform(){
        return getRandomTransform(1);
    }

    private static Transform buildTransform(double x, double y, float a){
        return new Transform(new Vector2(x, y), a);
    }

    private static Transform buildZeroTransform(){
        return new Transform(new Vector2(0,0), 0);
    }

    public interface DrawableThing{
        public List<Pair<Model, Transform>> getTransformedComponents();
        public List<Model> getRequiredModels();
    }

    private static class Entity implements DrawableThing{

        private Transform transform;
        private final List<Pair<Model, Transform>> models;

        public List<Pair<Model, Transform>> getTransformedComponents(){
            List<Pair<Model, Transform>> result = new ArrayList<>();

            final float entityAngle = transform.angle;
            final Vector2 entityPos = transform.position;

            for (Pair<Model, Transform> component : models) {
                Vector2 newPos = component.component2().position.copy().rotate(entityAngle).add(entityPos);
                float newAngle = entityAngle + component.component2().angle;
                result.add(new Pair<>(component.component1(),new Transform(newPos, newAngle)));
            }

            return result;
        }

        public List<Model> getRequiredModels(){
            List<Model> result = new ArrayList<>();
            for (Pair<Model, Transform> model : models) {
                if(result.contains(model.component1())) continue;
                result.add(model.component1());
            }
            return result;
        }

        public void updateTransform(Transform t){
            transform = t;
        }

        public Transform getTransform(){
            return transform;
        }

        public Entity(Transform initialTransform, List<Pair<Model, Transform>> m) {
            transform = initialTransform;
            models = m;
        }
    }

    public record Transform(Vector2 position, float angle){}

    public static class Model {

        static Model TRIANGLE = new Model(new float[]{
                -1.0f, -1.0f, 2, 1, 0, 0,
                +0.0f, +2.0f, 2, 1, 0, 0,
                +1.0f, -1.0f, 2, 1, 0, 0}, GL_TRIANGLES);
        static Model SQUARE1 = new Model(new float[]{
                -0.5f, -0.5f, 1, 0, 1, 0,
                -0.5f, +0.5f, 1, 0, 1, 0,
                +0.5f, +0.5f, 1, 0, 1, 0,
                +0.5f, -0.5f, 1, 0, 1, 0}, GL_TRIANGLE_FAN);
        static Model SQUARE2 = new Model(new float[]{
                -0.5f, -0.5f, 1, 1, 1, 0,
                -0.5f, +0.5f, 1, 1, 1, 0,
                +0.5f, +0.5f, 1, 1, 1, 0,
                +0.5f, -0.5f, 1, 1, 1, 0}, GL_TRIANGLE_FAN);

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

        updateInstanceData(gl, thingsToDraw);

        initVAOs(gl);

        initProgram(gl);

        gl.glEnable(GL_DEPTH_TEST);

    }

    private void initVBOs(GL3 gl) {

        //Generate vertex data and store offsets for models
        List<Float> verticeList = new ArrayList<>();
        int marker = 0;
        for (Model value : sortedModelCounts.keySet()) {
            for (float vertexDatum : value.vertexData) {
                verticeList.add(vertexDatum);
            }
            verticeIndexes.put(value, marker);
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

//    private void updateEntities(){
//        for (Entity entity : thingsToDraw) {
//            Transform transform = entity.getTransform();
//            double x = transform.position.x;
//            double y = transform.position.y;
//            double angle = transform.angle();
//
//            x += Math.cos(angle + Math.PI/2.0)/10.0;
//            y += Math.sin(angle + Math.PI/2.0)/10.0;
//            angle += 0.2/ thingsToDraw.indexOf(entity);
//
//            entity.updateTransform(buildTransform(x, y, (float)angle));
//        }
//
//    }

    int ticks = 0;
    List<Long> timeBuckets = new ArrayList<>();

    private void updateInstanceData(GL3 gl, List<? extends DrawableThing> drawableThings){

        List<Long> timestamps = new ArrayList<>();

        timestamps.add(System.nanoTime());
        int modelCount = 0;

        HashMap<Model, List<Transform>> sortedComponents = new HashMap<>();
        for (DrawableThing drawable : drawableThings) {
            var components = drawable.getTransformedComponents();
            for (Pair<Model, Transform> component : components) {
                Model model = component.component1();
                Transform transform = component.component2();
                if(!sortedComponents.containsKey(model)){
                    sortedComponents.put(model, new ArrayList<>());
                }
                sortedComponents.get(model).add(transform);
                modelCount++;
            }

        }
        timestamps.add(System.nanoTime());

        int indexCounter = 0;
        int instanceDataIndex = 0;
        float[] instanceData = new float[modelCount * 3];
        for (Model model : sortedComponents.keySet()) {
            for (Transform transform : sortedComponents.get(model)) {

                double x = transform.position.x;
                double y = transform.position.y;
                double angle = transform.angle;

//                System.out.println("x, y, angle : " + x + ", " + y + ", " + angle);

                instanceData[instanceDataIndex++] = (float)x;
                instanceData[instanceDataIndex++] = (float)y;
                instanceData[instanceDataIndex++] = (float)angle;

            }
            instanceIndexes.put(model, indexCounter);
            sortedModelCounts.put(model, sortedComponents.get(model).size());
            indexCounter += sortedComponents.get(model).size();
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

        for (Model value : sortedModelCounts.keySet()) {
            if(sortedModelCounts.get(value) != null && sortedModelCounts.get(value) > 0) {
                gl.glDrawArraysInstancedBaseInstance(value.drawMode, verticeIndexes.get(value), value.points, sortedModelCounts.get(value), instanceIndexes.get(value));
            }
        }


        gl.glUseProgram(0);
        gl.glBindVertexArray(0);

        checkError(gl, "display");

        updateInstanceData(gl, thingsToDraw);
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