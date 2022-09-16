package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.*;
import org.lwjgl.opengl.GL20C;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import static lemondead.game.engine.render.ogl.ValueType.UNSIGNED_INT;
import static lemondead.game.engine.render.ogl.ValueType.UNSIGNED_SHORT;

public final class IndexedBuilder implements BufferDataBuilder {
  private static final int defaultVertexCount = 1 << 6;
  private static final int defaultIndexCount = 1 << 7;

  private final Program program;

  private ByteBuffer data;
  private int[] indices;
  private int capacity;
  private int vertexCount;
  private int indexCount;
  private final RenderMode mode;

  public IndexedBuilder(RenderMode mode, Program program) {
    this(mode, program, defaultVertexCount, defaultIndexCount);
  }

  public IndexedBuilder(RenderMode mode, Program program, int initialVertexCount, int initialIndexCount) {
    this.program = program;
    capacity = initialVertexCount;
    this.mode = mode;
    data = ByteBuffer.allocate(program.getVertexFormat().getVertexByteSize() * capacity).order(ByteOrder.nativeOrder());
    indices = new int[initialIndexCount];
    Objects.requireNonNull(data);
  }

  private void ensureCapacity(int newVertexCount) {
    if (newVertexCount > capacity) {
      capacity = Math.max(newVertexCount, capacity * 3 / 2);
      ByteBuffer newData = ByteBuffer.allocate(program.getVertexFormat().getVertexByteSize() * capacity).order(ByteOrder.nativeOrder());
      data.rewind();
      newData.put(data.array());
      data = newData;
    }
  }

  private void ensureIndicesCapacity(int newVertexCount) {
    if (newVertexCount > indices.length) {
      int newCap = (int) Math.max(newVertexCount, indices.length * 1.5);
      indices = Arrays.copyOf(indices, newCap);
    }
  }

  private int getPosition(VertexFormat.VertexAttribute attrib) {
    ensureCapacity(vertexCount + 1);
    return program.getVertexFormat().getOffset(attrib) + vertexCount * program.getVertexFormat().getVertexByteSize();
  }

  public IndexedBuilder pos(float x, float y, float z) {
    data.position(getPosition(VertexFormat.VertexAttribute.POSITION));
    data.putFloat(x).putFloat(y).putFloat(z);
    return this;
  }

  public IndexedBuilder texPos(float u, float v) {
    data.position(getPosition(VertexFormat.VertexAttribute.TEXTURE_POS));
    final float f = (1 << 16) - 1;
    data.putShort((short) (u * f)).putShort((short) (v * f));
    return this;
  }

  public IndexedBuilder color(float r, float g, float b, float a) {
    data.position(getPosition(VertexFormat.VertexAttribute.COLOR));
    final float i = (1 << 8) - 1;
    data.put((byte) (r * i)).put((byte) (g * i)).put((byte) (b * i)).put((byte) (a * i));
    return this;
  }

  public void end() {
    vertexCount++;
  }

  public IndexedBuilder index(int index) {
    ensureIndicesCapacity(indexCount + 1);
    if (index < 0) {
      index = vertexCount + index;
      if (index < 0) {
        throw new IllegalArgumentException("Cannot specify negative index greater than vertex count.");
      }
    }
    if (index >= vertexCount) {
      throw new IllegalArgumentException("Cannot specify index greater or equal to vertex count");
    }
    indices[indexCount] = index;
    indexCount++;
    return this;
  }

  public RenderMode getMode() {
    return mode;
  }

  @Override
  public BufferRenderer upload(OGLBufferAllocator allocator, ModelTransformer transformer) {
    if (vertexCount == 0 || indexCount == 0) {
      return new BufferRenderer() {
        @Override
        public void draw(Consumer<UniformSetter> uniformSetter) {
        }

        @Override
        public void close() {
        }
      };
    }
    OGLBuffer vertexBuffer = allocator.create(vertexCount * program.getVertexFormat().getVertexByteSize());
    data.rewind();
    try (OGLBuffer.MappedBuffer mappedBuffer = vertexBuffer.map(GL20C.GL_WRITE_ONLY)) {
      ByteBuffer vertexData = mappedBuffer.getBuffer();
      float[] vertex = new float[program.getVertexFormat().getVertexValueCount()];
      for (int i = 0; i < vertexCount; i++) {
        int l = 0;
        for (VertexFormat.VertexAttribute attribute : program.getVertexFormat().getAttributes()) {
          switch (attribute) {
            case POSITION:
              vertex[l++] = data.getFloat();
              vertex[l++] = data.getFloat();
              vertex[l++] = data.getFloat();
              break;
            case TEXTURE_POS:
              vertex[l++] = data.getShort() / (float) (1 << 16 - 1);
              vertex[l++] = data.getShort() / (float) (1 << 16 - 1);
              break;
            case COLOR:
              vertex[l++] = data.get() / (float) (1 << 8 - 1);
              vertex[l++] = data.get() / (float) (1 << 8 - 1);
              vertex[l++] = data.get() / (float) (1 << 8 - 1);
              vertex[l++] = data.get() / (float) (1 << 8 - 1);
              break;
          }
        }
        transformer.transform(vertex, program.getVertexFormat());
        l = 0;
        for (VertexFormat.VertexAttribute attribute : program.getVertexFormat().getAttributes()) {
          switch (attribute) {
            case POSITION:
              vertexData.putFloat(vertex[l++]).putFloat(vertex[l++]).putFloat(vertex[l++]);
              break;
            case TEXTURE_POS:
              vertexData.putShort((short) (vertex[l++] * (1 << 16 - 1))).putShort((short) (vertex[l++] * (1 << 16 - 1)));
              break;
            case COLOR:
              vertexData.put((byte) (vertex[l++] * (float) (1 << 8 - 1))).put((byte) (vertex[l++] * (float) (1 << 8 - 1)))
                        .put((byte) (vertex[l++] * (float) (1 << 8 - 1))).put((byte) (vertex[l++] * (float) (1 << 8 - 1)));
              break;
          }
        }
      }
    }

    ValueType type = vertexCount < 1 << 16 ? UNSIGNED_SHORT : UNSIGNED_INT;
    OGLBuffer indexBuffer = allocator.create(type.getByteSize() * indexCount);
    try (OGLBuffer.MappedBuffer mappedBuffer = indexBuffer.map(GL20C.GL_WRITE_ONLY)) {
      ByteBuffer indexData = mappedBuffer.getBuffer();
      switch (type) {
        case UNSIGNED_SHORT:
          for (int i = 0; i < indexCount; i++) {
            indexData.putShort((short) indices[i]);
          }
          break;
        case UNSIGNED_INT:
          for (int i = 0; i < indexCount; i++) {
            indexData.putInt(indices[i]);
          }
          break;
      }
    }
    return new SimpleRenderer(vertexBuffer, indexBuffer, indexCount, type, program, mode);
  }
}
