package lemondead.game.engine.util.vector;

import lemondead.game.engine.util.MathUtil;

public interface Vec3 {
  Vec3 ZERO = of(0, 0, 0);

  double x();

  double y();

  double z();

  static OfDouble of(Vec2 vec2, double z) {
    return of(vec2.x(), vec2.y(), z);
  }

  static OfFloat of(float x, float y, float z) {
    return new OfFloat(x, y, z);
  }

  static OfDouble of(double x, double y, double z) {
    return new OfDouble(x, y, z);
  }

  default Vec3 add(double x, double y, double z) {
    return new OfDouble(x() + x, y() + y, z() + z);
  }

  default Vec3 add(Vec3 vec3) {
    return new OfDouble(x() + vec3.x(), y() + vec3.y(), z() + vec3.z());
  }

  default Vec3 subtract(double x, double y, double z) {
    return new OfDouble(x() - x, y() - y, z() - z);
  }

  default Vec3 subtract(Vec3 vec3) {
    return new OfDouble(x() - vec3.x(), y() - vec3.y(), z() - vec3.z());
  }

  default double dot(Vec3 vec3) {
    return x() * vec3.x() + y() * vec3.y() + z() * vec3.z();
  }

  default double length() {
    return Math.sqrt(MathUtil.sq(x()) + MathUtil.sq(y()) + MathUtil.sq(z()));
  }

  default double lengthSq() {
    return MathUtil.sq(x()) + MathUtil.sq(y()) + MathUtil.sq(z());
  }

  default Vec3 cross(Vec3 cross) {
    return new OfDouble(y() * cross.z() - z() * cross.y(),
                        z() * cross.x() - x() * cross.z(),
                        x() * cross.y() - y() * cross.x());
  }

  default Vec3 normalize() {
    double length = length();
    return length == 0 ? ZERO : new OfDouble(x() / length, y() / length, z() / length);
  }

  default double distanceToSq(Vec3 vec3) {
    return MathUtil.sq(x() - vec3.x()) + MathUtil.sq(y() - vec3.y()) + MathUtil.sq(z() - vec3.z());
  }

  default double distanceTo(Vec3 vec3) {
    return Math.sqrt(distanceToSq(vec3));
  }

  default Vec3 scale(double scale) {
    return new OfDouble(x() * scale, y() * scale, z() * scale);
  }

  OfFloat toFloat();

  OfDouble toDouble();

  final class OfDouble implements Vec3 {
    public final double x, y, z;

    private OfDouble(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
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
    public OfFloat toFloat() {
      return new OfFloat((float) x, (float) y, (float) z);
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
      return Double.compare(ofDouble.z, z) == 0;
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
      return result;
    }

    @Override
    public String toString() {
      return "OfDouble{" +
             "x=" + x +
             ", y=" + y +
             ", z=" + z +
             '}';
    }
  }

  final class OfFloat implements Vec3 {
    public final float x, y, z;

    private OfFloat(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
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
    public OfFloat toFloat() {
      return this;
    }

    @Override
    public OfDouble toDouble() {
      return new OfDouble(x, y, z);
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
      return Float.compare(ofFloat.z, z) == 0;
    }

    @Override
    public int hashCode() {
      int result = (x != 0.0f ? Float.floatToIntBits(x) : 0);
      result = 31 * result + (y != 0.0f ? Float.floatToIntBits(y) : 0);
      result = 31 * result + (z != 0.0f ? Float.floatToIntBits(z) : 0);
      return result;
    }

    @Override
    public String toString() {
      return "OfFloat{" +
             "x=" + x +
             ", y=" + y +
             ", z=" + z +
             '}';
    }
  }
}
