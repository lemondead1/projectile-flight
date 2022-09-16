package lemondead.game.engine.render.ogl;

import org.lwjgl.opengl.GL20C;

import java.util.Arrays;

public final class VertexFormat {
  public static final VertexFormat POSITION_COLOR = new VertexFormat(VertexAttribute.POSITION, VertexAttribute.COLOR);
  public static final VertexFormat POSITION_COLOR_TEXTURE = new VertexFormat(VertexAttribute.POSITION, VertexAttribute.COLOR,
                                                                             VertexAttribute.TEXTURE_POS);

  private final VertexAttribute[] attributes;
  private final int vertexByteSize;
  private final int vertexValueCount;
  private final int[] offsets = new int[VertexAttribute.values().length];

  private VertexFormat(VertexAttribute... attributes) {
    this.attributes = attributes;
    vertexByteSize = Arrays.stream(attributes).mapToInt(VertexAttribute::getByteSize).sum();
    vertexValueCount = Arrays.stream(attributes).mapToInt(VertexAttribute::getValueCount).sum();
    Arrays.fill(offsets, -1);
    int offset = 0;
    for (VertexAttribute attribute : attributes) {
      offsets[attribute.ordinal()] = offset;
      offset += attribute.getByteSize();
    }
  }

  public int getOffset(VertexAttribute attribute) {
    int offset = offsets[attribute.ordinal()];
    if (offset == -1) {
      throw new RuntimeException("Attribute " + attribute.getAttribute() + " is not present.");
    }
    return offset;
  }

  public int getVertexByteSize() {
    return vertexByteSize;
  }

  public int getVertexValueCount() {
    return vertexValueCount;
  }

  public VertexAttribute[] getAttributes() {
    return attributes;
  }

  public enum VertexAttribute {
    POSITION(12, 3, GL20C.GL_FLOAT, false, "position"),
    TEXTURE_POS(4, 2, GL20C.GL_UNSIGNED_SHORT, true, "texPos"),
    COLOR(4, 4, GL20C.GL_UNSIGNED_BYTE, true, "color");

    private final int byteSize;
    private final int valueCount;
    private final int valueType;
    private final String attribute;
    private final boolean normalized;

    VertexAttribute(int byteSize, int valueCount, int valueType, boolean normalized, String attribute) {
      this.byteSize = byteSize;
      this.valueCount = valueCount;
      this.valueType = valueType;
      this.attribute = attribute;
      this.normalized = normalized;
    }

    public int getByteSize() {
      return byteSize;
    }

    public int getValueType() {
      return valueType;
    }

    public int getValueCount() {
      return valueCount;
    }

    public String getAttribute() {
      return attribute;
    }

    public boolean isNormalized() {
      return normalized;
    }
  }
}
