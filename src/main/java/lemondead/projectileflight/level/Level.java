package lemondead.projectileflight.level;

import lemondead.game.engine.render.Camera;
import lemondead.game.engine.util.BoundingRect;
import lemondead.game.engine.util.vector.Vec2;
import lemondead.projectileflight.level.LevelObject.Control;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Level {
  final List<LevelObject> objects = new ArrayList<>();
  LevelObject selected = null;
  Control dragging = null;

  public void draw(BoundingRect visible, List<ObjectRenderer> renderers) {
    for (LevelObject object : objects) {
      for (BoundingRect rect : object.getCullingRects()) {
        if (visible.intersects(rect)) {
          renderers.add(object.getRenderer());
          break;
        }
      }
    }
  }

  public void onMouseClick(Vec2 screenPos, Camera camera) {
    for (LevelObject object : objects) {
      for (Control control : object.getControls()) {
        if (control.onClick(screenPos, camera)) {
          return;
        }
      }
    }
  }

  public Optional<Control> onMouseDrag(Vec2 screenPos, Camera camera) {
    for (LevelObject object : objects) {
      for (Control control : object.getControls()) {
        if (control.startDragging(screenPos, camera)) {
          dragging = control;
          return Optional.of(control);
        }
      }
    }
    return Optional.empty();
  }

  public boolean addObject(LevelObject object, boolean correctPos) {
    if (correctPos) {
      Optional<Vec2> optional = getPosition(object.getPos(), object.getBoundingRect(), levelObject -> true);
      if (!optional.isPresent()) {
        return false;
      }
      object.setPos(optional.get());
    }
    objects.add(object);
    object.level = this;
    return true;
  }

  public void removeObject(LevelObject object) {
    object.unselect();
    objects.remove(object);
    object.level = null;
  }

  public LevelObject getSelected() {
    return selected;
  }

  private static final BoundingRect floorRect = new BoundingRect(-10E8, -10E8, 10E8, 0);

  public Stream<BoundingRect> intersectsObject(BoundingRect rect, Predicate<LevelObject> filter) {
    return Stream
        .concat(objects.stream().filter(filter).map(o -> o.getBoundingRect().translate(o.getPos())), Stream.of(floorRect))
        .filter(r -> r.intersects(rect));
  }

  public Optional<Vec2> getPosition(Vec2 startPosition, BoundingRect maxRect, Predicate<LevelObject> filter) {
    BestPos pos = new BestPos(startPosition);
    List<BoundingRect> allRects = new ArrayList<>(objects.size() + 1);
    allRects.add(floorRect);
    for (LevelObject object : objects) {
      if (filter.test(object)) {
        allRects.add(object.getBoundingRect().translate(object.getPos()));
      }
    }
    correctPos(startPosition, maxRect, new Stack<>(), pos, allRects, 0);
    return Optional.ofNullable(pos.currentBest);
  }

  private static final int maxDepth = 8;

  private static final double maxDistanceSq = 2000 * 2000;

  private void correctPos(Vec2 currentPos, BoundingRect maxRect, Stack<BoundingRect> prevRects, BestPos bestPos, List<BoundingRect> allRects,
                          int depth) {
    BoundingRect translated = maxRect.translate(currentPos);
    Optional<BoundingRect> intersecting = allRects.stream().filter(r -> r.intersects(translated)).findAny();
    if (intersecting.isPresent()) {
      if (bestPos.foundPosisions >= bestPos.maxFoundPosisions || bestPos.isWorse(currentPos) || depth > maxDepth) {
        return;
      }
      BoundingRect rect = intersecting.get();
      if (prevRects.contains(rect)) {
        return;
      }
      prevRects.push(rect);
      correctPos(Vec2.of(currentPos.x(), rect.maxY - maxRect.minY), maxRect, prevRects, bestPos, allRects, depth + 1); //Top
      correctPos(Vec2.of(rect.minX - maxRect.maxX, currentPos.y()), maxRect, prevRects, bestPos, allRects, depth + 1); //Left
      correctPos(Vec2.of(rect.maxX - maxRect.minX, currentPos.y()), maxRect, prevRects, bestPos, allRects, depth + 1); //Right
      correctPos(Vec2.of(currentPos.x(), rect.minY - maxRect.maxY), maxRect, prevRects, bestPos, allRects, depth + 1); //Bottom
      prevRects.pop();
    } else {
      bestPos.replaceIfBetter(currentPos);
    }
  }

  private static class BestPos {
    private final Vec2 startPos;
    private Vec2 currentBest;
    private double currentDisSq;
    private final int maxFoundPosisions = 8;
    private int foundPosisions = 0;

    private BestPos(Vec2 startPos) {
      this.startPos = startPos;
    }

    public void replaceIfBetter(Vec2 vec2) {
      double dis = vec2.distanceToSq(startPos);
      if (currentBest == null || dis < currentDisSq) {
        currentBest = vec2;
        currentDisSq = dis;
      }
      foundPosisions++;
    }

    public boolean isWorse(Vec2 vec2) {
      double dis = vec2.distanceToSq(startPos);
      return dis > maxDistanceSq || (currentBest != null && dis > currentDisSq);
    }
  }

  public void reset() {
    objects.forEach(LevelObject::reset);
  }

  public void collectBodies(BiFunction<BodyDef, FixtureDef, Body> function) {
    objects.forEach(o -> o.initBodies(function));
  }
}
