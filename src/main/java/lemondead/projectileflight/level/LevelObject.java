package lemondead.projectileflight.level;

import lemondead.game.engine.render.Camera;
import lemondead.game.engine.util.BoundingRect;
import lemondead.game.engine.util.vector.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;

import java.util.function.BiFunction;

public abstract class LevelObject {
  Level level;
  protected Vec2 pos = Vec2.ZERO;

  protected LevelObject() {
  }

  public void select() {
    if (level == null) {
      throw new IllegalStateException("Level has not been set yet.");
    }
    level.selected = this;
  }

  public void unselect() {
    if (level == null) {
      throw new IllegalStateException("Level has not been set yet.");
    }
    level.selected = null;
  }

  public boolean isSelected() {
    if (level == null) {
      throw new IllegalStateException("Level has not been set yet.");
    }
    return level.selected == this;
  }

  public Level getLevel() {
    return level;
  }

  public abstract void initBodies(BiFunction<BodyDef, FixtureDef, Body> function);

  protected abstract void reset();

  public abstract ObjectRenderer getRenderer();

  public Control[] getControls() {
    return new Control[0];
  }

  protected abstract BoundingRect getBoundingRect();

  public Vec2 getPos() {
    return pos;
  }

  public void setPos(Vec2 pos) {
    this.pos = pos;
  }

  public BoundingRect[] getCullingRects() {
    return new BoundingRect[] { getBoundingRect().translate(getPos()) };
  }

  public abstract class Control {
    protected Control() {
    }

    public abstract boolean isMouseOver(Vec2 screenPos, Camera camera);

    public boolean startDragging(Vec2 screenPos, Camera camera) {
      return false;
    }

    public boolean onClick(Vec2 screenPos, Camera camera) {
      return false;
    }

    public void drag(Vec2 screenStart, Vec2 screenNext, Camera camera) {

    }

    public void stopDragging() {
      level.dragging = null;
    }

    public boolean isDragging() {
      return level.dragging == Control.this;
    }
  }
}
