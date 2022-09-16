package lemondead.game.engine.util;

import lemondead.game.engine.render.Image;
import lemondead.game.engine.util.vector.Vec2;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class WindowWrapper {
  final long location;

  public WindowWrapper(String title, int width, int height) {
    location = glfwCreateWindow(width, height, title, NULL, NULL);
    if (location == NULL) {
      throw new IllegalStateException("Not able to create a window.");
    }
  }

  public void show() {
    glfwShowWindow(location);
  }

  public void hide() {
    glfwHideWindow(location);
  }

  public void makeContextCurrent() {
    glfwMakeContextCurrent(location);
  }

  public void swapBuffers() {
    glfwSwapBuffers(location);
  }

  public void setSize(int width, int height) {
    glfwSetWindowSize(location, width, height);
  }

  public void setIcon(Image... images) {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      GLFWImage.Buffer buffer = GLFWImage.mallocStack(images.length, stack);
      for (int i = 0; i < images.length; i++) {
        Image image = images[i];
        buffer.position(i);
        buffer.height(image.getHeight());
        buffer.width(image.getWidth());
        buffer.pixels(image.getBuffer());
      }
      glfwSetWindowIcon(location, buffer);
    }
  }

  public Vec2.OfInt getSize() {
    int[] x = new int[1];
    int[] y = new int[1];
    glfwGetWindowSize(location, x, y);
    return Vec2.of(x[0], y[0]);
  }

  public void setPos(int x, int y) {
    glfwSetWindowPos(location, x, y);
  }

  public Vec2.OfInt getPos() {
    int[] x = new int[1];
    int[] y = new int[1];
    glfwGetWindowPos(location, x, y);
    return Vec2.of(x[0], y[0]);
  }

  public void setCursorPos(double xPos, double yPos) {
    glfwSetCursorPos(location, xPos, yPos);
  }

  public Vec2 getCursorPos() {
    try (MemoryStack stack = MemoryStack.stackPush()) {
      DoubleBuffer x = stack.mallocDouble(1);
      DoubleBuffer y = stack.mallocDouble(1);
      glfwGetCursorPos(location, x, y);
      return Vec2.of(x.get(0), y.get(0));
    }
  }

  public void freeCallbacks() {
    Callbacks.glfwFreeCallbacks(location);
  }

  public void destroy() {
    glfwDestroyWindow(location);
  }

  public void setShouldClose(boolean close) {
    glfwSetWindowShouldClose(location, close);
  }

  public boolean shouldClose() {
    return glfwWindowShouldClose(location);
  }

  public void setScrollCallback(ScrollCallback callback) {
    glfwSetScrollCallback(location, callback);
  }

  @FunctionalInterface
  public interface ScrollCallback extends GLFWScrollCallbackI {
    @Override
    default void invoke(long window, double xoffset, double yoffset) {
      invoke(xoffset, yoffset);
    }

    void invoke(double xOffset, double yOffset);
  }

  public void setFramebufferSizeCallback(FramebufferSizeCallback callback) {
    glfwSetFramebufferSizeCallback(location, callback);
  }

  @FunctionalInterface
  public interface FramebufferSizeCallback extends GLFWFramebufferSizeCallbackI {
    @Override
    default void invoke(long window, int width, int height) {
      invoke(width, height);
    }

    void invoke(int width, int height);
  }
}
