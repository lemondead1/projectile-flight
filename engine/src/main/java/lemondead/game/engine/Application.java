package lemondead.game.engine;

import lemondead.game.engine.render.TextRenderer;
import lemondead.game.engine.render.ogl.OGLManager;
import lemondead.game.engine.render.ogl.Program;
import lemondead.game.engine.util.WindowWrapper;
import lemondead.game.engine.util.vector.Vec2;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20C;

import java.util.Objects;

public abstract class Application {
  protected WindowWrapper window;
  public TextRenderer textRenderer;
  protected long prevFrameTime = 1;

  protected String title = "Title";

  public void run() {
    setup();
    window.show();
    loop();
    stop();
  }

  protected void setup() {
    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }

    GLFWErrorCallback.createPrint(System.err).set();

    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 8);

    window = new WindowWrapper(title, 800, 800);

    Vec2.OfInt size = window.getSize();
    GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
    Objects.requireNonNull(vidMode);
    window.setPos((vidMode.width() - size.x) / 2, (vidMode.height() - size.y) / 2);

    window.makeContextCurrent();
    GL.createCapabilities();

    GLFW.glfwSwapInterval(1);
    OGLManager.init();
    Program.init();
    GL20C.glClearColor(1, 1, 1, 1);
    GL20C.glEnable(GL20C.GL_BLEND);
    GL20C.glBlendFunc(GL20C.GL_SRC_ALPHA, GL20C.GL_ONE_MINUS_SRC_ALPHA);
    GL20C.glEnable(GL20C.GL_MULTISAMPLE);
    GL20C.glLineWidth(2);
    GL20C.glDepthRange(0, 1);

    textRenderer = new TextRenderer();
  }

  private void loop() {
    while (!window.shouldClose()) {
      long frameStartTime = System.nanoTime();
      onFrame();
      window.swapBuffers();
      prevFrameTime = System.nanoTime() - frameStartTime;
    }
  }

  protected void stop() {
    window.freeCallbacks();
    window.destroy();
    GLFW.glfwTerminate();
    Objects.requireNonNull(GLFW.glfwSetErrorCallback(null)).free();
  }

  public abstract void onFrame();
}
