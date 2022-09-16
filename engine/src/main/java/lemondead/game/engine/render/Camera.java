package lemondead.game.engine.render;

import lemondead.game.engine.util.BoundingRect;
import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec2;
import lemondead.game.engine.util.vector.Vec3;
import lemondead.game.engine.util.vector.Vec4;

import static java.lang.Math.tan;

public final class Camera {
  private Vec3 pos;
  private final double nearZ;
  private final double farZ;
  private double fov;
  private double pitch, yaw, roll;
  private double flatten;
  private double aspect;
  private Vec2.OfInt screenSize;

  private Mat4 cachedViewProjectionMatrix;
  private Mat4 cachedScreenSpaceMatrix;

  public Camera(double fov, Vec3 pos, double nearZ, double farZ, double flatten, Vec2.OfInt screenSize) {
    this.pos = pos;
    this.fov = fov;
    this.nearZ = nearZ;
    this.farZ = farZ;
    this.flatten = flatten;
    this.screenSize = screenSize;
    aspect = screenSize.x() / screenSize.y();
    refreshMatrix();
    cachedScreenSpaceMatrix = Mat4.translate(-1, 1, 0).multiply(Mat4.scale(2 / screenSize.x(), -2 / screenSize.y(), 1, 1));
  }

  private void refreshMatrix() {
    Mat4 projection;

    double actualZ;

    if (flatten < 1) {
      double actualFoV = fov * (1f - flatten * 0.975f);
      double distanceMultiplier = tan(fov / 2) / tan(actualFoV / 2);
      actualZ = pos.z() * distanceMultiplier;
      projection = Mat4.perspective(actualFoV, aspect, nearZ * distanceMultiplier, farZ * distanceMultiplier);
    } else {
      double height = pos.z() * Math.tan(fov / 2);
      projection = Mat4.isometric(height * aspect, height, nearZ, farZ);
      actualZ = pos.z();
    }
    Mat4 view = Mat4.view(Vec3.of(pos.x(), pos.y(), actualZ), pitch, yaw, roll);
    cachedViewProjectionMatrix = projection.multiply(view);
  }

  public void setPos(Vec3 pos) {
    this.pos = pos;
    refreshMatrix();
  }

  public void translate(double x, double y, double z) {
    pos = pos.add(x, y, z);
    refreshMatrix();
  }

  public void setFov(double fov) {
    this.fov = fov;
    refreshMatrix();
  }

  public void setScreenSize(Vec2.OfInt screenSize) {
    this.screenSize = screenSize;
    aspect = screenSize.x() / screenSize.y();
    cachedScreenSpaceMatrix = Mat4.translate(-1, 1, 0).multiply(Mat4.scale(2 / screenSize.x(), -2 / screenSize.y(), 1, 1));
    refreshMatrix();
  }

  public void setFlatten(double flatten) {
    this.flatten = flatten;
    refreshMatrix();
  }

  public void setRotation(double pitch, double yaw, double roll) {
    this.pitch = pitch;
    this.yaw = yaw;
    this.roll = roll;
    refreshMatrix();
  }

  public Vec3 getPos() {
    return pos;
  }

  public double getPitch() {
    return pitch;
  }

  public double getYaw() {
    return yaw;
  }

  public double getRoll() {
    return roll;
  }

  public Mat4 getMatrix() {
    return cachedViewProjectionMatrix;
  }

  public Mat4 getScreenMatrix() {
    return cachedScreenSpaceMatrix;
  }

  public double getFrustumHeight(double flatten, double z) {
    if (flatten < 1) {
      double actualFoV = fov * (1f - flatten * 0.975f);
      double distanceMultiplier = tan(fov / 2) / tan(actualFoV / 2);
      return z * distanceMultiplier * Math.tan(actualFoV / 2) * 2;
    } else {
      return z * Math.tan(fov / 2) * 2;
    }
  }

  public double getFrustumWidth(double z) {
    return getFrustumHeight(flatten, z) * aspect;
  }

  public BoundingRect getFrustumBoundingRect(double z) {
    double height = getFrustumHeight(flatten, z);
    double width = height * aspect;
    return new BoundingRect(pos.x() - width / 2, pos.y() - height / 2, pos.x() + width / 2, pos.y() + height / 2);
  }

  public boolean isWithinFrustum(Vec3 vector) {
    Vec4 transformed = cachedViewProjectionMatrix.multiply(Vec4.of(vector, 1));
    double x = transformed.x() / transformed.w();
    double y = transformed.y() / transformed.w();
    double z = transformed.z() / transformed.w();
    return x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= -1 && z <= 1;
  }

  public Vec3 toNDC(Vec3 vector) {
    Vec4 transformed = cachedViewProjectionMatrix.multiply(Vec4.of(vector, 1));
    double x = transformed.x() / transformed.w();
    double y = transformed.y() / transformed.w();
    double z = transformed.z() / transformed.w();
    return Vec3.of(x, y, z);
  }

  public Vec2 toScreenSpace(Vec3 vector) {
    Vec4 transformed = cachedViewProjectionMatrix.multiply(Vec4.of(vector, 1));
    double x = (transformed.x() / transformed.w() + 1) * screenSize.x() / 2;
    double y = screenSize.y() - (transformed.y() / transformed.w() + 1) * screenSize.y() / 2;
    return Vec2.of(x, y);
  }

  public Vec2 fromScreenSpace(Vec2 pos) {
    Vec4 vec = cachedViewProjectionMatrix.multiply(Vec4.of(0, 0, 0, 1));
    double w = vec.w();
    double x1 = (pos.x() / screenSize.x() * 2 - 1) * w;
    double y1 = -(pos.y() / screenSize.y() * 2 - 1) * w;
    Vec4 ndc = cachedViewProjectionMatrix.solve(Vec4.of(x1, y1, vec.z(), vec.w())).orElse(Vec4.ZERO);
    return Vec2.of(ndc.x() / ndc.w(), ndc.y() / ndc.w());
  }
}
