package lemondead.game.engine.util;

public interface DragHandler {
  void onDrag(double prevX, double prevY, double nextX, double nextY);

  default void onDragStop() {

  }
}
