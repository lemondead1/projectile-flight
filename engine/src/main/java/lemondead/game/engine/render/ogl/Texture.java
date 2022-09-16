package lemondead.game.engine.render.ogl;

import lemondead.game.engine.render.Image;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.opengl.GL11C.glTexImage2D;
import static org.lwjgl.opengl.GL20C.*;

public final class Texture {
  private final int location;
  public Texture() {
    location = glGenTextures();
    setParameter(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    setParameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    setParameter(GL_TEXTURE_WRAP_S, GL_REPEAT);
    setParameter(GL_TEXTURE_WRAP_T, GL_REPEAT);
  }

  public void activate(int textureUnit) {
    glActiveTexture(textureUnit);
    OGLManager.bindTexture(location);
  }

  public void bind() {
    OGLManager.bindTexture(location);
  }

  public void loadImage(Image image) {
    ByteBuffer buffer = image.getBuffer();
    buffer.rewind();
    int width = image.getWidth();
    int height = image.getHeight();
    int old = OGLManager.bindTexture(location);
    switch (image.getChannels()) {
      case STBImage.STBI_rgb:
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, buffer);
        break;
      case STBImage.STBI_rgb_alpha:
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        break;
      case STBImage.STBI_grey:
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, buffer);
        break;
    }
    OGLManager.bindTexture(old);
  }

  public void allocate(int internalFormat, int u, int v, int format, ValueType type) {
    int old = OGLManager.bindTexture(location);
    glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, u, v, 0, format, type.getConstant(), (ByteBuffer) null);
    OGLManager.bindTexture(old);
  }

  public void setParameter(int parameter, int value) {
    int old = OGLManager.bindTexture(location);
    glTexParameteri(GL_TEXTURE_2D, parameter, value);
    OGLManager.bindTexture(old);
  }

  public void delete() {
    GL20C.glDeleteTextures(location);
  }
}
