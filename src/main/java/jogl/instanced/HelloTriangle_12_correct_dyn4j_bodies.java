package jogl.instanced;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import jogl.HelloTriangle_Base;
import jogl.Semantic;
import kotlin.Pair;
import org.dyn4j.collision.Fixture;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.*;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.world.World;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.List;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES3.*;
import static com.jogamp.opengl.GL3.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL3.GL_DEPTH_TEST;
import static com.jogamp.opengl.GL3.GL_FLOAT;

/**
 * Render stuff things
 */
public class HelloTriangle_12_correct_dyn4j_bodies extends HelloTriangle_Base implements KeyListener {

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

    final HashMap<Model, Integer> sortedModelCounts = new HashMap<>();

    World<Entity> w = new World();

    public HelloTriangle_12_correct_dyn4j_bodies() {
        w.setGravity(0, 0);
    }

    public static void main(String[] args) {
        HelloTriangle_12_correct_dyn4j_bodies gui = new HelloTriangle_12_correct_dyn4j_bodies();
        gui.setup(gui);
    }

    List<Entity> entities = new ArrayList<>();

    Entity player;

    private static Transform getRandomTransform(int mult){
        return buildTransform((Math.random() - 0.5) * mult, (Math.random() - 0.5) * mult, (float)(Math.random()*2.0*Math.PI));
    }

    private static Transform getRandomTransform(){
        return getRandomTransform(1);
    }

    private static Transform buildTransform(double x, double y, float a){
        return new Transform(new Vector2(x, y), a);
    }

    private static Transform getZeroTransform(){
        return new Transform(new Vector2(0,0), 0);
    }

    {
        Entity p = new Entity(getZeroTransform(), List.of(
                new Pair<>(Model.SQUARE1, buildTransform(1.0d, 0.0d, 0)),
                new Pair<>(Model.SQUARE1, buildTransform(0.0d, 0.0d, 0))
        ));
        entities.add(p);
        w.addBody(p);
        for (int i = 0; i < 10; i++) {
            Entity e = new Entity(getRandomTransform(10), List.of(
//                    new Pair<>(Model.TRIANGLE, getZeroTransform()),
//                    new Pair<>(Model.SQUARE1, buildTransform(0.0d, 0.0d, (float)(Math.PI/2.0)))
                    new Pair<>(Model.SQUARE1, buildTransform(0.0d, 0.0d, 0))
//                    new Pair<>(Model.SQUARE1, buildTransform(-0.5d, 0.0d, 0)),
//                    new Pair<>(Model.SQUARE2, buildTransform(0.5d, 0.0d, 0))
            ));
            entities.add(e);
            w.addBody(e);
        }
        player = entities.get(0);
    }

    Vector2 desiredVelocity = new Vector2(0, 0);
    float desiredRotationalVelocity = 0;
    BitSet keySet = new BitSet(256);

    @Override
    public void keyPressed(KeyEvent e) {
        keySet.set(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if(!e.isAutoRepeat()) {
            keySet.set(e.getKeyCode(), false);
        }
    }

    private static class Entity extends Body {

        private final List<Pair<Model, HelloTriangle_12_correct_dyn4j_bodies.Transform>> models;

        public List<Pair<Model, Transform>> getTransformedComponents(){
            List<Pair<Model, Transform>> result = new ArrayList<>();

            final float entityAngle = (float) this.getTransform().getRotationAngle();
            final Vector2 entityPos = this.getWorldCenter();

            for (Pair<Model, Transform> component : models) {
                Vector2 newPos = component.component2().position.copy().rotate(entityAngle).add(entityPos);
                float newAngle = entityAngle + component.component2().angle;
                result.add(new Pair<>(component.component1(),new Transform(newPos, newAngle)));
            }

            return result;
        }

        public Entity(Transform initialTransform, List<Pair<Model, Transform>> m) {

            this.translate(initialTransform.position);
            this.rotate(initialTransform.angle);
            for (Pair<Model, Transform> pair : m) {
                Convex v = new Polygon(pair.component1().asVectorData);
                v.translate(pair.component2().position.product(1.0));
                v.rotate(pair.component2().angle());
                System.out.println(pair.component2().angle);
                BodyFixture f = new BodyFixture(v);
                this.addFixture(f);

            }

            this.setMass(MassType.NORMAL);


            models = m;
        }

        public void render(Graphics2D g, double scale, Color color) {
            // point radius
            final int pr = 4;

            // save the original transform
            AffineTransform ot = g.getTransform();

            // transform the coordinate system from world coordinates to local coordinates
            AffineTransform lt = new AffineTransform();
            lt.translate(this.transform.getTranslationX() * scale, this.transform.getTranslationY() * scale);
            lt.rotate(this.transform.getRotationAngle());

            // apply the transform
            g.transform(lt);

            // loop over all the body fixtures for this body
            for (BodyFixture fixture : this.fixtures) {
                this.renderFixture(g, scale, fixture, color);
            }

            // draw a center point
            Ellipse2D.Double ce = new Ellipse2D.Double(
                    this.getLocalCenter().x * scale - pr * 0.5,
                    this.getLocalCenter().y * scale - pr * 0.5,
                    pr,
                    pr);
            g.setColor(Color.WHITE);
            g.fill(ce);
            g.setColor(Color.DARK_GRAY);
            g.draw(ce);

            // set the original transform
            g.setTransform(ot);
        }

        protected void renderFixture(Graphics2D g, double scale, BodyFixture fixture, Color color) {
            // get the shape on the fixture
            Convex convex = fixture.getShape();

            // brighten the color if asleep
            if (this.isAtRest()) {
                color = color.brighter();
            }

            // render the fixture
            Graphics2DRenderer.render(g, convex, scale, color);
        }

    }

    private record Transform(Vector2 position, float angle){}

    private enum Model {
        TRIANGLE(new float[]{
                +0.0f, +2.0f, 2, 1, 0, 0,
                -1.0f, -1.0f, 2, 1, 0, 0,
                +1.0f, -1.0f, 2, 1, 0, 0
        }, GL_TRIANGLES),
        SQUARE1(new float[]{
                -0.5f, -0.5f, 1, 0, 1, 0,
                +0.5f, -0.5f, 1, 0, 1, 0,
                +0.5f, +0.5f, 1, 0, 1, 0,
                -0.5f, +0.5f, 1, 0, 1, 0
        }, GL_TRIANGLE_FAN),
        SQUARE2(new float[]{
                -0.5f, -0.5f, 1, 0, 1, 1,
                +0.5f, -0.5f, 1, 0, 1, 1,
                +0.5f, +0.5f, 1, 0, 1, 1,
                -0.5f, +0.5f, 1, 0, 1, 1
        }, GL_TRIANGLE_FAN);
        final int points;
        final float[] vertexData;
        final Vector2[] asVectorData;
        final int drawMode;

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

        initVAOs(gl);

        initProgram(gl);

        gl.glEnable(GL_DEPTH_TEST);

    }

    private void initVBOs(GL3 gl) {

        //Generate vertex data and store offsets for models
        List<Float> verticeList = new ArrayList<>();
        int marker = 0;
        for (Model value : Model.values()) {
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

    private void updateEntities(){

        float x = 0;
        float y = 0;
        float a = 0;

        if(keySet.get(KeyEvent.VK_W)){
            y++;
        }
        if(keySet.get(KeyEvent.VK_S)){
            y--;
        }
        if(keySet.get(KeyEvent.VK_A)){
            x--;
        }
        if(keySet.get(KeyEvent.VK_D)){
            x++;
        }
        if(keySet.get(KeyEvent.VK_Q)){
            a++;
        }
        if(keySet.get(KeyEvent.VK_E)){
            a--;
        }

        if(keySet.get(KeyEvent.VK_SPACE)){
            keySet.set(KeyEvent.VK_SPACE, false);
            BufferedImage bI = new BufferedImage(1920,1080, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) bI.getGraphics();

            for (Entity entity : entities) {
                entity.render(g, 10, Color.RED);
            }

            JFrame frame = new JFrame();
            JPanel panel = new JPanel(){
                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    g.drawImage(bI, 0, 0, null);
                }
            };
            frame.add(panel);
            frame.setSize(1920,1080);
            frame.setVisible(true);
        }

        desiredVelocity = new Vector2(x, y);
        desiredRotationalVelocity = a;

        player.applyForce(desiredVelocity.multiply(2));
        player.applyTorque(desiredRotationalVelocity*2);

        w.update(1.0);

    }

    private void updateInstanceData(GL3 gl){

        int modelCount = 0;

        HashMap<Model, List<Transform>> sortedComponents = new HashMap<>();
        for (Entity entity : entities) {
            var components = entity.getTransformedComponents();
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

        int indexCounter = 0;
        int instanceDataIndex = 0;
        float[] instanceData = new float[modelCount * 3];
        for (Model model : sortedComponents.keySet()) {
            for (Transform transform : sortedComponents.get(model)) {

                double x = transform.position.x;
                double y = transform.position.y;
                double angle = transform.angle;

                instanceData[instanceDataIndex++] = (float)x;
                instanceData[instanceDataIndex++] = (float)y;
                instanceData[instanceDataIndex++] = (float)angle;

            }
            instanceIndexes.put(model, indexCounter);
            sortedModelCounts.put(model, sortedComponents.get(model).size());
            indexCounter += sortedComponents.get(model).size();
        }

        FloatBuffer instanceBuffer = GLBuffers.newDirectFloatBuffer(instanceData);
        gl.glBindBuffer(GL_ARRAY_BUFFER, VBOs.get(Buffer.INSTANCED_STUFF));
        gl.glBufferData(GL_ARRAY_BUFFER, (long) instanceBuffer.capacity() * Float.BYTES, instanceBuffer, GL_DYNAMIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        updateEntities();
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

        for (Model value : Model.values()) {
            if(sortedModelCounts.get(value) != null){
                gl.glDrawArraysInstancedBaseInstance(value.drawMode, verticeIndexes.get(value), value.points, sortedModelCounts.get(value), instanceIndexes.get(value));
            }
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