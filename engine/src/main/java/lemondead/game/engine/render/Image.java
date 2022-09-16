package lemondead.game.engine.render;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class Image {
  private final ByteBuffer buffer;
  private final int width;
  private final int height;
  private final int channels;
  private Runnable free;

  public Image(int width, int height, int channels) {
    this.width = width;
    this.height = height;
    this.channels = channels;
    buffer = MemoryUtil.memAlloc(width * height * channels);
    free = () -> MemoryUtil.memFree(buffer);
  }

  public Image(String location) {
    this(location, 0);
  }

  public Image(String location, int targetChannels) {
    ByteBuffer imageData = null;
    try (InputStream stream = ClassLoader.getSystemResourceAsStream(location); MemoryStack mem = MemoryStack.stackPush()) {
      if (stream == null) {
        throw new IllegalStateException("Could not find file: " + location);
      }
      imageData = MemoryUtil.memAlloc(stream.available());
      int i;
      int read = 0;
      while ((i = stream.read()) != -1) {
        imageData.put((byte) i);
        read++;
      }
      imageData.limit(read);
      imageData.position(0);
      IntBuffer xSize = mem.mallocInt(1);
      IntBuffer ySize = mem.mallocInt(1);
      IntBuffer channels = mem.mallocInt(1);
      buffer = STBImage.stbi_load_from_memory(imageData, xSize, ySize, channels, targetChannels);
      if (buffer == null) {
        throw new IllegalStateException("Could not load image " + location + "\n" + STBImage.stbi_failure_reason());
      }
      width = xSize.get(0);
      height = ySize.get(0);
      this.channels = channels.get(0);
      free = () -> STBImage.stbi_image_free(buffer);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    } finally {
      if (imageData != null) {
        MemoryUtil.memFree(imageData);
      }
    }
  }

  public ByteBuffer getBuffer() {
    return buffer;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int getChannels() {
    return channels;
  }

  public int getColor(int u, int v) {
    if (u >= width || u < 0 || v >= height || v < 0) {
      throw new IllegalArgumentException("Coordinates exceed image size.");
    }
    switch (channels) {
      case STBImage.STBI_grey: {
        int gray = Byte.toUnsignedInt(buffer.get(v * width + u));
        return ByteColor.toInt(gray, gray, gray, 255);
      }
      case STBImage.STBI_grey_alpha: {
        int index = (v * width + u) * 2;
        int gray = Byte.toUnsignedInt(buffer.get(index));
        int alpha = Byte.toUnsignedInt(buffer.get(index + 1));
        return ByteColor.toInt(gray, gray, gray, alpha);
      }
      case STBImage.STBI_rgb: {
        int index = (v * width + u) * 3;
        int r = Byte.toUnsignedInt(buffer.get(index));
        int g = Byte.toUnsignedInt(buffer.get(index + 1));
        int b = Byte.toUnsignedInt(buffer.get(index + 2));
        return ByteColor.toInt(r, g, b, 255);
      }
      case STBImage.STBI_rgb_alpha: {
        int index = (v * width + u) * 4;
        int r = Byte.toUnsignedInt(buffer.get(index));
        int g = Byte.toUnsignedInt(buffer.get(index + 1));
        int b = Byte.toUnsignedInt(buffer.get(index + 2));
        int a = Byte.toUnsignedInt(buffer.get(index + 3));
        return ByteColor.toInt(r, g, b, a);
      }
    }
    return 0;
  }

  public void setColor(int u, int v, int color) {
    if (u >= width || u < 0 || v >= height || v < 0) {
      throw new IllegalArgumentException("Coordinates exceed image size.");
    }

    switch (channels) {
      case STBImage.STBI_grey: {
        int index = v * width + u;
        int grey = (ByteColor.getRed(color) + ByteColor.getGreen(color) + ByteColor.getBlue(color)) / 3;
        buffer.put(index, (byte) grey);
      }
      break;
      case STBImage.STBI_grey_alpha: {
        int index = (v * width + u) * 2;
        int grey = (ByteColor.getRed(color) + ByteColor.getGreen(color) + ByteColor.getBlue(color)) / 3;
        buffer.put(index, (byte) grey);
        buffer.put(index + 1, (byte) ByteColor.getAlpha(color));
      }
      break;
      case STBImage.STBI_rgb: {
        int index = (v * width + u) * 3;
        buffer.put(index, (byte) ByteColor.getRed(color));
        buffer.put(index + 1, (byte) ByteColor.getGreen(color));
        buffer.put(index + 2, (byte) ByteColor.getBlue(color));
      }
      break;
      case STBImage.STBI_rgb_alpha: {
        int index = (v * width + u) * 4;
        buffer.put(index, (byte) ByteColor.getRed(color));
        buffer.put(index + 1, (byte) ByteColor.getGreen(color));
        buffer.put(index + 2, (byte) ByteColor.getBlue(color));
        buffer.put(index + 3, (byte) ByteColor.getAlpha(color));
      }
      break;
    }
  }

  public void blit(Image target, int u, int v) {
    if (target.getChannels() == getChannels()) {
      int length = getWidth() * channels;
      byte[] bytes = new byte[length];
      for (int y = 0; y < height; y++) {
        int srcIndex = y * width * channels;
        buffer.position(srcIndex);
        buffer.get(bytes, 0, length);
        int destIndex = ((v + y) * target.getWidth() + u) * channels;
        target.getBuffer().position(destIndex);
        target.getBuffer().put(bytes, 0, length);
      }
      buffer.position(0);
      target.getBuffer().position(0);
    } else {
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          target.setColor(u + x, v + y, getColor(x, y));
        }
      }
    }
  }

  public void free() {
    if (free != null) {
      free.run();
      free = null;
    }
  }

  @Override
  protected void finalize() {
    free();
  }
}
