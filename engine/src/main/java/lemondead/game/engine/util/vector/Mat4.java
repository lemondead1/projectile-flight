package lemondead.game.engine.util.vector;

import java.nio.FloatBuffer;
import java.util.Optional;

import static java.lang.Math.*;

public interface Mat4 {
  OfDouble IDENTITY = new OfDouble(1, 0, 0, 0,
                                   0, 1, 0, 0,
                                   0, 0, 1, 0,
                                   0, 0, 0, 1);


  static OfDouble of(double m11, double m12, double m13, double m14,
                     double m21, double m22, double m23, double m24,
                     double m31, double m32, double m33, double m34,
                     double m41, double m42, double m43, double m44) {
    return new OfDouble(m11, m12, m13, m14,
                        m21, m22, m23, m24,
                        m31, m32, m33, m34,
                        m41, m42, m43, m44);
  }

  static OfFloat of(float m11, float m12, float m13, float m14,
                    float m21, float m22, float m23, float m24,
                    float m31, float m32, float m33, float m34,
                    float m41, float m42, float m43, float m44) {
    return new OfFloat(m11, m12, m13, m14,
                       m21, m22, m23, m24,
                       m31, m32, m33, m34,
                       m41, m42, m43, m44);
  }

  static OfDouble scale(Vec4 factor) {
    return scale(factor.x(), factor.y(), factor.z(), factor.w());
  }

  static OfDouble scale(double x, double y, double z, double w) {
    return new OfDouble(x, 0, 0, 0,
                        0, y, 0, 0,
                        0, 0, z, 0,
                        0, 0, 0, w);
  }

  static OfDouble rotate(Vec3 rotation) {
    return rotate(rotation.x(), rotation.y(), rotation.z());
  }

  static OfDouble rotate(double x, double y, double z) {
    double sinX = sin(x);
    double cosX = cos(x);
    double sinY = sin(y);
    double cosY = cos(y);
    double sinZ = sin(z);
    double cosZ = cos(z);
    return new OfDouble(cosZ * cosY, cosZ * sinY * sinX - sinZ * cosX, cosZ * sinY * cosX + sinZ * sinX, 0,
                        sinZ * cosY, sinZ * sinY * sinX + cosZ * cosX, sinZ * sinY * cosX - cosZ * sinX, 0,
                        -sinY, cosY * sinX, cosY * cosX, 0,
                        0, 0, 0, 1);
  }

  static OfDouble rotate(double angle, Vec3 v) {
    double cos = cos(angle);
    double sin = sin(angle);

    return new OfDouble(
        cos + v.x() * v.x() * (1 - cos), v.x() * v.y() * (1 - cos) - v.z() * sin, v.x() * v.z() * (1 - cos) + v.y() * sin, 0,
        v.y() * v.x() * (1 - cos) + v.z() * sin, cos + v.y() * v.y() * (1 - cos), v.y() * v.z() * (1 - cos) - v.x() * sin, 0,
        v.z() * v.x() * (1 - cos) - v.y() * sin, v.z() * v.y() * (1 - cos) + v.x() * sin, cos + v.z() * v.z() * (1 - cos), 0,
        0, 0, 0, 1);
  }

  static OfDouble translate(Vec3 vec) {
    return translate(vec.x(), vec.y(), vec.z());
  }

  static OfDouble translate(double x, double y, double z) {
    return new OfDouble(1, 0, 0, x,
                        0, 1, 0, y,
                        0, 0, 1, z,
                        0, 0, 0, 1);
  }

  static OfDouble view(Vec3 eye, Vec3 target, Vec3 up) {
    Vec3 zAxis = eye.subtract(target).normalize();
    Vec3 xAxis = up.cross(zAxis).normalize();
    Vec3 yAxis = zAxis.cross(xAxis);

    return new OfDouble(xAxis.x(), xAxis.y(), xAxis.z(), -xAxis.dot(eye),
                        yAxis.x(), yAxis.y(), yAxis.z(), -yAxis.dot(eye),
                        zAxis.x(), zAxis.y(), zAxis.z(), -zAxis.dot(eye),
                        0, 0, 0, 1);
  }

  static OfDouble view(Vec3 eye, double pitch, double yaw, double roll) {
    double sinX = sin(-pitch);
    double cosX = cos(-pitch);
    double sinY = sin(-yaw);
    double cosY = cos(-yaw);
    double sinZ = sin(-roll);
    double cosZ = cos(-roll);
    return new OfDouble(cosZ * cosY, cosZ * sinY * sinX - sinZ * cosX, cosZ * sinY * cosX + sinZ * sinX, -eye.x(),
                        sinZ * cosY, sinZ * sinY * sinX + cosZ * cosX, sinZ * sinY * cosX - cosZ * sinX, -eye.y(),
                        -sinY, cosY * sinX, cosY * cosX, -eye.z(),
                        0, 0, 0, 1);
  }

  static OfDouble perspective(double fov, double aspect, double zNear, double zFar) {
    double f1 = 1 / tan(fov / 2);
    double f2 = zNear - zFar;

    return new OfDouble(f1 / aspect, 0, 0, 0,
                        0, f1, 0, 0,
                        0, 0, (zFar + zNear) / f2, (2 * zFar * zNear) / f2,
                        0, 0, -1, 0);
  }

  static OfDouble isometric(double left, double right, double top, double bottom, double zNear, double zFar) {
    return new OfDouble(2 / (right - left), 0, 0, (left + right) / (left - right),
                        0, 2 / (top - bottom), 0, (bottom + top) / (bottom - top),
                        0, 0, -2 / (zFar - zNear), (zNear + zFar) / (zNear - zFar),
                        0, 0, 0, 1);
  }

  static OfDouble isometric(double width, double height, double zNear, double zFar) {
    return new OfDouble(1 / width, 0, 0, 0,
                        0, 1 / height, 0, 0,
                        0, 0, -1 / (zFar - zNear), 0,
                        0, 0, 0, 1);
  }

  default Mat4 multiply(Mat4 b) {
    double a11 = get11() * b.get11() + get12() * b.get21() + get13() * b.get31() + get14() * b.get41();
    double a12 = get11() * b.get12() + get12() * b.get22() + get13() * b.get32() + get14() * b.get42();
    double a13 = get11() * b.get13() + get12() * b.get23() + get13() * b.get33() + get14() * b.get43();
    double a14 = get11() * b.get14() + get12() * b.get24() + get13() * b.get34() + get14() * b.get44();

    double a21 = get21() * b.get11() + get22() * b.get21() + get23() * b.get31() + get24() * b.get41();
    double a22 = get21() * b.get12() + get22() * b.get22() + get23() * b.get32() + get24() * b.get42();
    double a23 = get21() * b.get13() + get22() * b.get23() + get23() * b.get33() + get24() * b.get43();
    double a24 = get21() * b.get14() + get22() * b.get24() + get23() * b.get34() + get24() * b.get44();

    double a31 = get31() * b.get11() + get32() * b.get21() + get33() * b.get31() + get34() * b.get41();
    double a32 = get31() * b.get12() + get32() * b.get22() + get33() * b.get32() + get34() * b.get42();
    double a33 = get31() * b.get13() + get32() * b.get23() + get33() * b.get33() + get34() * b.get43();
    double a34 = get31() * b.get14() + get32() * b.get24() + get33() * b.get34() + get34() * b.get44();

    double a41 = get41() * b.get11() + get42() * b.get21() + get43() * b.get31() + get44() * b.get41();
    double a42 = get41() * b.get12() + get42() * b.get22() + get43() * b.get32() + get44() * b.get42();
    double a43 = get41() * b.get13() + get42() * b.get23() + get43() * b.get33() + get44() * b.get43();
    double a44 = get41() * b.get14() + get42() * b.get24() + get43() * b.get34() + get44() * b.get44();

    return new OfDouble(a11, a12, a13, a14,
                        a21, a22, a23, a24,
                        a31, a32, a33, a34,
                        a41, a42, a43, a44);
  }

  default Optional<Vec4> solve(Vec4 b) {
    double a11 = get11();
    double a12 = get12();
    double a13 = get13();
    double a14 = get14();
    double a15 = b.x();

    double a21 = get21();
    double a22 = get22();
    double a23 = get23();
    double a24 = get24();
    double a25 = b.y();

    double a31 = get31();
    double a32 = get32();
    double a33 = get33();
    double a34 = get34();
    double a35 = b.z();

    double a41 = get41();
    double a42 = get42();
    double a43 = get43();
    double a44 = get44();
    double a45 = b.w();

    double c;
    c = -a21 / a11;
    a21 += a11 * c;
    a22 += a12 * c;
    a23 += a13 * c;
    a24 += a14 * c;
    a25 += a15 * c;
    c = -a31 / a11;
    a31 += a11 * c;
    a32 += a12 * c;
    a33 += a13 * c;
    a34 += a14 * c;
    a35 += a15 * c;
    c = -a41 / a11;
    a41 += a11 * c;
    a42 += a12 * c;
    a43 += a13 * c;
    a44 += a14 * c;
    a45 += a15 * c;
    c = -a32 / a22;
    a31 += a21 * c;
    a32 += a22 * c;
    a33 += a23 * c;
    a34 += a24 * c;
    a35 += a25 * c;
    c = -a42 / a22;
    a41 += a21 * c;
    a42 += a22 * c;
    a43 += a23 * c;
    a44 += a24 * c;
    a45 += a25 * c;
    c = -a43 / a33;
    a41 += a31 * c;
    a42 += a32 * c;
    a43 += a33 * c;
    a44 += a34 * c;
    a45 += a35 * c;

    if (nanOrNotZero(a21) ||
        nanOrNotZero(a31) || nanOrNotZero(a32) ||
        nanOrNotZero(a41) || nanOrNotZero(a42) || nanOrNotZero(a43)) {
      return Optional.empty();
    }

    c = -a34 / a44;
    a35 += a45 * c;
    c = -a24 / a44;
    a25 += a45 * c;
    a23 += a43 * c;
    c = -a14 / a44;
    a15 += a45 * c;
    a13 += a43 * c;
    a12 += a42 * c;
    a45 /= a44;
    c = -a23 / a33;
    a25 += a35 * c;
    c = -a13 / a33;
    a15 += a35 * c;
    a12 += a32 * c;
    a35 /= a33;
    c = -a12 / a22;
    a15 += a25 * c;
    a25 /= a22;
    a15 /= a11;

    return Optional.of(Vec4.of(a15, a25, a35, a45));
  }

  static boolean nanOrNotZero(double f) {
    return abs(f) > 1E-6 || Double.isNaN(f);
  }

  default Vec4 multiply(Vec4 v) {
    double x = v.x() * get11() + v.y() * get12() + v.z() * get13() + v.w() * get14();
    double y = v.x() * get21() + v.y() * get22() + v.z() * get23() + v.w() * get24();
    double z = v.x() * get31() + v.y() * get32() + v.z() * get33() + v.w() * get34();
    double w = v.x() * get41() + v.y() * get42() + v.z() * get43() + v.w() * get44();
    return Vec4.of(x, y, z, w);
  }

  double get11();

  double get12();

  double get13();

  double get14();

  double get21();

  double get22();

  double get23();

  double get24();

  double get31();

  double get32();

  double get33();

  double get34();

  double get41();

  double get42();

  double get43();

  double get44();

  OfDouble toDouble();

  OfFloat toFloat();

  default void writeToBufferColMaj(FloatBuffer buffer) {
    buffer.put((float) get11()).put((float) get21()).put((float) get31()).put((float) get41());
    buffer.put((float) get12()).put((float) get22()).put((float) get32()).put((float) get42());
    buffer.put((float) get13()).put((float) get23()).put((float) get33()).put((float) get43());
    buffer.put((float) get14()).put((float) get24()).put((float) get34()).put((float) get44());
  }

  final class OfDouble implements Mat4 {
    public final double m11, m12, m13, m14;
    public final double m21, m22, m23, m24;
    public final double m31, m32, m33, m34;
    public final double m41, m42, m43, m44;

    private OfDouble(double m11, double m12, double m13, double m14,
                     double m21, double m22, double m23, double m24,
                     double m31, double m32, double m33, double m34,
                     double m41, double m42, double m43, double m44) {
      this.m11 = m11;
      this.m12 = m12;
      this.m13 = m13;
      this.m14 = m14;
      this.m21 = m21;
      this.m22 = m22;
      this.m23 = m23;
      this.m24 = m24;
      this.m31 = m31;
      this.m32 = m32;
      this.m33 = m33;
      this.m34 = m34;
      this.m41 = m41;
      this.m42 = m42;
      this.m43 = m43;
      this.m44 = m44;
    }

    @Override
    public double get11() {
      return m11;
    }

    @Override
    public double get12() {
      return m12;
    }

    @Override
    public double get13() {
      return m13;
    }

    @Override
    public double get14() {
      return m14;
    }

    @Override
    public double get21() {
      return m21;
    }

    @Override
    public double get22() {
      return m22;
    }

    @Override
    public double get23() {
      return m23;
    }

    @Override
    public double get24() {
      return m24;
    }

    @Override
    public double get31() {
      return m31;
    }

    @Override
    public double get32() {
      return m32;
    }

    @Override
    public double get33() {
      return m33;
    }

    @Override
    public double get34() {
      return m34;
    }

    @Override
    public double get41() {
      return m41;
    }

    @Override
    public double get42() {
      return m42;
    }

    @Override
    public double get43() {
      return m43;
    }

    @Override
    public double get44() {
      return m44;
    }

    public double[] toArrayRowMaj() {
      return new double[] { m11, m12, m13, m14,
                            m21, m22, m23, m24,
                            m31, m32, m33, m34,
                            m41, m42, m43, m44 };
    }

    public double[] toArrayColMaj() {
      return new double[] { m11, m21, m31, m41,
                            m12, m22, m32, m42,
                            m13, m23, m33, m43,
                            m14, m24, m34, m44 };
    }

    @Override
    public OfDouble toDouble() {
      return this;
    }

    @Override
    public OfFloat toFloat() {
      return new OfFloat((float) m11, (float) m12, (float) m13, (float) m14,
                         (float) m21, (float) m22, (float) m23, (float) m24,
                         (float) m31, (float) m32, (float) m33, (float) m34,
                         (float) m41, (float) m42, (float) m43, (float) m44);
    }
  }

  final class OfFloat implements Mat4 {
    public final float m11, m12, m13, m14;
    public final float m21, m22, m23, m24;
    public final float m31, m32, m33, m34;
    public final float m41, m42, m43, m44;

    private OfFloat(float m11, float m12, float m13, float m14,
                    float m21, float m22, float m23, float m24,
                    float m31, float m32, float m33, float m34,
                    float m41, float m42, float m43, float m44) {
      this.m11 = m11;
      this.m12 = m12;
      this.m13 = m13;
      this.m14 = m14;
      this.m21 = m21;
      this.m22 = m22;
      this.m23 = m23;
      this.m24 = m24;
      this.m31 = m31;
      this.m32 = m32;
      this.m33 = m33;
      this.m34 = m34;
      this.m41 = m41;
      this.m42 = m42;
      this.m43 = m43;
      this.m44 = m44;
    }

    @Override
    public double get11() {
      return m11;
    }

    @Override
    public double get12() {
      return m12;
    }

    @Override
    public double get13() {
      return m13;
    }

    @Override
    public double get14() {
      return m14;
    }

    @Override
    public double get21() {
      return m21;
    }

    @Override
    public double get22() {
      return m22;
    }

    @Override
    public double get23() {
      return m23;
    }

    @Override
    public double get24() {
      return m24;
    }

    @Override
    public double get31() {
      return m31;
    }

    @Override
    public double get32() {
      return m32;
    }

    @Override
    public double get33() {
      return m33;
    }

    @Override
    public double get34() {
      return m34;
    }

    @Override
    public double get41() {
      return m41;
    }

    @Override
    public double get42() {
      return m42;
    }

    @Override
    public double get43() {
      return m43;
    }

    @Override
    public double get44() {
      return m44;
    }

    public float[] toArrayRowMaj() {
      return new float[] { m11, m12, m13, m14,
                           m21, m22, m23, m24,
                           m31, m32, m33, m34,
                           m41, m42, m43, m44 };
    }

    public float[] toArrayColMaj() {
      return new float[] { m11, m21, m31, m41,
                           m12, m22, m32, m42,
                           m13, m23, m33, m43,
                           m14, m24, m34, m44 };
    }

    @Override
    public OfFloat toFloat() {
      return this;
    }

    @Override
    public OfDouble toDouble() {
      return new OfDouble(m11, m12, m13, m14,
                          m21, m22, m23, m24,
                          m31, m32, m33, m34,
                          m41, m42, m43, m44);
    }
  }
}
