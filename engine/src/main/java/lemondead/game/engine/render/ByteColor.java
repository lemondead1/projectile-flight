package lemondead.game.engine.render;

public class ByteColor {
  private static final int mask = 255;
  private static final int greenShift = 8;
  private static final int blueShift = greenShift + 8;
  private static final int alphaShift = blueShift + 8;

  public static int getRed(int color) {
    return color & mask;
  }

  public static int getGreen(int color) {
    return (color >>> greenShift) & mask;
  }

  public static int getBlue(int color) {
    return (color >>> blueShift) & mask;
  }

  public static int getAlpha(int color) {
    return (color >> alphaShift) & mask;
  }

  public static int toInt(float r, float g, float b, float a) {
    return toInt((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
  }

  public static int toInt(int r, int g, int b, int a) {
    if (r > 255 || g > 255 || b > 255 || a > 255 || r < 0 || g < 0 || b < 0 | a < 0) {
      throw new IllegalArgumentException("Color exceeds unsigned byte range.");
    }
    int i = a;
    i = (i << 8) | b;
    i = (i << 8) | g;
    i = (i << 8) | r;
    return i;
  }

  public static int toRGBAInt(float r, float g, float b, float a) {
    return toRGBAInt((int) (r * 255), (int) (g * 255), (int) (b * 255), (int) (a * 255));
  }

  public static int toRGBAInt(int r, int g, int b, int a) {
    return toInt(a, b, g, r);
  }
}
