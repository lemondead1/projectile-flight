package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.VertexFormat;

public interface ModelTransformer {
  ModelTransformer IDENTITY = (a, v) -> {};

  void transform(float[] vertex, VertexFormat format);

  static ModelTransformer scale(float x, float y, float z) {
    return (vertex, format) -> {
      int offset = format.getOffset(VertexFormat.VertexAttribute.POSITION);
      vertex[offset] *= x;
      vertex[offset + 1] *= y;
      vertex[offset + 2] *= z;
    };
  }

  static ModelTransformer scale(float scale) {
    return scale(scale, scale, scale);
  }

}
