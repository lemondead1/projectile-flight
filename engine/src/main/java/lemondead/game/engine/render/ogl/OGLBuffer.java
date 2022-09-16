package lemondead.game.engine.render.ogl;

import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

import java.io.Closeable;
import java.nio.ByteBuffer;

public abstract class OGLBuffer {
  private final int size;
  protected final int bufferLocation;
  protected boolean freed;

  protected OGLBuffer(int size, int bufferLocation) {
    this.size = size;
    this.bufferLocation = bufferLocation;
  }

  private void checkFreed() {
    if (freed) {
      throw new IllegalStateException("This buffer section has already been freed.");
    }
  }

  public void bind(int target) {
    checkFreed();
    GL30C.glBindBuffer(target, bufferLocation);
  }

  public MappedBuffer map(int access) {
    checkFreed();
    int oldBinding = GL20C.glGetInteger(GL20C.GL_ARRAY_BUFFER_BINDING);
    bind(GL20C.GL_ARRAY_BUFFER);
    ByteBuffer byteBuffer = GL20C.glMapBuffer(GL20C.GL_ARRAY_BUFFER, access, size, null);
    if (byteBuffer == null) {
      throw new RuntimeException("Could not map buffer: " + GL20C.glGetError());
    }
    return new MappedBuffer(oldBinding, byteBuffer);
  }

  public int size() {
    return size;
  }

  public abstract void free();

  public static class MappedBuffer implements Closeable {
    private final int oldBinding;
    private final ByteBuffer buffer;

    public MappedBuffer(int oldBinding, ByteBuffer buffer) {
      this.oldBinding = oldBinding;
      this.buffer = buffer;
    }

    public ByteBuffer getBuffer() {
      return buffer;
    }

    @Override
    public void close() {
      GL20C.glUnmapBuffer(GL20C.GL_ARRAY_BUFFER);
      GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, oldBinding);
    }
  }
}
