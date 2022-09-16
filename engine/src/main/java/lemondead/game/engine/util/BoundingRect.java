package lemondead.game.engine.util;

import lemondead.game.engine.util.vector.Vec2;

import java.util.Objects;

public final class BoundingRect {
  public final double minX;
  public final double minY;
  public final double maxX;
  public final double maxY;

  public BoundingRect(double minX, double minY, double maxX, double maxY) {
    this.minX = Math.min(minX, maxX);
    this.minY = Math.min(minY, maxY);
    this.maxX = Math.max(maxX, minX);
    this.maxY = Math.max(maxY, minY);
  }

  public static BoundingRect withCenter(double x, double y, double halfWidth, double halfHeight) {
    return new BoundingRect(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight);
  }

  public BoundingRect scale(double scale) {
    return new BoundingRect(minX * scale, minY * scale, maxX * scale, maxY * scale);
  }

  public BoundingRect translate(double x, double y) {
    return new BoundingRect(minX + x, minY + y, maxX + x, maxY + y);
  }

  public BoundingRect translate(Vec2 vec2f) {
    return translate(vec2f.x(), vec2f.y());
  }

  public boolean contains(double x, double y) {
    return x >= minX && x <= maxX && y >= minY && y <= maxY;
  }

  public boolean contains(Vec2 vec2d) {
    return contains(vec2d.x(), vec2d.y());
  }

  public boolean intersects(BoundingRect rect) {
    return maxX - rect.minX > 1E-7 && rect.maxX - minX > 1E-7 && maxY - rect.minY > 1E-7 && rect.maxY - minY > 1E-7;
  }

  public boolean contains(BoundingRect rectangle) {
    return rectangle.minX >= minX && rectangle.minY >= minY && rectangle.maxX <= maxX && rectangle.maxY <= maxY;
  }

  public double height() {
    return maxY - minY;
  }

  public double width() {
    return maxX - minX;
  }

  public BoundingRect extend(double value) {
    return new BoundingRect(minX - value, minY - value, maxX + value, maxY + value);
  }

  public Vec2 center() {
    return Vec2.of((maxX + minX) / 2, (maxY + minY) / 2);
  }

  public BoundingRect rotate(float originX, float originY, double angle) {
    double cos = Math.cos(angle);
    double sin = Math.sin(angle);
    double x1 = (this.minX - originX) * cos - (this.maxY - originY) * sin + originX;
    double x2 = (this.minX - originX) * cos - (this.minY - originY) * sin + originX;
    double x3 = (this.maxX - originX) * cos - (this.maxY - originY) * sin + originX;
    double x4 = (this.maxX - originX) * cos - (this.minY - originY) * sin + originX;
    double minX = MathUtil.min(x1, x2, x3, x4);
    double maxX = MathUtil.max(x1, x2, x3, x4);

    double y1 = (this.minX - originX) * sin + (this.maxY - originY) * cos + originY;
    double y2 = (this.minX - originX) * sin + (this.minY - originY) * cos + originY;
    double y3 = (this.maxX - originX) * sin + (this.maxY - originY) * cos + originY;
    double y4 = (this.maxX - originX) * sin + (this.minY - originY) * cos + originY;
    double minY = MathUtil.min(y1, y2, y3, y4);
    double maxY = MathUtil.max(y1, y2, y3, y4);

    return new BoundingRect((float) minX, (float) minY, (float) maxX, (float) maxY);
  }

  public BoundingRect unite(BoundingRect... rectangles) {
    double minX = this.minX;
    double minY = this.minY;
    double maxX = this.maxX;
    double maxY = this.maxY;

    for (BoundingRect rect : rectangles) {
      minX = Math.min(rect.minX, minX);
      minY = Math.min(rect.minY, minY);
      maxX = Math.max(rect.maxX, maxX);
      maxY = Math.max(rect.maxY, maxY);
    }

    return new BoundingRect(minX, minY, maxX, maxY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BoundingRect that = (BoundingRect) o;
    return Double.compare(that.minX, minX) == 0 && Double.compare(that.minY, minY) == 0 && Double
                                                                                               .compare(that.maxX, maxX) == 0 &&
           Double.compare(that.maxY, maxY) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hash(minX, minY, maxX, maxY);
  }

  @Override
  public String toString() {
    return "BoundingRect{" +
           "minX=" + minX +
           ", minY=" + minY +
           ", maxX=" + maxX +
           ", maxY=" + maxY +
           '}';
  }
}
