package lemondead.game.engine.util.vector;

import lemondead.game.engine.util.MathUtil;

public interface Vec4 {
  Vec4 ZERO = of(0, 0, 0, 0);

  double x();

  double y();

  double z();

  double w();

  static OfDouble of(Vec3 vec3, double w) {
    return of(vec3.x(), vec3.y(), vec3.z(), w);
  }

  static OfFloat of(float x, float y, float z, float w) {
    return new OfFloat(x, y, z, w);
  }

  static OfDouble of(double x, double y, double z, double w) {
    return new OfDouble(x, y, z, w);
  }

  default Vec4 multiply(Vec4 vec4) {
    return Vec4.of(x() * vec4.x(), y() * vec4.y(), z() * vec4.z(), w() * vec4.w());
  }

  default Vec4 add(double x, double y, double z, double w) {
    return new OfDouble(x() + x, y() + y, z() + z, w() + w);
  }

  default Vec4 add(Vec4 vec4) {
    return new OfDouble(x() + vec4.x(), y() + vec4.y(), z() + vec4.z(), w() + vec4.w());
  }

  default Vec4 subtract(double x, double y, double z, double w) {
    return new OfDouble(x() - x, y() - y, z() - z, w() - w);
  }

  default Vec4 subtract(Vec4 vec4) {
    return new OfDouble(x() - vec4.x(), y() - vec4.y(), z() - vec4.z(), w() - vec4.w());
  }

  default double dot(Vec4 vec4) {
    return x() * vec4.x() + y() * vec4.y() + z() * vec4.z();
  }

  default double length() {
    return Math.sqrt(MathUtil.sq(x()) + MathUtil.sq(y()) + MathUtil.sq(z()) + MathUtil.sq(w()));
  }

  default double lengthSq() {
    return MathUtil.sq(x()) + MathUtil.sq(y()) + MathUtil.sq(z()) + MathUtil.sq(w());
  }

  default Vec4 normalize() {
    double length = length();
    return length == 0 ? ZERO : new OfDouble(x() / length, y() / length, z() / length, w() / length);
  }

  default double distanceToSq(Vec4 vec4) {
    return MathUtil.sq(x() - vec4.x()) + MathUtil.sq(y() - vec4.y()) + MathUtil.sq(z() - vec4.z()) + MathUtil.sq(w() - vec4.w());
  }

  default double distanceTo(Vec4 vec4) {
    return Math.sqrt(distanceToSq(vec4));
  }

  OfFloat toFloat();

  OfDouble toDouble();

  final class OfDouble implements Vec4 {
    public final double x, y, z, w;

    private OfDouble(double x, double y, double z, double w) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
    }

    @Override
    public double x() {
      return x;
    }

    @Override
    public double y() {
      return y;
    }

    @Override
    public double z() {
      return z;
    }

    @Override
    public double w() {
      return w;
    }

    @Override
    public OfFloat toFloat() {
      return new OfFloat((float) x, (float) y, (float) z, (float) w);
    }

    @Override
    public OfDouble toDouble() {
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      OfDouble ofDouble = (OfDouble) o;

      if (Double.compare(ofDouble.x, x) != 0) {
        return false;
      }
      if (Double.compare(ofDouble.y, y) != 0) {
        return false;
      }
      if (Double.compare(ofDouble.z, z) != 0) {
        return false;
      }
      return Double.compare(ofDouble.w, w) == 0;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(x);
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(y);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(z);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(w);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    @Override
    public String toString() {
      return "OfDouble{" +
             "x=" + x +
             ", y=" + y +
             ", z=" + z +
             ", w=" + w +
             '}';
    }
  }

  final class OfFloat implements Vec4 {
    public final float x, y, z, w;

    private OfFloat(float x, float y, float z, float w) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
    }

    @Override
    public double x() {
      return x;
    }

    @Override
    public double y() {
      return y;
    }

    @Override
    public double z() {
      return z;
    }

    @Override
    public double w() {
      return w;
    }

    @Override
    public OfFloat toFloat() {
      return this;
    }

    @Override
    public OfDouble toDouble() {
      return new OfDouble(x, y, z, w);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      OfFloat ofFloat = (OfFloat) o;

      if (Float.compare(ofFloat.x, x) != 0) {
        return false;
      }
      if (Float.compare(ofFloat.y, y) != 0) {
        return false;
      }
      if (Float.compare(ofFloat.z, z) != 0) {
        return false;
      }
      return Float.compare(ofFloat.w, w) == 0;
    }

    @Override
    public int hashCode() {
      int result = (x != 0.0f ? Float.floatToIntBits(x) : 0);
      result = 31 * result + (y != 0.0f ? Float.floatToIntBits(y) : 0);
      result = 31 * result + (z != 0.0f ? Float.floatToIntBits(z) : 0);
      result = 31 * result + (w != 0.0f ? Float.floatToIntBits(w) : 0);
      return result;
    }

    @Override
    public String toString() {
      return "OfFloat{" +
             "x=" + x +
             ", y=" + y +
             ", z=" + z +
             ", w=" + w +
             '}';
    }
  }
}
