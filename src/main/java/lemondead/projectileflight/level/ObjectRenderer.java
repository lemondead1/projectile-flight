package lemondead.projectileflight.level;

import lemondead.game.engine.render.Camera;
import lemondead.game.engine.render.ogl.DrawPass;
import lemondead.game.engine.util.vector.Vec4;

public interface ObjectRenderer {
  void render(DrawPass pass, Camera camera, Vec4 color, double timeAfterUpdate);

  void renderFlat(DrawPass pass, Camera camera, Vec4 color);
}
