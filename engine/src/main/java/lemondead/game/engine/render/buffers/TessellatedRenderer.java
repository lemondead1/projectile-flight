package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.*;
import lemondead.game.engine.util.MathUtil;
import lemondead.game.engine.util.Util;
import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec2;
import lemondead.game.engine.util.vector.Vec3;
import lemondead.game.engine.util.vector.Vec4;
import org.lwjgl.opengl.GL20C;

import java.util.function.Consumer;

public final class TessellatedRenderer implements BufferRenderer {
  private final OGLBuffer vertexBuffer;
  private final OGLBuffer indexBuffer;
  private final int[] lods;
  private final Vec4[] measurementPoints;
  private final Program program;
  private final ValueType indexType;
  private final RenderMode mode;
  private final int transformIndex;

  public TessellatedRenderer(OGLBuffer vertexBuffer, OGLBuffer indexBuffer, int[] lods,
                             Vec3[] measurementPoints, ValueType indexType, Program program, RenderMode mode) {
    this.vertexBuffer = vertexBuffer;
    this.indexBuffer = indexBuffer;
    this.measurementPoints = new Vec4[] { Vec4.of(measurementPoints[0], 1), Vec4.of(measurementPoints[1], 1) };
    this.lods = lods;
    this.program = program;
    this.mode = mode;
    this.indexType = indexType;
    int transformIndex = -1;
    Program.Uniform<?>[] uniforms = program.getUniforms();
    for (int i = 0; i < uniforms.length; i++) {
      if (uniforms[i] == Program.Uniform.TRANSFORM_MATRIX) {
        transformIndex = i;
        break;
      }
    }
    this.transformIndex = transformIndex;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void draw(Consumer<UniformSetter> uniformGetter) {
    program.use();
    vertexBuffer.bind(GL20C.GL_ARRAY_BUFFER);
    program.applyVertexAttributes();

    Program.Uniform<?>[] uniforms = program.getUniforms();
    Object[] objects = new Object[uniforms.length];

    uniformGetter.accept(new UniformSetter() {
      @Override
      public <T> void setUniform(Program.Uniform<T> uniform, T value) {
        for (int i = 0; i < uniforms.length; i++) {
          if (uniforms[i] == uniform) {
            objects[i] = value;
          }
        }
      }
    });

    int[] uniformLocations = program.getUniformLocations();

    for (int i = 0; i < uniforms.length; i++) {
      Object object = objects[i];
      Program.Uniform<Object> uniform = (Program.Uniform<Object>) uniforms[i];
      uniform.set(uniformLocations[i], object == null ? uniform.getDefault() : object);
    }

    Mat4 transform = transformIndex == -1 ? Mat4.IDENTITY : (Mat4) objects[transformIndex];

    Vec4 vec1 = transform.multiply(measurementPoints[0]);
    Vec4 vec2 = transform.multiply(measurementPoints[1]);

    Vec2 viewportSize = OGLManager.getViewportSize();

    double w = viewportSize.x() / 2f;
    double h = viewportSize.y() / 2f;
    double f = 1 / (vec1.w() * 2);
    Vec2 norm1 = Vec2.of(vec1.x() * w * f + w, vec1.y() * h * f + h);
    f = 1 / (vec2.w() * 2);
    Vec2 norm2 = Vec2.of(vec2.x() * w * f + w, vec2.y() * h * f + h);

    double dis = norm1.subtract(norm2).length() / 20;

    int lod = MathUtil.clamp(0, Util.getNextPowerOf2((int) dis) / 2, lods.length - 2);

    indexBuffer.bind(GL20C.GL_ELEMENT_ARRAY_BUFFER);

    GL20C.glDrawElements(mode.getConstant(), lods[lod + 1] - lods[lod], indexType.getConstant(), (long) lods[lod] * indexType.getByteSize());

    program.removeAttributes();
  }

  @Override
  public void close() {
    indexBuffer.free();
    vertexBuffer.free();
  }
}
