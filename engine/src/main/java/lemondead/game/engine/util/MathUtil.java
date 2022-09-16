package lemondead.game.engine.util;

public class MathUtil {
  public static double clamp(double min, double value, double max) {
    return value > max ? max : Math.max(value, min);
  }

  public static float clamp(float min, float value, float max) {
    return value > max ? max : Math.max(value, min);
  }

  public static int clamp(int min, int value, int max) {
    return value > max ? max : Math.max(value, min);
  }

  public static double lerp(double start, double t, double end) {
    return (end - start) * t + start;
  }

  public static float lerp(float start, float t, float end) {
    return (end - start) * t + start;
  }

  public static int max(int a, int b, int c) {
    return a > b ? Math.max(a, c) : Math.max(b, c);
  }

  public static int min(int a, int b, int c) {
    return a < b ? Math.min(a, c) : Math.min(b, c);
  }

  public static double max(double a, double b, double c) {
    return a > b ? Math.max(a, c) : Math.max(b, c);
  }

  public static double min(double a, double b, double c) {
    return a < b ? Math.min(a, c) : Math.min(b, c);
  }

  public static int max(int a, int b, int c, int d) {
    return Math.max(a, max(b, c, d));
  }

  public static int min(int a, int b, int c, int d) {
    return Math.min(a, min(b, c, d));
  }

  public static double max(double a, double b, double c, double d) {
    return Math.max(a, max(b, c, d));
  }

  public static double min(double a, double b, double c, double d) {
    return Math.min(a, min(b, c, d));
  }

  public static double sq(double d) {
    return d * d;
  }

  public static int ceil(double f) {
    int i = (int) f;
    if (i < f) {
      return i + 1;
    } else {
      return i;
    }
  }

  public static double roundTo(double a, double b) {
    double d = a / b;
    double d1 = ((int) d) * b;
    double remain = a - d1;
    return remain * 2 > b ? d1 + b : d1;
  }

  public static double ceilTo(double a, double b) {
    double d = a / b;
    double d1 = ((int) d) * b;
    double remain = a - d1;
    return remain > 0 ? d1 + b : d1;
  }

  public static int ceilTo(int a, int b) {
    return ceilDiv(a, b) * b;
  }

  public static double floorTo(double a, double b) {
    return Math.floor(a / b) * b;
  }

  public static int ceilDiv(int a, int b) {
    return (int) ((-1L + a + b) / b);
  }

  public static double smoothStep(double x) {
    return x >= 1 ? 1 : x <= 0 ? 0 : 3 * x * x - 2 * x * x * x;
  }

  public static double smootherStep(double x) {
    return x <= 0 ? 0 : x >= 1 ? 1 : 6 * x * x * x * x * x - 15 * x * x * x * x + 10 * x * x * x;
  }
}
