import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import jogl.Semantic;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;

public abstract class GraphicsBase implements GLEventListener {
    private static GLWindow window;
    private static Animator animator;

    protected void setup() {

        GLProfile glProfile = GLProfile.get(GLProfile.GL3);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setTitle("Hello Triangle (simple)");
        window.setSize(1024, 768);


        window.setVisible(true);

        window.addGLEventListener(this);

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

    protected Program EntityProgram;
    protected Program BackgroundProgram;

    public class Program {

        //TODO Make the 'program' class extendable it's being overused and overburdened!
        public int name, modelToWorldMatUL, positionInSpace, time;

        public Program(GL3 gl, Class context, String root, String vertex, String fragment, boolean linkUniforms) {

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

            //TODO This is overburdened
            if(linkUniforms) {
                modelToWorldMatUL = gl.glGetUniformLocation(name, "model");

                if (modelToWorldMatUL == -1) {
                    System.err.println("uniform 'model' not found!");
                }


                int globalMatricesBI = gl.glGetUniformBlockIndex(name, "GlobalMatrices");

                if (globalMatricesBI == -1) {
                    System.err.println("block index 'GlobalMatrices' not found!");
                }

                gl.glUniformBlockBinding(name, globalMatricesBI, Semantic.Uniform.GLOBAL_MATRICES);
            }else{
                positionInSpace = gl.glGetUniformLocation(name, "position");
                if(positionInSpace == -1){
                    System.err.println("uniform 'position' not found!");
                }
                time = gl.glGetUniformLocation(name, "time");
                if(time == -1){
                    System.err.println("uniform 'time' not found!");
                }

            }
        }
    }



    public void checkError(GL gl, String location) {

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
