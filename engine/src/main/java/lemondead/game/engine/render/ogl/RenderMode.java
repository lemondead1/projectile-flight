package lemondead.game.engine.render.ogl;

import org.lwjgl.opengl.GL20C;

public enum RenderMode {
  SOLID(GL20C.GL_TRIANGLES, 3),
  LINES(GL20C.GL_LINES, 2);

  private final int constant;
  private final int verticesPerPrimitive;

  RenderMode(int constant, int verticesPerPrimitive) {
    this.constant = constant;
    this.verticesPerPrimitive = verticesPerPrimitive;
  }

  public int getConstant() {
    return constant;
  }

  public int getVerticesPerPrimitive() {
    return verticesPerPrimitive;
  }
}
