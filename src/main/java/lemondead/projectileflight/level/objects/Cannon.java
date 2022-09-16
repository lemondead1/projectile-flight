package lemondead.projectileflight.level.objects;

import lemondead.game.engine.render.Camera;
import lemondead.game.engine.render.TextRenderer;
import lemondead.game.engine.render.buffers.BufferRenderer;
import lemondead.game.engine.render.buffers.ModelTransformer;
import lemondead.game.engine.render.buffers.SimpleBuilder;
import lemondead.game.engine.render.obj.ObjLoader;
import lemondead.game.engine.render.ogl.*;
import lemondead.game.engine.util.BoundingRect;
import lemondead.game.engine.util.MathUtil;
import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec2;
import lemondead.game.engine.util.vector.Vec3;
import lemondead.game.engine.util.vector.Vec4;
import lemondead.projectileflight.KeyboardInputManager;
import lemondead.projectileflight.Main;
import lemondead.projectileflight.ProjectileFlight;
import lemondead.projectileflight.level.LevelObject;
import lemondead.projectileflight.level.ObjectRenderer;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;

import java.util.function.BiFunction;

public class Cannon extends LevelObject {
  private static final float friction = Float.parseFloat(
      Main.GAME_INSTANCE.configuration.getValue("physics.ball_friction_coefficient", "0.5", s -> {
        float f;
        try {
          f = Float.parseFloat(s);
        } catch (NumberFormatException e) {
          return false;
        }
        return f >= 0 && f <= 1;
      }));
  private static final float restitution = Float.parseFloat(
      Main.GAME_INSTANCE.configuration.getValue("physics.ball_restitution_coefficient", "0.5", s -> {
        float f;
        try {
          f = Float.parseFloat(s);
        } catch (NumberFormatException e) {
          return false;
        }
        return f >= 0 && f <= 1;
      }));
  private static final boolean rotationEnabled = Boolean.parseBoolean(
      Main.GAME_INSTANCE.configuration.getValue("physics.ball_rotation_enabled", "true", s -> s.toLowerCase().matches("(true)|(false)")));

  private static final float ballRadius = 0.5f;
  private static final float arrowLengthMultiplier = 1 / 3f;

  private static final BoundingRect cannonRect = new BoundingRect(-1, -0.8, 4, 0.8).scale(5 / 7f);
  private static final BoundingRect wheelsRect = new BoundingRect(-1.2, -1.45, 1.2, 0.95).scale(5 / 7f);

  private static final ObjLoader.BakedObj ballModel;
  private static final ObjLoader.BakedObj cannonFrontModel;
  private static final ObjLoader.BakedObj cannonModel;
  private static final ObjLoader.BakedObj cannonWheelModel;


  static {
    OGLBufferAllocator alloc = OGLManager.STATIC_DRAW_ALLOCATOR;
    cannonWheelModel = ObjLoader.loadSafe(alloc, ModelTransformer.scale(5 / 7f), "models/cannon_wheels.obj");
    ballModel = ObjLoader.loadSafe(alloc, ModelTransformer.scale(10 / 7f), "models/cannon_ball.obj");
    cannonFrontModel = ObjLoader.loadSafe(alloc, ModelTransformer.scale(5 / 7f), "models/cannon_front.obj");
    cannonModel = ObjLoader.loadSafe(alloc, ModelTransformer.scale(5 / 7f), "models/cannon.obj");
  }

  private double angle = (float) Math.PI / 3;
  private double initialSpeed = 15;
  private Body ball;
  private final CannonRenderer renderer = new CannonRenderer();

  private final Control[] controls = {
      new Control() {
        @Override
        public boolean isMouseOver(Vec2 screenPos, Camera camera) {
          Vec2 worldStart = camera.fromScreenSpace(screenPos);
          return getCannonRect().translate(pos).contains(worldStart.x(), worldStart.y()) ||
                 getWheelsRect().translate(pos).contains(worldStart.x(), worldStart.y());
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
          getLevel().getPosition(newPos, maxRect, o -> o != Cannon.this).ifPresent(Cannon.this::setPos);
        }
      },
      new Control() {
        @Override
        public boolean isMouseOver(Vec2 screenPos, Camera camera) {
          double d = getArrowLength();
          Vec2 pos = getPos();
          Vec2 screenControlPos = camera.toScreenSpace(Vec3.of(pos.x() + Math.cos(angle) * d, pos.y() + Math.sin(angle) * d, 0));
          return screenControlPos.subtract(screenPos).lengthSq() < 256;
        }

        @Override
        public boolean startDragging(Vec2 screenPos, Camera camera) {
          return isMouseOver(screenPos, camera) && isSelected();
        }

        @Override
        public void drag(Vec2 screenStart, Vec2 screenNext, Camera camera) {
          Vec2 nextWorld = camera.fromScreenSpace(screenNext);
          Vec2 pos = getPos();
          double distance = nextWorld.subtract(pos).length();
          double sin = (nextWorld.y() - pos.y()) / distance;
          distance = MathUtil.clamp(4, distance, 15);
          initialSpeed = distance / arrowLengthMultiplier;
          double newAngle = Math.asin(sin);
          if (nextWorld.x() < pos.x()) {
            newAngle = Math.PI - newAngle;
          } else if (newAngle < 0) {
            newAngle += Math.PI * 2;
          }
          if (Main.GAME_INSTANCE.keyboard.isPressed(KeyboardInputManager.SNAP)) {
            newAngle = MathUtil.roundTo(newAngle, Math.PI / 24);
            if (Math.abs(newAngle - Math.PI * 2) < 1E-4) {
              newAngle = 0;
            }
          }
          if (!getLevel().intersectsObject(cannonRect.rotate(0, 0, newAngle).translate(pos.x(), pos.y()), o -> o != Cannon.this)
                         .findAny().isPresent()) {
            angle = (float) newAngle;
          }
        }
      }
  };

  public Cannon() {
  }

  private BoundingRect getCannonRect() {
    return cannonRect.rotate(0, 0, angle);
  }

  private BoundingRect getWheelsRect() {
    return wheelsRect;
  }

  private double getArrowLength() {
    return initialSpeed * arrowLengthMultiplier;
  }

  @Override
  protected void reset() {
    ball = null;
  }

  @Override
  public ObjectRenderer getRenderer() {
    renderer.posX = pos.x();
    renderer.posY = pos.y();
    renderer.selected = isSelected();
    renderer.arrowLength = (float) getArrowLength();
    renderer.rect = getBoundingRect().translate(pos);
    if (ball == null) {
      renderer.ballPosX = pos.x();
      renderer.ballPosY = pos.y();
      renderer.ballVelX = 0;
      renderer.ballVelY = 0;
      renderer.ballAngle = 0;
      renderer.ballAngularVelocity = 0;
    } else {
      org.jbox2d.common.Vec2 position = ball.getPosition();
      renderer.ballPosX = position.x;
      renderer.ballPosY = position.y;
      org.jbox2d.common.Vec2 linearVelocity = ball.getLinearVelocity();
      renderer.ballVelX = linearVelocity.x;
      renderer.ballVelY = linearVelocity.y;
      renderer.ballAngle = ball.getAngle();
      renderer.ballAngularVelocity = ball.getAngularVelocity();
    }
    renderer.angle = angle;
    renderer.initialSpeed = initialSpeed;
    return renderer;
  }

  @Override
  public void initBodies(BiFunction<BodyDef, FixtureDef, Body> function) {
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyType.DYNAMIC;
    Vec2 pos = getPos();
    bodyDef.position = new org.jbox2d.common.Vec2((float) pos.x(), (float) pos.y());
    bodyDef.linearVelocity = new org.jbox2d.common.Vec2((float) (Math.cos(angle) * initialSpeed), (float) (Math.sin(angle) * initialSpeed));
    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.density = 1000;
    fixtureDef.restitution = restitution;
    CircleShape circleShape = new CircleShape();
    circleShape.m_radius = ballRadius;
    fixtureDef.shape = circleShape;
    fixtureDef.friction = friction * friction;
    bodyDef.fixedRotation = !rotationEnabled;
    ball = function.apply(bodyDef, fixtureDef);
  }

  @Override
  public Control[] getControls() {
    return controls;
  }

  public BoundingRect getUntranslatedMaxRect() {
    return cannonRect.rotate(0, 0, angle).unite(wheelsRect);
  }


  @Override
  protected BoundingRect getBoundingRect() {
    return getCannonRect().unite(getWheelsRect());
  }

  @Override
  public BoundingRect[] getCullingRects() {
    if (ball == null) {
      return super.getCullingRects();
    }
    org.jbox2d.common.Vec2 ballPos = ball.getPosition();
    BoundingRect[] rects = new BoundingRect[2];
    rects[0] = getBoundingRect().translate(getPos());
    rects[1] = BoundingRect.withCenter(ballPos.x, ballPos.y, ballRadius * 2, ballRadius * 2);
    return rects;
  }

  private static class CannonRenderer implements ObjectRenderer {
    private double posX, posY;
    private double angle, initialSpeed;
    private double ballPosX, ballPosY;
    private double ballVelX, ballVelY;
    private double ballAngle, ballAngularVelocity;
    private BoundingRect rect;
    private boolean selected;
    private float arrowLength;

    @Override
    public void render(DrawPass pass, Camera camera, Vec4 color, double timeAfterUpdate) {
      Vec4 newColor = Vec4.of(0, 0, 0, color.w());
      Mat4 matrix = camera.getMatrix();
      switch (pass) {
        case DEPTH_ONLY:
          cannonModel.drawSolid(matrix.multiply(Mat4.translate(posX, posY, 0)).multiply(Mat4.rotate(0, 0, angle)), Vec4.ZERO);
          ballModel.drawSolid(matrix.multiply(Mat4.translate(ballPosX + ballVelX * timeAfterUpdate, ballPosY + ballVelY * timeAfterUpdate, 0))
                                    .multiply(Mat4.rotate(0, 0, ballAngle + ballAngularVelocity * timeAfterUpdate)), newColor);
          break;
        case LINES:
          ballModel.drawLines(matrix.multiply(Mat4.translate(ballPosX + ballVelX * timeAfterUpdate, ballPosY + ballVelY * timeAfterUpdate, 0))
                                    .multiply(Mat4.rotate(0, 0, ballAngle + ballAngularVelocity * timeAfterUpdate)), newColor);
          Mat4 translated = matrix.multiply(Mat4.translate(posX, posY, 0));
          cannonWheelModel.drawLines(translated, newColor);
          cannonModel.drawLines(translated.multiply(Mat4.rotate(0, 0, angle)), newColor);
          break;
        case TRANSPARENT:
          TextRenderer text = Main.GAME_INSTANCE.textRenderer;
          cannonFrontModel.drawLines(matrix.multiply(Mat4.translate(posX, posY, 0)).multiply(Mat4.rotate(0, 0, angle)), newColor);
          if (Main.GAME_INSTANCE.keyboard.isPressed(KeyboardInputManager.SHOW_VELOCITIES)) {
            SimpleBuilder builder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR);
            float length = (float) Math.sqrt(ballVelX * ballVelX + ballVelY * ballVelY);
            builder.startVertices(v -> {
              v.pos(0, 0, 0).color(1, 1, 1, 1).end();
              v.pos(length, 0, 0).color(1, 1, 1, 1).end();
              v.pos(length - 0.5f, 0.2f, 0).color(1, 1, 1, 1).end();
              v.pos(length, 0, 0).color(1, 1, 1, 1).end();
              v.pos(length - 0.5f, -0.2f, 0).color(1, 1, 1, 1).end();
              v.pos(length, 0, 0).color(1, 1, 1, 1).end();
            });
            try (BufferRenderer renderer = builder.upload(OGLManager.STREAM_DRAW_ALLOCATOR, ModelTransformer.IDENTITY)) {
              renderer.draw(u -> {
                u.setUniform(Program.Uniform.TRANSFORM_MATRIX, matrix.multiply(Mat4.translate(ballPosX, ballPosY, 0))
                                                                     .multiply(Mat4.rotate(0, 0, Math.atan2(ballVelY, ballVelX))));
                u.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, 1).multiply(color));
              });
            }
            Vec2 screenPos = camera.toScreenSpace(Vec3.of(ballPosX + ballVelX / 2, ballPosY + ballVelY / 2, 0)).add(5, 5);
            text.drawString("X " + ProjectileFlight.DECIMAL_FORMAT.format(ballVelX), screenPos.add(0, 14), Vec4.of(0, 0, 0, 0.5).multiply(color),
                            TextRenderer.FontSize.PT_11, TextRenderer.Alignment.CENTERED, camera);
            text.drawString("Y " + ProjectileFlight.DECIMAL_FORMAT.format(ballVelY), screenPos, Vec4.of(0, 0, 0, 0.5).multiply(color),
                            TextRenderer.FontSize.PT_11, TextRenderer.Alignment.CENTERED, camera);
          }
          break;
      }
    }

    @Override
    public void renderFlat(DrawPass pass, Camera camera, Vec4 color) {
      Mat4 matrix = camera.getMatrix();
      Vec4 newColor = Vec4.of(0, 0, 0, color.w());
      switch (pass) {
        case DEPTH_ONLY:
          cannonModel.drawSolid(matrix.multiply(Mat4.translate(posX, posY, 0)).multiply(Mat4.rotate(0, 0, angle)), Vec4.ZERO);
          break;
        case LINES:
          Mat4 translated = matrix.multiply(Mat4.translate(posX, posY, 0));
          cannonWheelModel.drawLines(translated, newColor);
          cannonModel.drawLines(translated.multiply(Mat4.rotate(0, 0, angle)), newColor);
          SimpleBuilder dataBuilder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR, 6);
          dataBuilder.startVertices(builder -> {
            builder.pos(2, 0, 0).color(1, 1, 1, 1).end();
            builder.pos(arrowLength, 0, 0).color(1, 1, 1, 1).end();
            builder.pos(arrowLength - 0.15f, 0.1f, 0).color(1, 1, 1, 1).end();
            builder.pos(arrowLength, 0, 0).color(1, 1, 1, 1).end();
            builder.pos(arrowLength, 0, 0).color(1, 1, 1, 1).end();
            builder.pos(arrowLength - 0.15f, -0.1f, 0).color(1, 1, 1, 1).end();
          });
          try (BufferRenderer buffer = dataBuilder.upload(OGLManager.STREAM_DRAW_ALLOCATOR, ModelTransformer.IDENTITY)) {
            Mat4 arrowMatrix = translated.multiply(Mat4.rotate(0, 0, angle));
            buffer.draw(c -> {
              c.setUniform(Program.Uniform.TRANSFORM_MATRIX, arrowMatrix);
              c.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, color.w()));
            });
          }
          break;
        case TRANSPARENT:
          cannonFrontModel.drawLines(matrix.multiply(Mat4.translate(posX, posY, 0)).multiply(Mat4.rotate(0, 0, angle)), newColor);
          dataBuilder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR, 6);
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
          if (selected) {
            double textPosX = posX + arrowLength * Math.cos(angle);
            double textPosY = posY + arrowLength * Math.sin(angle);
            TextRenderer text = Main.GAME_INSTANCE.textRenderer;
            text.drawString(ProjectileFlight.DECIMAL_FORMAT.format(Math.toDegrees(angle)) + "°",
                            camera.toScreenSpace(Vec3.of(textPosX, textPosY, 0)).add(5, 5),
                            Vec4.of(0, 0, 0, color.w() * 0.5), TextRenderer.FontSize.PT_14, TextRenderer.Alignment.LEFT, camera);
            text.drawString(ProjectileFlight.DECIMAL_FORMAT.format(initialSpeed) + " m/s",
                            camera.toScreenSpace(Vec3.of(textPosX, textPosY, 0)).add(5, 25),
                            Vec4.of(0, 0, 0, color.w() * 0.5), TextRenderer.FontSize.PT_14, TextRenderer.Alignment.LEFT, camera);
          }
          break;
      }
    }
  }
}
