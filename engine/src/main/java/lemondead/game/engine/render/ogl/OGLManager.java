package lemondead.game.engine.render.ogl;

import lemondead.game.engine.util.vector.Vec2;
import org.lwjgl.opengl.GL20C;

import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL20C.*;

public class OGLManager {
  public static final Logger RENDER_LOGGER = Logger.getLogger("Render logger");

  public static final OGLBufferAllocator STATIC_DRAW_ALLOCATOR = (size) -> {
    int oldBinding = GL20C.glGetInteger(GL20C.GL_ARRAY_BUFFER_BINDING);
    int location = GL20C.glGenBuffers();
    GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, location);
    GL20C.glBufferData(GL20C.GL_ARRAY_BUFFER, size, GL20C.GL_STATIC_DRAW);
    GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, oldBinding);
    return new OGLBuffer(size, location) {
      @Override
      public void free() {
        if (freed) {
          RENDER_LOGGER.warning("This buffer has already been freed.");
          return;
        }
        GL20C.glDeleteBuffers(bufferLocation);
        freed = true;
      }
    };
  };
  public static final OGLBufferAllocator STREAM_DRAW_ALLOCATOR = new OGLBufferAllocator() {
    private final int buffer1;
    private final int buffer2;
    private boolean buffer1Free = true;
    private boolean buffer2Free = true;

    {
      int oldBinding = GL20C.glGetInteger(GL20C.GL_ARRAY_BUFFER_BINDING);
      buffer1 = GL20C.glGenBuffers();
      GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, buffer1);
      GL20C.glBufferData(GL20C.GL_ARRAY_BUFFER, 1024, GL20C.GL_DYNAMIC_DRAW);
      buffer2 = GL20C.glGenBuffers();
      GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, buffer2);
      GL20C.glBufferData(GL20C.GL_ARRAY_BUFFER, 1024, GL20C.GL_DYNAMIC_DRAW);
      GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, oldBinding);
    }

    @Override
    public OGLBuffer create(int size) {
      if (size > 1024 || !(buffer1Free || buffer2Free)) {
        int oldBinding = GL20C.glGetInteger(GL20C.GL_ARRAY_BUFFER_BINDING);
        int location = GL20C.glGenBuffers();
        GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, location);
        GL20C.glBufferData(GL20C.GL_ARRAY_BUFFER, size, GL20C.GL_STATIC_DRAW);
        GL20C.glBindBuffer(GL20C.GL_ARRAY_BUFFER, oldBinding);
        return new OGLBuffer(size, location) {
          @Override
          public void free() {
            if (freed) {
              RENDER_LOGGER.warning("This buffer has already been freed.");
              return;
            }
            GL20C.glDeleteBuffers(bufferLocation);
            freed = true;
          }
        };
      }
      if (buffer1Free) {
        buffer1Free = false;
        return new OGLBuffer(1024, buffer1) {
          @Override
          public void free() {
            if (freed) {
              RENDER_LOGGER.warning("This buffer has already been freed.");
              return;
            }
            freed = true;
            buffer1Free = true;
          }
        };
      }
      buffer2Free = false;
      return new OGLBuffer(1024, buffer2) {
        @Override
        public void free() {
          if (freed) {
            RENDER_LOGGER.warning("This buffer has already been freed.");
            return;
          }
          freed = true;
          buffer2Free = true;
        }
      };
    }
  };

  public static void init() {
    onFramebufferSizeCallback(800, 800);
  }

  public static void onFramebufferSizeCallback(int width, int height) {
    glViewport(0, 0, width, height);
  }

  static int bindTexture(int texture) {
    int old = glGetInteger(GL_TEXTURE_BINDING_2D);
    glBindTexture(GL_TEXTURE_2D, texture);
    return old;
  }

  public static Vec2.OfInt getViewportSize() {
    int[] is = new int[4];
    glGetIntegerv(GL_VIEWPORT, is);
    return Vec2.of(is[2], is[3]);
  }

  public static void draw(Consumer<DrawPass> consumer) {
    glDepthMask(true);
    glEnable(GL_DEPTH_TEST);
    glDepthFunc(GL_LEQUAL);
    glColorMask(false, false, false, false);
    consumer.accept(DrawPass.DEPTH_ONLY);
    glColorMask(true, true, true, true);
    consumer.accept(DrawPass.SOLID);
    consumer.accept(DrawPass.LINES);
    glDepthMask(false);
    glDisable(GL_DEPTH_TEST);
    consumer.accept(DrawPass.TRANSPARENT);
    consumer.accept(DrawPass.OVERLAY);
    glDepthMask(true);
  }
}
