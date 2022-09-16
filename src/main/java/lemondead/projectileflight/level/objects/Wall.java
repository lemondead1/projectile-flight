package lemondead.projectileflight.level.objects;

import lemondead.game.engine.render.Camera;
import lemondead.game.engine.render.SimpleModels;
import lemondead.game.engine.render.buffers.BufferRenderer;
import lemondead.game.engine.render.buffers.IndexedBuilder;
import lemondead.game.engine.render.buffers.ModelTransformer;
import lemondead.game.engine.render.buffers.SimpleBuilder;
import lemondead.game.engine.render.ogl.DrawPass;
import lemondead.game.engine.render.ogl.OGLManager;
import lemondead.game.engine.render.ogl.Program;
import lemondead.game.engine.render.ogl.RenderMode;
import lemondead.game.engine.util.BoundingRect;
import lemondead.game.engine.util.MathUtil;
import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec2;
import lemondead.game.engine.util.vector.Vec3;
import lemondead.game.engine.util.vector.Vec4;
import lemondead.projectileflight.KeyboardInputManager;
import lemondead.projectileflight.Main;
import lemondead.projectileflight.level.LevelObject;
import lemondead.projectileflight.level.ObjectRenderer;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;

import java.util.function.BiFunction;

public class Wall extends LevelObject {
  private static final float halfWidth = 0.25f;

  private static final BufferRenderer round = SimpleModels.getCircleModel(OGLManager.STATIC_DRAW_ALLOCATOR, 0, 0, 1, 16, 1, 1, 1, 1);
  private static final BufferRenderer solidCube;
  private static final BufferRenderer wireframeCube;

  static {
    solidCube = SimpleModels
        .getCuboid(new IndexedBuilder(RenderMode.SOLID, Program.POSITION_COLOR), -1, -2, -1, 2, 2, 2)
        .upload(OGLManager.STATIC_DRAW_ALLOCATOR, ModelTransformer.IDENTITY);
    wireframeCube = SimpleModels
        .getCuboid(new IndexedBuilder(RenderMode.LINES, Program.POSITION_COLOR), -1, -2, -1, 2, 2, 2)
        .upload(OGLManager.STATIC_DRAW_ALLOCATOR, ModelTransformer.IDENTITY);
  }

  @SuppressWarnings("EmptyMethod")
  public static void init() {
    //Static initializer trigger
  }

  private double extent = 3;
  private double angle = (float) (Math.PI / 3);
  private final WallRenderer renderer = new WallRenderer();

  private final Control[] controls = {
      new Control() {
        @Override
        public boolean isMouseOver(Vec2 screenPos, Camera camera) {
          Vec2 offset = Vec2.of(Math.cos(angle) * extent, Math.sin(angle) * extent);
          Vec2 posScreen = camera.toScreenSpace(Vec3.of(pos.add(offset), 0));
          return posScreen.subtract(screenPos.x(), screenPos.y()).lengthSq() < 144;
        }

        @Override
        public boolean startDragging(Vec2 screenPos, Camera camera) {
          return isSelected() && isMouseOver(screenPos, camera);
        }

        @Override
        public void drag(Vec2 screenStart, Vec2 screenNext, Camera camera) {
          Vec2 offset = Vec2.of(Math.cos(angle) * extent, Math.sin(angle) * extent);
          Vec2 otherHandle = pos.subtract(offset);
          Vec2 nextWorld = camera.fromScreenSpace(screenNext);

          if (Main.GAME_INSTANCE.keyboard.isPressed(KeyboardInputManager.SNAP)) {
            double snapDistance = Main.GAME_INSTANCE.getSnapDistance();
            nextWorld = Vec2.of(MathUtil.roundTo(nextWorld.x(), snapDistance), MathUtil.roundTo(nextWorld.y(), snapDistance));
          }

          Vec2 diff = nextWorld.subtract(otherHandle);

          double extend1 = diff.length() / 2;
          double angle1 = Math.atan2(diff.y(), diff.x());
          Vec2 newPos = otherHandle.add(diff.scale(0.5));

          BoundingRect rect = new BoundingRect(-extend1, -2 * halfWidth, extend1, 0).rotate(0, 0, angle1).translate(newPos);

          if (getLevel().intersectsObject(rect, o -> !(o instanceof Wall)).findAny().isPresent()) {
            return;
          }

          Wall.this.pos = newPos;
          Wall.this.angle = angle1;
          Wall.this.extent = extend1;
        }
      },
      new Control() {
        @Override
        public boolean isMouseOver(Vec2 screenPos, Camera camera) {
          Vec2 offset = Vec2.of(Math.cos(angle) * extent, Math.sin(angle) * extent);
          Vec2 posScreen = camera.toScreenSpace(Vec3.of(pos.subtract(offset), 0));
          return posScreen.subtract(screenPos).lengthSq() < 144;
        }

        @Override
        public boolean startDragging(Vec2 screenPos, Camera camera) {
          return isSelected() && isMouseOver(screenPos, camera);
        }

        @Override
        public void drag(Vec2 screenStart, Vec2 screenNext, Camera camera) {
          Vec2 offset = Vec2.of(Math.cos(angle) * extent, Math.sin(angle) * extent);
          Vec2 otherHandle = pos.add(offset);
          Vec2 nextWorld = camera.fromScreenSpace(screenNext);

          if (Main.GAME_INSTANCE.keyboard.isPressed(KeyboardInputManager.SNAP)) {
            double snapDistance = Main.GAME_INSTANCE.getSnapDistance();
            nextWorld = Vec2.of(MathUtil.roundTo(nextWorld.x(), snapDistance), MathUtil.roundTo(nextWorld.y(), snapDistance));
          }

          Vec2 diff = nextWorld.subtract(otherHandle);

          double extend1 = diff.length() / 2;
          double angle1 = Math.PI + Math.atan2(diff.y(), diff.x());
          Vec2 newPos = otherHandle.add(diff.scale(0.5));

          BoundingRect rect = new BoundingRect(-extend1, -2 * halfWidth, extend1, 0).rotate(0, 0, angle1).translate(newPos);

          if (getLevel().intersectsObject(rect, o -> !(o instanceof Wall)).findAny().isPresent()) {
            return;
          }

          Wall.this.pos = newPos;
          Wall.this.angle = angle1;
          Wall.this.extent = extend1;
        }
      },
      new Control() {
        @Override
        public boolean isMouseOver(Vec2 screenPos, Camera camera) {
          Vec2 worldStart = camera.fromScreenSpace(screenPos);
          return isPointOverWall(worldStart.x(), worldStart.y());
        }

        @Override
        public boolean onClick(Vec2 screenPos, Camera camera) {
          if (isMouseOver(screenPos, camera)) {
            select();
            return true;
          } else {
            unselect();
            return false;
          }
        }

        @Override
        public boolean startDragging(Vec2 screenPos, Camera camera) {
          if (!isSelected()) {
            return false;
          }
          boolean bool = isMouseOver(screenPos, camera);
          if (bool) {
            startPos = pos;
          }
          return bool;
        }

        private Vec2 startPos;

        @Override
        public void drag(Vec2 screenStart, Vec2 screenNext, Camera camera) {
          Vec2 next = camera.fromScreenSpace(screenNext);
          Vec2 start = camera.fromScreenSpace(screenStart);
          Vec2 newPos = startPos.add(next).subtract(start);
          if (Main.GAME_INSTANCE.keyboard.isPressed(KeyboardInputManager.SNAP)) {
            double snapDistance = Main.GAME_INSTANCE.getSnapDistance();
            newPos = Vec2.of(MathUtil.roundTo(newPos.x(), snapDistance), MathUtil.roundTo(newPos.y(), snapDistance));
          }
          BoundingRect maxRect = getBoundingRect();
          getLevel().getPosition(newPos, maxRect, o -> !(o instanceof Wall)).ifPresent(Wall.this::setPos);
        }
      }
  };


  @Override
  protected void reset() {
  }

  @Override
  public Control[] getControls() {
    return controls;
  }

  private boolean isPointOverWall(double x, double y) {
    double sin = Math.sin(angle);
    double cos = -Math.cos(angle);
    x -= pos.x();
    y -= pos.y();
    double newX = x * cos - y * sin;
    double newY = x * sin + y * cos;
    return newX >= -extent && newX <= extent && newY >= 0 && newY <= halfWidth * 2;
  }

  @Override
  public void initBodies(BiFunction<BodyDef, FixtureDef, Body> function) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.position = new org.jbox2d.common.Vec2((float) pos.x(), (float) pos.y());
    bodyDef.angle = (float) angle;
    FixtureDef fixtureDef = new FixtureDef();
    PolygonShape polygonShape = new PolygonShape();
    polygonShape.setAsBox((float) extent, halfWidth * 2, new org.jbox2d.common.Vec2(0, -2 * halfWidth), 0);
    fixtureDef.shape = polygonShape;
    fixtureDef.friction = 1;
    fixtureDef.restitution = 0;
    function.apply(bodyDef, fixtureDef);
  }

  @Override
  public ObjectRenderer getRenderer() {
    renderer.x = pos.x();
    renderer.y = pos.y();
    renderer.extent = extent;
    renderer.angle = angle;
    renderer.rect = getBoundingRect().translate(pos);
    renderer.selected = isSelected();
    return renderer;
  }

  public void setPos(Vec2 pos) {
    this.pos = pos;
  }

  @Override
  protected BoundingRect getBoundingRect() {
    return new BoundingRect(-extent, -2 * halfWidth, extent, 0).rotate(0, 0, angle);
  }

  private static class WallRenderer implements ObjectRenderer {
    private double x, y;
    private double angle;
    private double extent;
    private BoundingRect rect;
    private boolean selected;

    @Override
    public void render(DrawPass pass, Camera camera, Vec4 color, double timeAfterUpdate) {
      Mat4 matrix = camera.getMatrix();
      switch (pass) {
        case LINES:
          wireframeCube.draw(uniformSetter -> {
            uniformSetter.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix.multiply(Mat4.translate(x, y, 0)).multiply(Mat4.rotate(0, 0, angle))
                                                                             .multiply(Mat4.scale(extent, halfWidth, 1, 1)));
            uniformSetter.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, color.w()));
          });
          break;
        case DEPTH_ONLY:
          GL20C.glEnable(GL20C.GL_POLYGON_OFFSET_FILL);
          GL20C.glPolygonOffset(1.0f, 1.0f);
          solidCube.draw(uniformSetter -> {
            uniformSetter.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix.multiply(Mat4.translate(x, y, 0)).multiply(Mat4.rotate(0, 0, angle))
                                                                             .multiply(Mat4.scale(extent, halfWidth, 1, 1)));
            uniformSetter.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, 0));
          });
          GL20C.glDisable(GL20C.GL_POLYGON_OFFSET_FILL);
          break;
      }
    }

    @Override
    public void renderFlat(DrawPass pass, Camera camera, Vec4 color) {
      Mat4 matrix = camera.getMatrix();
      switch (pass) {
        case TRANSPARENT:
          SimpleBuilder dataBuilder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR, 6);
          dataBuilder.startStrip(1, builder -> {
            builder.pos((float) rect.minX, (float) rect.minY, 0).color(0, 0, 0, 1).end();
            builder.pos((float) rect.maxX, (float) rect.minY, 0).color(0, 0, 0, 1).end();
            builder.pos((float) rect.maxX, (float) rect.maxY, 0).color(0, 0, 0, 1).end();
            builder.pos((float) rect.minX, (float) rect.maxY, 0).color(0, 0, 0, 1).end();
            builder.pos((float) rect.minX, (float) rect.minY, 0).color(0, 0, 0, 1).end();
          });
          try (BufferRenderer renderer = dataBuilder.upload(OGLManager.STREAM_DRAW_ALLOCATOR, ModelTransformer.IDENTITY)) {
            if (selected) {
              GL20C.glEnable(GL30.GL_LINE_STIPPLE);
              GL30.glLineStipple(1, (short) 0x00FF);
            }
            renderer.draw(c -> {
              c.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix);
              c.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, selected ? color.w() : color.w() / 4));
            });
            GL20C.glDisable(GL30.GL_LINE_STIPPLE);
          }
          double offsetY = Math.sin(angle) * extent;
          double offsetX = Math.cos(angle) * extent;

          round.draw(uniformSetter -> {
            uniformSetter.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix.multiply(Mat4.translate(x + offsetX, y + offsetY, 1))
                                                                             .multiply(Mat4.scale(halfWidth / 2, halfWidth / 2, 1, 1)));
            uniformSetter.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, color.w()));
          });
          round.draw(uniformSetter -> {
            uniformSetter.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix.multiply(Mat4.translate(x - offsetX, y - offsetY, 1))
                                                                             .multiply(Mat4.scale(halfWidth / 2, halfWidth / 2, 1, 1)));
            uniformSetter.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, color.w()));
          });
          break;
        case LINES:
          wireframeCube.draw(uniformSetter -> {
            uniformSetter.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix.multiply(Mat4.translate(x, y, 0)).multiply(Mat4.rotate(0, 0, angle))
                                                                             .multiply(Mat4.scale(extent, halfWidth, 1, 1)));
            uniformSetter.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, color.w()));
          });
          break;
        case DEPTH_ONLY:
          GL20C.glEnable(GL20C.GL_POLYGON_OFFSET_FILL);
          GL20C.glPolygonOffset(1.0f, 1.0f);
          solidCube.draw(uniformSetter -> {
            uniformSetter.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix.multiply(Mat4.translate(x, y, 0)).multiply(Mat4.rotate(0, 0, angle))
                                                                             .multiply(Mat4.scale(extent, halfWidth, 1, 1)));
            uniformSetter.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, 0));
          });
          GL20C.glDisable(GL20C.GL_POLYGON_OFFSET_FILL);
          break;
      }
    }
  }
}
