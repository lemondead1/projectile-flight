package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.OGLBuffer;
import lemondead.game.engine.render.ogl.Program;
import lemondead.game.engine.render.ogl.RenderMode;
import lemondead.game.engine.render.ogl.ValueType;
import org.lwjgl.opengl.GL20C;

import java.util.function.Consumer;

public final class SimpleRenderer implements BufferRenderer {
  private final OGLBuffer indexBuffer;
  private final OGLBuffer vertexBuffer;
  private final int indexCount;
  private final Program program;
  private final RenderMode mode;
  private final ValueType indexType;

  public SimpleRenderer(OGLBuffer vertexBuffer, OGLBuffer indexBuffer, int indexCount,
                        ValueType indexType, Program program, RenderMode mode) {
    this.indexBuffer = indexBuffer;
    this.vertexBuffer = vertexBuffer;
    this.indexCount = indexCount;
    this.program = program;
    this.mode = mode;
    this.indexType = indexType;
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

    indexBuffer.bind(GL20C.GL_ELEMENT_ARRAY_BUFFER);

    GL20C.glDrawElements(mode.getConstant(), indexCount, indexType.getConstant(), 0);

    program.removeAttributes();
  }

  @Override
  public void close() {
    indexBuffer.free();
    vertexBuffer.free();
  }
}
