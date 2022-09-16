package lemondead.game.engine.render.obj;

import lemondead.game.engine.util.vector.Vec3;

public class Material {
  private static final Vec3 defaultColor = Vec3.of(1, 1, 1);

  private final String name;
  private Vec3 diffuseColor = defaultColor;
  private String diffuseTexture;
  private float density = 1;

  public Material(String name, Vec3 diffuseColor, String diffuseTexture, float density) {
    this.name = name;
    this.diffuseColor = diffuseColor;
    this.diffuseTexture = diffuseTexture;
    this.density = density;
  }

  public Material(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public float getDensity() {
    return density;
  }

  public void setDensity(float density) {
    this.density = density;
  }

  public Vec3 getDiffuseColor() {
    return diffuseColor;
  }

  public void setDiffuseColor(Vec3 diffuseColor) {
    this.diffuseColor = diffuseColor;
  }

  public String getDiffuseTexture() {
    return diffuseTexture;
  }

  public void setDiffuseTexture(String diffuseTexture) {
    this.diffuseTexture = diffuseTexture;
  }
}
