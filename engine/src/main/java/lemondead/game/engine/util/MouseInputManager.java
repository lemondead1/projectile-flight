package lemondead.game.engine.util;

import lemondead.game.engine.util.vector.Vec2;
import org.lwjgl.glfw.GLFW;

public class MouseInputManager {
  private Vec2 mouseDragStartPos = Vec2.ZERO;
  private Vec2 prevPos;
  private Vec2 currentPos = Vec2.ZERO;

  private boolean pressed;
  private boolean isDragging;
  private DragHandler dragHandler;

  private MouseDragStartCallback dragCallback;
  private MouseClickCallback clickCallback;

  public MouseInputManager(WindowWrapper window) {
    GLFW.glfwSetMouseButtonCallback(window.location, (window1, button, action, mods) -> onMouseButton(button, action));
  }

  private void onMouseButton(int button, int action) {
    if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
      switch (action) {
        case GLFW.GLFW_PRESS:
          mouseDragStartPos = currentPos;
          pressed = true;
          break;
        case GLFW.GLFW_RELEASE:
          if (!isDragging && clickCallback != null) {
            clickCallback.click(currentPos.x(), currentPos.y(), MouseButton.LMB);
          } else if (isDragging && dragHandler != null) {
            dragHandler.onDragStop();
          }
          pressed = false;
          isDragging = false;
          dragHandler = null;
          break;
      }
    } else if (action == GLFW.GLFW_RELEASE && clickCallback != null) {
      switch (button) {
        case GLFW.GLFW_MOUSE_BUTTON_2:
          clickCallback.click(currentPos.x(), currentPos.y(), MouseButton.RMB);
          break;
        case GLFW.GLFW_MOUSE_BUTTON_3:
          clickCallback.click(currentPos.x(), currentPos.y(), MouseButton.WHEEL);
          break;
      }
    }
  }

  public void onMousePos(double x, double y) {
    prevPos = currentPos;
    currentPos = Vec2.of(x, y);
    if (isDragging && dragHandler != null) {
      dragHandler.onDrag(prevPos.x(), prevPos.y(), currentPos.x(), currentPos.y());
    }
    if (!isDragging && pressed && currentPos.subtract(mouseDragStartPos).lengthSq() > 9) {
      if (dragCallback != null) {
        DragHandler handler = dragCallback.drag(currentPos.x(), currentPos.y());
        if (handler != null) {
          dragHandler = handler;
        }
      }
      isDragging = true;
    }
  }

  public Vec2 getCurrentPos() {
    return currentPos;
  }

  public Vec2 getPrevPos() {
    return prevPos;
  }

  public void setDragStartCallback(MouseDragStartCallback dragCallback) {
    this.dragCallback = dragCallback;
  }

  public void setClickCallback(MouseClickCallback clickCallback) {
    this.clickCallback = clickCallback;
  }

  public boolean isPressed() {
    return pressed;
  }

  public boolean isDragging() {
    return isDragging;
  }

  public enum MouseButton {
    LMB,
    RMB,
    WHEEL
  }

  public interface MouseDragStartCallback {
    DragHandler drag(double startX, double startY);
  }

  public interface MouseClickCallback {
    void click(double x, double y, MouseButton button);
  }
}
