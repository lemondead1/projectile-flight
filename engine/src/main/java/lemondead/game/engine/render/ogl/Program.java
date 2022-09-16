package lemondead.game.engine.render.ogl;

import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec4;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL20C.*;

public final class Program {
  public static Program POSITION_COLOR;
  public static Program POSITION_COLOR_TEXTURE;
  public static Program TEXT;

  public static void init() {
    POSITION_COLOR_TEXTURE = new Builder()
        .loadVert("textured.vert")
        .loadFrag("textured.frag")
        .format(VertexFormat.POSITION_COLOR_TEXTURE)
        .uniforms(Uniform.TRANSFORM_MATRIX, Uniform.DIFFUSE_TEXTURE, Uniform.TINT)
        .build("textured");
    POSITION_COLOR = new Builder()
        .loadVert("simple.vert")
        .loadFrag("simple.frag")
        .format(VertexFormat.POSITION_COLOR)
        .uniforms(Uniform.TRANSFORM_MATRIX, Uniform.TINT)
        .build("simple");
    TEXT = new Builder()
        .loadVert("textured.vert")
        .loadFrag("text.frag")
        .format(VertexFormat.POSITION_COLOR_TEXTURE)
        .uniforms(Uniform.TRANSFORM_MATRIX, Uniform.DIFFUSE_TEXTURE, Uniform.TINT)
        .build("textured");
  }


  private final int programLocation;
  private final Uniform<?>[] uniforms;
  private final VertexFormat format;
  private final int[] uniformLocations;
  private final int[] attribLocations;

  public Program(int programLocation, Uniform<?>[] uniforms, VertexFormat format) {
    this.programLocation = programLocation;
    this.uniforms = uniforms;
    this.format = format;
    uniformLocations = new int[uniforms.length];
    for (int i = 0; i < uniforms.length; i++) {
      int location = GL20C.glGetUniformLocation(programLocation, uniforms[i].getName());
      int error = GL20C.glGetError();
      if (error != GL20C.GL_NO_ERROR) {
        throw new IllegalStateException("Opengl has thrown an error during uniform location " + error);
      }
      uniformLocations[i] = location;
    }
    attribLocations = new int[format.getAttributes().length];
    for (int i = 0; i < attribLocations.length; i++) {
      attribLocations[i] = GL20C.glGetAttribLocation(programLocation, format.getAttributes()[i].getAttribute());
    }
  }

  public Uniform<?>[] getUniforms() {
    return uniforms;
  }

  public VertexFormat getVertexFormat() {
    return format;
  }

  public int[] getUniformLocations() {
    return uniformLocations;
  }

  int getProgramLocation() {
    return programLocation;
  }

  public void use() {
    glUseProgram(getProgramLocation());
  }

  public void applyVertexAttributes() {
    int offset = 0;
    VertexFormat.VertexAttribute[] attributes = getVertexFormat().getAttributes();
    int vertexByteSize = getVertexFormat().getVertexByteSize();
    for (int i = 0; i < attributes.length; i++) {
      VertexFormat.VertexAttribute attribute = attributes[i];
      glEnableVertexAttribArray(attribLocations[i]);
      glVertexAttribPointer(attribLocations[i], attribute.getValueCount(), attribute.getValueType(), attribute.isNormalized(), vertexByteSize,
                            offset);
      offset += attribute.getByteSize();
    }
  }

  public void removeAttributes() {
    VertexFormat.VertexAttribute[] attributes = getVertexFormat().getAttributes();
    for (int i = 0; i < attributes.length; i++) {
      glDisableVertexAttribArray(attribLocations[i]);
    }
  }

  public static class Builder {
    private static final Uniform<?>[] uEmpty = { };

    private int vertexShader = 0;
    private int fragmentShader = 0;
    private VertexFormat format = null;
    private Uniform<?>[] uniforms = uEmpty;

    public Builder loadVert(String location) {
      if (vertexShader != 0) {
        throw new IllegalArgumentException("Vertex shader has already been set.");
      }
      vertexShader = ShaderUtils.loadVertexShader(location);
      return this;
    }

    public Builder loadFrag(String location) {
      if (fragmentShader != 0) {
        throw new IllegalArgumentException("Fragment shader has already been set.");
      }
      fragmentShader = ShaderUtils.loadFragmentShader(location);
      return this;
    }

    public Builder uniforms(Uniform<?>... uniforms) {
      this.uniforms = uniforms;
      return this;
    }

    public Builder format(VertexFormat format) {
      this.format = format;
      return this;
    }

    public Program build(String name) {
      if (vertexShader == 0) {
        throw new IllegalStateException("Vertex shader is not present.");
      }
      if (format == null) {
        throw new IllegalStateException("VertexFormat is not present.");
      }
      int[] shaders = IntStream.of(vertexShader, fragmentShader).filter(i -> i != 0).toArray();
      int program = ShaderUtils.createProgram(name, shaders);
      IntStream.of(shaders).forEach(GL20C::glDeleteShader);
      return new Program(program, uniforms, format);
    }
  }

  public abstract static class Uniform<T> {
    public static final Uniform<Mat4> TRANSFORM_MATRIX = new Uniform<Mat4>("transformMatrix", Mat4.class, Mat4.IDENTITY) {
      @Override
      public void set(int location, Mat4 value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
          FloatBuffer buffer = stack.mallocFloat(16);
          value.writeToBufferColMaj(buffer);
          buffer.rewind();
          GL20C.glUniformMatrix4fv(location, false, buffer);
        }
      }
    };
    public static final Uniform<Vec4> TINT = new Uniform<Vec4>("tint", Vec4.class, Vec4.of(1, 1, 1, 1)) {
      @Override
      public void set(int location, Vec4 value) {
        GL20C.glUniform4fv(location, new float[] { (float) value.x(), (float) value.y(), (float) value.z(), (float) value.w() });
      }
    };
    public static final Uniform<Integer> DIFFUSE_TEXTURE = new Uniform<Integer>("diffuseTexture", Integer.class, 1) {
      @Override
      public void set(int location, Integer value) {
        GL20C.glUniform1i(location, value);
      }
    };

    private final String name;
    private final Class<T> clazz;
    private final T defaultValue;

    private Uniform(String name, Class<T> clazz, T defaultValue) {
      this.name = name;
      this.clazz = clazz;
      this.defaultValue = defaultValue;
    }

    public String getName() {
      return name;
    }

    public Class<T> valueClazz() {
      return clazz;
    }

    public abstract void set(int location, T value);

    public T getDefault() {
      return defaultValue;
    }
  }

}
