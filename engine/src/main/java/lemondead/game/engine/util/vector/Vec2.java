package lemondead.game.engine.util.vector;

import lemondead.game.engine.util.MathUtil;

public interface Vec2 {
  OfInt ZERO = of(0, 0);

  double x();

  double y();

  static OfDouble of(double x, double y) {
    return new OfDouble(x, y);
  }

  static OfInt of(int x, int y) {
    return new OfInt(x, y);
  }

  static OfDouble of(Vec2 vec2) {
    return of(vec2.x(), vec2.y());
  }

  default Vec2 add(double x, double y) {
    return new OfDouble(x() + x, y() + y);
  }

  default Vec2 add(Vec2 vec2) {
    return new OfDouble(x() + vec2.x(), y() + vec2.y());
  }

  default Vec2 subtract(double x, double y) {
    return new OfDouble(x() - x, y() - y);
  }

  default Vec2 subtract(Vec2 vec2) {
    return new OfDouble(x() - vec2.x(), y() - vec2.y());
  }

  default double dot(Vec2 vec2) {
    return x() * vec2.x() + y() * vec2.y();
  }

  default double length() {
    return Math.sqrt(MathUtil.sq(x()) + MathUtil.sq(y()));
  }

  default double lengthSq() {
    return MathUtil.sq(x()) + MathUtil.sq(y());
  }

  default Vec2 normalize() {
    double length = length();
    return length == 0 ? ZERO : new OfDouble(x() / length, y() / length);
  }

  default Vec2 scale(double scale) {
    return new OfDouble(x() * scale, y() * scale);
  }

  default double distanceToSq(Vec2 vec2) {
    return MathUtil.sq(x() - vec2.x()) + MathUtil.sq(y() - vec2.y());
  }

  default double distanceTo(Vec2 vec2) {
    return Math.sqrt(distanceToSq(vec2));
  }

  OfDouble toDouble();

  OfInt toInt();

  final class OfDouble implements Vec2 {
    public final double x, y;

    private OfDouble(double x, double y) {
      this.x = x;
      this.y = y;
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
    public OfDouble toDouble() {
      return this;
    }

    @Override
    public OfInt toInt() {
      return new OfInt((int) x, (int) y);
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
      return Double.compare(ofDouble.y, y) == 0;
    }

    @Override
    public int hashCode() {
      int result;
      long temp;
      temp = Double.doubleToLongBits(x);
      result = (int) (temp ^ (temp >>> 32));
      temp = Double.doubleToLongBits(y);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      return result;
    }

    @Override
    public String toString() {
      return "OfDouble{" +
             "x=" + x +
             ", y=" + y +
             '}';
    }
  }

  final class OfInt implements Vec2 {
    public final int x, y;

    private OfInt(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public double x() {
      return x;
    }

    @Override
    public double y() {
      return y;
    }

    public OfInt add(int x, int y) {
      return new OfInt(this.x + x, this.y + y);
    }

    public OfInt add(OfInt vec2) {
      return new OfInt(this.x + vec2.x, this.y + vec2.y);
    }

    public OfInt subtract(int x, int y) {
      return new OfInt(this.x - x, this.y - y);
    }

    public OfInt subtract(OfInt vec2) {
      return new OfInt(this.x - vec2.x, this.y - vec2.y);
    }

    @Override
    public OfDouble toDouble() {
      return new OfDouble(x, y);
    }

    @Override
    public OfInt toInt() {
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

      OfInt ofInt = (OfInt) o;

      if (x != ofInt.x) {
        return false;
      }
      return y == ofInt.y;
    }

    @Override
    public int hashCode() {
      int result = x;
      result = 31 * result + y;
      return result;
    }

    @Override
    public String toString() {
      return "OfInt{" +
             "x=" + x +
             ", y=" + y +
             '}';
    }
  }
}
