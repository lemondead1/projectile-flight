package lemondead.projectileflight;

import lemondead.game.engine.Application;
import lemondead.game.engine.render.Camera;
import lemondead.game.engine.render.Image;
import lemondead.game.engine.render.TextRenderer;
import lemondead.game.engine.render.buffers.BufferRenderer;
import lemondead.game.engine.render.buffers.ModelTransformer;
import lemondead.game.engine.render.buffers.SimpleBuilder;
import lemondead.game.engine.render.ogl.DrawPass;
import lemondead.game.engine.render.ogl.OGLManager;
import lemondead.game.engine.render.ogl.Program;
import lemondead.game.engine.render.ogl.RenderMode;
import lemondead.game.engine.util.*;
import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec2;
import lemondead.game.engine.util.vector.Vec3;
import lemondead.game.engine.util.vector.Vec4;
import lemondead.projectileflight.level.Level;
import lemondead.projectileflight.level.LevelObject;
import lemondead.projectileflight.level.ObjectRenderer;
import lemondead.projectileflight.level.objects.Cannon;
import lemondead.projectileflight.level.objects.Wall;
import lemondead.projectileflight.utils.Quadruple;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.WorldManifold;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL20C;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ProjectileFlight extends Application {
  public double cameraVelocityX = 0;
  public double cameraVelocityY = 0;
  public BufferRenderer floor;
  public BufferRenderer grid;
  public BufferRenderer guiSquare;
  public BufferRenderer hitRenderer;
  public Level level;
  public double flatten = 0;
  public final Camera camera = new Camera(Math.toRadians(60), Vec3.of(0, 0, 50), 5, 500, flatten, Vec2.of(800, 800));
  public boolean flattening = false;
  public final AtomicBoolean paused = new AtomicBoolean(true);
  public final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
  public ScheduledFuture<?> worldFuture;
  public static World world;
  public static double worldTime;
  public final List<Quadruple<Vec2, Vec2, Double, Body>> collisions = new ArrayList<>();
  public volatile long lastUpdateTime = 0;
  public final DecimalFormat framerateFormat = new DecimalFormat("0.0");
  public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");
  public DecimalFormat timeFormat;
  public DecimalFormat distanceFormat;

  private boolean showFPS;

  private static final double cameraDamping = 0.00179701029;
  private static final double cameraDampingLog = Math.log(cameraDamping);

  public final Configuration configuration = new Configuration("projectileflight.cfg");

  public KeyboardInputManager keyboard;
  public MouseInputManager mouse;

  public ResourceBundle labels;

  public PlaySpeed playSpeed = PlaySpeed.X1;

  public boolean showKeybinds = true;

  public Vec2 gravity;

  private final Object mutex = new Object();

  public ProjectileFlight() {
    labels = ResourceBundle.getBundle("Labels");

    timeFormat = new DecimalFormat("0.00 " + labels.getString("units.seconds_short"));
    distanceFormat = new DecimalFormat("0.00 " + labels.getString("units.meter_short"));
    distanceFormat = new DecimalFormat("0.00 " + labels.getString("units.meter_per_second_short"));

    title = labels.getString("app.title");
  }

  @Override
  protected void stop() {
    super.stop();
    executorService.shutdownNow();
  }

  @Override
  protected void setup() {
    super.setup();

    mouse = new MouseInputManager(window);
    keyboard = new KeyboardInputManager(window);


    mouse.setDragStartCallback((startX, startY) -> {
      if (flatten == 1) {
        Vec2 start = Vec2.of(startX, startY);
        Optional<LevelObject.Control> handlerOpt = level.onMouseDrag(start, camera);
        if (handlerOpt.isPresent()) {
          return handlerOpt.map(h -> new DragHandler() {
            @Override
            public void onDrag(double prevX, double prevY, double nextX, double nextY) {
              h.drag(start, Vec2.of(nextX, nextY), camera);
            }

            @Override
            public void onDragStop() {
              h.stopDragging();
            }
          }).get();
        }
      }
      return (prevX, prevY, nextX, nextY) -> {
        Vec2 prevWorld = camera.fromScreenSpace(Vec2.of(prevX, prevY));
        Vec2 nextWorld = camera.fromScreenSpace(Vec2.of(nextX, nextY));
        camera.translate(prevWorld.x() - nextWorld.x(), prevWorld.y() - nextWorld.y(), 0);
        cameraVelocityX = (cameraVelocityX + (prevWorld.x() - nextWorld.x()) / prevFrameTime * 1E+9) / 2;
        cameraVelocityY = (cameraVelocityY + (prevWorld.y() - nextWorld.y()) / prevFrameTime * 1E+9) / 2;
      };
    });
    mouse.setClickCallback((x, y, button) -> {
      if (flatten == 1 && button == MouseInputManager.MouseButton.LMB) {
        level.onMouseClick(Vec2.of(x, y), camera);
      }
    });
    window.setScrollCallback((x, y) -> {
      Vec3 cameraPos = camera.getPos();
      Vec2 mousePos = mouse.getCurrentPos();
      Vec2 world = camera.fromScreenSpace(mousePos);
      Vec3 mouse3 = Vec3.of(world.x(), world.y(), 0);
      double d = Math.pow(1.125, -y);
      d = MathUtil.clamp(10, cameraPos.z() * d, 250) / cameraPos.z();
      Vec3 vec = cameraPos.subtract(mouse3).scale(d).add(mouse3);
      camera.setPos(vec);
    });
    window.setFramebufferSizeCallback((width, height) -> {
      camera.setScreenSize(Vec2.of(width, height));
      OGLManager.onFramebufferSizeCallback(width, height);
    });

    showFPS = configuration.getValueParsed("debug.show_fps", "false", Boolean::parseBoolean);

    Image icon16 = new Image("icons/icon_16.png", 4);
    Image icon32 = new Image("icons/icon_32.png", 4);
    Image icon48 = new Image("icons/icon_48.png", 4);
    Image icon64 = new Image("icons/icon_64.png", 4);
    Image icon128 = new Image("icons/icon_128.png", 4);
    Image icon256 = new Image("icons/icon_256.png", 4);
    window.setIcon(icon16, icon32, icon48, icon64, icon128, icon256);
    icon16.free();
    icon32.free();
    icon48.free();
    icon64.free();
    icon128.free();
    icon256.free();

    int gridX = -50;
    int gridY = -30;
    int gridWidth = 100;
    int gridHeight = 60;

    SimpleBuilder floorBuilder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR);
    floorBuilder.startVertices(builder -> {
      builder.pos(gridX, 0, 2).color(0, 0, 0, 1).end();
      builder.pos(gridX + gridWidth, 0, 2).color(0, 0, 0, 1).end();
      builder.pos(gridX, 0, -2).color(0, 0, 0, 1).end();
      builder.pos(gridX + gridWidth, 0, -2).color(0, 0, 0, 1).end();
    });
    floor = floorBuilder.upload(OGLManager.STATIC_DRAW_ALLOCATOR, ModelTransformer.IDENTITY);

    SimpleBuilder gridBuilder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR);
    gridBuilder.startVertices(builder -> {
      for (int y = gridY; y <= gridY + gridHeight; y++) {
        builder.pos(gridX, y, 0).color(1, 1, 1, 1).end();
        builder.pos(gridX + gridWidth, y, 0).color(1, 1, 1, 1).end();
      }
      for (int x = gridX; x <= gridX + gridWidth; x++) {
        builder.pos(x, gridY, 0).color(1, 1, 1, 1).end();
        builder.pos(x, gridY + gridHeight, 0).color(1, 1, 1, 1).end();
      }
    });
    grid = gridBuilder.upload(OGLManager.STATIC_DRAW_ALLOCATOR, ModelTransformer.IDENTITY);

    SimpleBuilder guiSquareBuilder = new SimpleBuilder(RenderMode.SOLID, Program.POSITION_COLOR);
    float bindingsRectHeight = keyboard.getBindings().size() * TextRenderer.FontSize.PT_14.getPixelSize() * 1.25f + 24;
    guiSquareBuilder.startFan(2, fan -> {
      fan.pos(20, 20, 0).color(1, 1, 1, 1).end();
      fan.pos(358, 20, 0).color(1, 1, 1, 1).end();
      fan.pos(358, bindingsRectHeight, 0).color(1, 1, 1, 1).end();
      fan.pos(20, bindingsRectHeight, 0).color(1, 1, 1, 1).end();
    });
    guiSquare = guiSquareBuilder.upload(OGLManager.STATIC_DRAW_ALLOCATOR, ModelTransformer.IDENTITY);
    SimpleBuilder builder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR);
    builder.startVertices(v -> {
      v.pos(-6, -6, 0).color(0, 0, 0, 1).end();
      v.pos(6, 6, 0).color(0, 0, 0, 1).end();
      v.pos(-6, 6, 0).color(0, 0, 0, 1).end();
      v.pos(6, -6, 0).color(0, 0, 0, 1).end();
    });
    hitRenderer = builder.upload(OGLManager.STATIC_DRAW_ALLOCATOR, ModelTransformer.IDENTITY);

    level = new Level();
    Cannon cannon = new Cannon();
    level.addObject(cannon, true);
    Wall.init();

    double gravX = configuration.getValueParsed("physics.gravity_x", "0", Double::parseDouble);
    double gravY = configuration.getValueParsed("physics.gravity_y", "-9.8", Double::parseDouble);
    gravity = Vec2.of(gravX, gravY);
    initWorld();

    configuration.saveConfig();

    Main.logger.info("Projectile flight has started in " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - Main.START_TIME) + " millis.");
  }

  public boolean isInEditMode() {
    return flattening && flatten == 1;
  }

  public boolean isInShowMode() {
    return !flattening && flatten == 0;
  }

  public void addLevelObjectAtMouse(LevelObject object) {
    Vec2 pos = camera.fromScreenSpace(mouse.getCurrentPos());
    object.setPos(pos);
    level.addObject(object, true);
  }

  public void togglePause() {
    boolean temp;
    do {
      temp = paused.get();
    } while (!paused.compareAndSet(temp, !temp));
  }

  public void resetWorld() {
    paused.set(true);
    initWorld();
  }

  public void removeSelectedObject() {
    if (!isInEditMode()) {
      return;
    }
    if (isInEditMode() && level.getSelected() != null) {
      level.removeObject(level.getSelected());
    }
  }

  public void startModeChange() {
    level.reset();
    paused.set(true);
    if (flattening) {
      flattening = false;
      initWorld();
    } else {
      flattening = true;
      level.reset();
      if (worldFuture != null) {
        worldFuture.cancel(true);
      }
    }
  }

  public void moveCameraToOrigin() {
    Vec2 diff = Vec2.of(camera.getPos().x(), camera.getPos().y());
    Vec2 direction = Vec2.of(-diff.x(), -diff.y()).normalize();
    double abs = -diff.length() * cameraDampingLog;
    cameraVelocityX = direction.x() * abs;
    cameraVelocityY = direction.y() * abs;
  }

  public void setPlaySpeedAndRescheduleWorldUpdate(PlaySpeed playSpeed) {
    this.playSpeed = playSpeed;
    if (worldFuture != null) {
      worldFuture.cancel(false);
    }
    worldFuture = executorService.scheduleAtFixedRate(() -> {
      if (!paused.get()) {
        synchronized (mutex) {
          worldTime += 1 / 120f;
          world.step(1 / 120f, 16, 8);
        }
      }
      lastUpdateTime = System.nanoTime();
    }, 0, (long) (1E+9 / playSpeed.getSpeedCoefficient() / 120), TimeUnit.NANOSECONDS);
  }

  public void initWorld() {
    if (worldFuture != null) {
      worldFuture.cancel(true);
    }

    collisions.clear();
    worldTime = 0;

    world = new World(new org.jbox2d.common.Vec2((float) gravity.x(), (float) gravity.y()));
    BodyDef groundBodyDef = new BodyDef();
    groundBodyDef.position.set(0, -10);
    Body groundBody = world.createBody(groundBodyDef);
    PolygonShape groundBox = new PolygonShape();
    groundBox.setAsBox(1E+6f, 10);
    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.shape = groundBox;
    fixtureDef.friction = 1;
    groundBody.createFixture(fixtureDef);

    level.collectBodies((body, fixture) -> {
      Body body1 = world.createBody(body);
      body1.createFixture(fixture);
      return body1;
    });

    world.setContactListener(new ContactListener() {
      @Override
      public void beginContact(Contact contact) {
        Manifold manifold = contact.getManifold();
        WorldManifold worldManifold = new WorldManifold();
        contact.getWorldManifold(worldManifold);
        double x = worldManifold.points[0].x;
        double y = worldManifold.points[0].y;
        if (manifold.pointCount == 2) {
          x = (x + worldManifold.points[1].x) / 2;
          y = (y + worldManifold.points[1].y) / 2;
        }
        Body body1 = contact.getFixtureA().getBody();
        if (Math.abs(body1.getLinearVelocity().x * worldManifold.normal.x) + Math.abs(body1.getLinearVelocity().y * worldManifold.normal.y) > 2) {
          collisions.add(new Quadruple<>(Vec2.of(x, y), Vec2.of(worldManifold.normal.x, worldManifold.normal.y), worldTime, body1));
        }
        Body body2 = contact.getFixtureB().getBody();
        if (Math.abs(body2.getLinearVelocity().x * worldManifold.normal.x) + Math.abs(body2.getLinearVelocity().y * worldManifold.normal.y) > 2) {
          collisions.add(new Quadruple<>(Vec2.of(x, y), Vec2.of(worldManifold.normal.x, worldManifold.normal.y), worldTime, body2));
        }
      }

      @Override
      public void endContact(Contact contact) {
      }

      @Override
      public void preSolve(Contact contact, Manifold oldManifold) {
      }

      @Override
      public void postSolve(Contact contact, ContactImpulse impulse) {
      }
    });

    setPlaySpeedAndRescheduleWorldUpdate(playSpeed);
  }

  public static float getGridSize(double scale) {
    if (scale <= 10) {
      return 1;
    } else if (scale <= 20) {
      return 2;
    } else if (scale <= 50) {
      return 5;
    } else if (scale <= 100) {
      return 10;
    } else if (scale <= 250) {
      return 25;
    } else if (scale <= 500) {
      return 50;
    }
    return 100;
  }

  public float getSnapDistance() {
    double scale = camera.getFrustumHeight(1, camera.getPos().z() - 2);
    if (scale <= 10) {
      return 0.25f;
    } else if (scale <= 20) {
      return 0.5f;
    } else if (scale <= 50) {
      return 1;
    } else if (scale <= 100) {
      return 2;
    } else if (scale <= 250) {
      return 5;
    } else if (scale <= 500) {
      return 10;
    }
    return 20;
  }

  public static World getWorld() {
    return world;
  }

  @Override
  public void onFrame() {
    double deltaSeconds = prevFrameTime * 1E-9f;
    GL20C.glClear(GL20C.GL_DEPTH_BUFFER_BIT | GL20C.GL_COLOR_BUFFER_BIT);
    GLFW.glfwPollEvents();

    Vec2 mousePos = window.getCursorPos();
    Vec2 windowSize = window.getSize();
    Vec2.OfInt screenSize = OGLManager.getViewportSize();

    mouse.onMousePos(mousePos.x() / windowSize.x() * screenSize.x(), mousePos.y() / windowSize.y() * screenSize.y());

    if (flattening) {
      flatten = MathUtil.clamp(0, flatten + deltaSeconds * 4, 1);
    } else {
      flatten = MathUtil.clamp(0, flatten - deltaSeconds * 4, 1);
    }
    double smoothFlatten = MathUtil.smootherStep(flatten);
    camera.setFlatten(smoothFlatten);

    if (!mouse.isPressed()) {
      camera.translate(cameraVelocityX * (Math.pow(cameraDamping, deltaSeconds) - 1) / cameraDampingLog,
                       cameraVelocityY * (Math.pow(cameraDamping, deltaSeconds) - 1) / cameraDampingLog, 0);
      Vec2 vec = Vec2.of(cameraVelocityX, cameraVelocityY);
      double length = vec.length();
      vec = vec.normalize().scale(length < 0.01 ? 0 : length * MathUtil.clamp(0, Math.pow(cameraDamping, deltaSeconds), 1));
      cameraVelocityX = vec.x();
      cameraVelocityY = vec.y();
    }

    BoundingRect visible = camera.getFrustumBoundingRect(camera.getPos().z() - 2);
    double height = visible.maxY - visible.minY;
    float cellSize = getGridSize(height);
    float snapCellSize = getSnapDistance();
    double numberAlpha = 0.15f + smoothFlatten * 0.35f;

    float xOff = Math.floorDiv((int) camera.getPos().x(), (int) Math.ceil(cellSize)) * cellSize;
    float yOff = Math.floorDiv((int) camera.getPos().y(), (int) Math.ceil(cellSize)) * cellSize;

    TextRenderer.FontSize fontSize = TextRenderer.FontSize.PT_18;

    double worldTime;

    List<ObjectRenderer> renderers = new ArrayList<>();
    List<Quadruple<Vec2, Vec2, Double, Body>> collisions;

    double timeAfterTick;

    synchronized (mutex) {
      worldTime = ProjectileFlight.worldTime;
      timeAfterTick = paused.get() ? 0 : (System.nanoTime() - lastUpdateTime) * 1E-9 * playSpeed.getSpeedCoefficient();
      level.draw(camera.getFrustumBoundingRect(camera.getPos().z()).extend(0.5), renderers);
      collisions = new ArrayList<>(this.collisions);
    }

    Vec4 flatColor = Vec4.of(0, 0, 0, smoothFlatten);
    Vec4 color = Vec4.of(0, 0, 0, 1 - smoothFlatten);

    long finalPrevFrameTime = prevFrameTime;
    OGLManager.draw(drawPass -> {
      if (drawPass == DrawPass.TRANSPARENT) {
        float dashSize = 6;
        for (double x = MathUtil.floorTo(visible.minX, cellSize); x <= visible.maxX + cellSize; x += cellSize) {
          Vec2 pos = camera.toScreenSpace(Vec3.of(x, 0, 2));
          pos = Vec2.of(pos.x(), MathUtil.clamp(25 + dashSize, pos.y() + 25 + dashSize, screenSize.y() - 8 - dashSize));
          textRenderer.drawString(Double.toString(x), pos, Vec4.of(0, 0, 0, numberAlpha), fontSize, TextRenderer.Alignment.CENTERED, camera);
        }
        for (double y = MathUtil.floorTo(visible.minY, cellSize); y <= visible.maxY + cellSize; y += cellSize) {
          if (y == 0) {
            continue;
          }
          Vec2 pos = camera.toScreenSpace(Vec3.of(0, y, 2));
          pos = Vec2.of(dashSize + 2, pos.y() + 8);
          textRenderer.drawString(Double.toString(y), pos, Vec4.of(0, 0, 0, numberAlpha), fontSize, TextRenderer.Alignment.LEFT, camera);
        }

        SimpleBuilder ticks = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR);
        ticks.startVertices(builder -> {
          for (double x = MathUtil.floorTo(visible.minX, cellSize); x <= visible.maxX + cellSize; x += cellSize) {
            Vec2 pos = camera.toScreenSpace(Vec3.of(x, 0, 2));
            float y = (float) MathUtil.clamp(0, pos.y() + 1, screenSize.y - dashSize);
            builder.pos((float) pos.x(), y, 0).color(0, 0, 0, 1).end();
            builder.pos((float) pos.x(), y + dashSize, 0).color(0, 0, 0, 1).end();
          }
          for (double y = MathUtil.floorTo(visible.minY, cellSize); y <= visible.maxY + cellSize; y += cellSize) {
            if (y == 0) {
              continue;
            }
            Vec2 pos = camera.toScreenSpace(Vec3.of(0, y, 2));
            builder.pos(0, (float) pos.y(), 0).color(0, 0, 0, 1).end();
            builder.pos(dashSize, (float) pos.y(), 0).color(0, 0, 0, 1).end();
          }
        });

        try (BufferRenderer renderer = ticks.upload(OGLManager.STREAM_DRAW_ALLOCATOR, ModelTransformer.IDENTITY)) {
          renderer.draw(u -> {
            u.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, 1));
            u.setUniform(Program.Uniform.TRANSFORM_MATRIX, camera.getScreenMatrix());
          });
        }

        if (smoothFlatten != 0) {
          grid.draw(c -> {
            c.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, smoothFlatten * 0.15f));
            c.setUniform(Program.Uniform.TRANSFORM_MATRIX,
                         camera.getMatrix().multiply(Mat4.translate(xOff, yOff, 0)).multiply(Mat4.scale(cellSize, cellSize, 1, 1)));
          });
          grid.draw(c -> {
            c.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, smoothFlatten * 0.075f));
            c.setUniform(Program.Uniform.TRANSFORM_MATRIX,
                         camera.getMatrix().multiply(Mat4.translate(xOff, yOff, 0)).multiply(Mat4.scale(snapCellSize, snapCellSize, 1, 1)));
          });
        }

        Mat4 screenMatrix = camera.getScreenMatrix();
        if (keyboard.isPressed(KeyboardInputManager.SHOW_COLLISIONS)) {
          for (Quadruple<Vec2, Vec2, Double, Body> c : collisions) {
            double x = c.getObject1().x();
            double y = c.getObject1().y();
            double t = c.getObject3();
            Vec2 screenSpace = camera.toScreenSpace(Vec3.of(x, y, 0));
            hitRenderer.draw(uniformSetter -> {
              uniformSetter
                  .setUniform(Program.Uniform.TRANSFORM_MATRIX, screenMatrix.multiply(Mat4.translate(screenSpace.x(), screenSpace.y(), 0)));
              uniformSetter.setUniform(Program.Uniform.TINT, color);
            });
            String str = "Y " + DECIMAL_FORMAT.format(y);
            TextRenderer text = Main.GAME_INSTANCE.textRenderer;
            text.drawString(str, screenSpace.add(0, 20), Vec4.of(0, 0, 0, color.w() * 0.5), TextRenderer.FontSize.PT_11,
                            TextRenderer.Alignment.CENTERED, camera);
            String str1 = "X " + DECIMAL_FORMAT.format(x);
            text.drawString(str1, screenSpace.add(0, 36), Vec4.of(0, 0, 0, color.w() * 0.5), TextRenderer.FontSize.PT_11,
                            TextRenderer.Alignment.CENTERED, camera);
            String str2 = "t " + DECIMAL_FORMAT.format(t);
            text.drawString(str2, screenSpace.add(0, 52), Vec4.of(0, 0, 0, color.w() * 0.5), TextRenderer.FontSize.PT_11,
                            TextRenderer.Alignment.CENTERED, camera);
          }
        }
      } else if (drawPass == DrawPass.LINES) {
        floor.draw(c -> {
          c.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, 1));
          c.setUniform(Program.Uniform.TRANSFORM_MATRIX,
                       camera.getMatrix().multiply(Mat4.translate(xOff, 0, 0)).multiply(Mat4.scale(cellSize, cellSize, 1, 1)));
        });
      } else if (drawPass == DrawPass.OVERLAY) {
        drawOverlay(finalPrevFrameTime, worldTime);
      }
      if (smoothFlatten > 0) {
        renderers.forEach(r -> r.renderFlat(drawPass, camera, flatColor));
      }
      if (smoothFlatten < 1) {
        renderers.forEach(r -> r.render(drawPass, camera, color, timeAfterTick));
      }
    });
  }

  public void drawOverlay(long frameTime, double worldTime) {
    Vec2 screenSize = OGLManager.getViewportSize();
    if (showFPS) {
      textRenderer.drawString(framerateFormat.format(1E+9 / (double) frameTime), Vec2.of(10, screenSize.y() - 10), Vec4.of(0, 0, 0, 1),
                              TextRenderer.FontSize.PT_22, TextRenderer.Alignment.LEFT, camera);
    }
    double smoothFlatten = MathUtil.smoothStep(1 - flatten);
    textRenderer.drawString("t = " + timeFormat.format(worldTime), Vec2.of(screenSize.x() / 2, screenSize.y() - 32),
                            Vec4.of(0, 0, 0, 0.5f * smoothFlatten), TextRenderer.FontSize.PT_18, TextRenderer.Alignment.CENTERED, camera);
    textRenderer.drawString(playSpeed.getName(), Vec2.of(screenSize.x() - 10, screenSize.y() - 32),
                            Vec4.of(0, 0, 0, 0.5f * smoothFlatten), TextRenderer.FontSize.PT_18, TextRenderer.Alignment.RIGHT, camera);

    if (showKeybinds) {
      guiSquare.draw(setter -> {
        setter.setUniform(Program.Uniform.TRANSFORM_MATRIX,
                          Mat4.translate(-1, 1, 0).multiply(Mat4.scale(1 / screenSize.x() * 2, -1 / screenSize.y() * 2, 1, 1)));
        setter.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, 0.25));
      });
      String s = keyboard.getBindings()
                         .entrySet()
                         .stream()
                         .map(e -> String.format("'%s': %s", e.getValue().getName(), labels.getString(e.getKey().getName())))
                         .collect(Collectors.joining("\n"));
      textRenderer.drawMultilineString(s, Vec2.of(30, 20), Vec2.of(410, 210), Vec4.of(1, 1, 1, 1), TextRenderer.Alignment.JUSTIFIED,
                                       TextRenderer.FontSize.PT_14, 0, 1, 1, 1.25f, camera);
    }
  }

  public enum PlaySpeed {
    X00015625(0.015625, "x0.015625"),
    X003125(0.03125, "x0.03125"),
    X00625(0.0625, "x0.0625"),
    X0125(0.125, "x0.125"),
    X025(0.25, "x0.25"),
    X05(0.5, "x0.5"),
    X075(0.75, "x0.75"),
    X1(1, "x1"),
    X15(1.5, "x1.5"),
    X2(2, "x2"),
    X4(4, "x4"),
    X8(8, "x8"),
    X16(16, "x16"),
    X32(32, "x32"),
    X64(64, "x64"),
    X128(128, "x128");

    private static final PlaySpeed[] values = values();

    private final String name;
    private final double speedCoefficient;

    PlaySpeed(double speedCoefficient, String name) {
      this.name = name;
      this.speedCoefficient = speedCoefficient;
    }

    public String getName() {
      return name;
    }

    public double getSpeedCoefficient() {
      return speedCoefficient;
    }

    public PlaySpeed prev() {
      return ordinal() == 0 ? this : values[ordinal() - 1];
    }

    public PlaySpeed next() {
      return ordinal() == values.length - 1 ? this : values[ordinal() + 1];
    }
  }
}