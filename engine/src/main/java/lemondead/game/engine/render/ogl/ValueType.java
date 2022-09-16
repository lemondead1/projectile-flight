package lemondead.game.engine.render.ogl;

import static org.lwjgl.opengl.GL20C.*;

@SuppressWarnings("unused")
public enum ValueType {
  BYTE(1, GL_BYTE, true),
  UNSIGNED_BYTE(1, GL_UNSIGNED_BYTE, false),
  SHORT(2, GL_SHORT, true),
  UNSIGNED_SHORT(2, GL_UNSIGNED_SHORT, false),
  INT(4, GL_INT, true),
  UNSIGNED_INT(4, GL_UNSIGNED_INT, false),
  FLOAT(4, GL_FLOAT, true);

  private final int byteSize;
  private final int constant;
  private final boolean signed;

  ValueType(int byteSize, int constant, boolean signed) {
    this.byteSize = byteSize;
    this.constant = constant;
    this.signed = signed;
  }

  public int getConstant() {
    return constant;
  }

  public int getByteSize() {
    return byteSize;
  }

  public boolean isSigned() {
    return signed;
  }
}
