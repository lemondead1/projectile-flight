package lemondead.projectileflight;

import lemondead.game.engine.util.AbstractKeyboardInputManager;
import lemondead.game.engine.util.WindowWrapper;
import lemondead.projectileflight.level.objects.Cannon;
import lemondead.projectileflight.level.objects.Wall;

import java.util.function.BiConsumer;

public class KeyboardInputManager extends AbstractKeyboardInputManager {
  public static final KeyBinding SNAP = new KeyBinding("binding.snap");
  public static final KeyBinding CREATE_CANNON = new KeyBinding(() -> {
    if (Main.GAME_INSTANCE.isInEditMode()) {
      Main.GAME_INSTANCE.addLevelObjectAtMouse(new Cannon());
    }
  }, "binding.create_cannon");
  public static final KeyBinding CREATE_WALL = new KeyBinding(() -> {
    if (Main.GAME_INSTANCE.isInEditMode()) {
      Main.GAME_INSTANCE.addLevelObjectAtMouse(new Wall());
    }
  }, "binding.create_wall");
  public static final KeyBinding DELETE_OBJECT = new KeyBinding(() -> {
    if (Main.GAME_INSTANCE.isInEditMode()) {
      Main.GAME_INSTANCE.removeSelectedObject();
    }
  }, "binding.delete_object");
  public static final KeyBinding PLAY_PAUSE = new KeyBinding(() -> {
    if (Main.GAME_INSTANCE.isInShowMode()) {
      Main.GAME_INSTANCE.togglePause();
    }
  }, "binding.play_pause");
  public static final KeyBinding SPEED_UP_PLAYBACK =
      new KeyBinding(() -> Main.GAME_INSTANCE.setPlaySpeedAndRescheduleWorldUpdate(Main.GAME_INSTANCE.playSpeed.next()), "binding.speed_up_playback");
  public static final KeyBinding SLOW_DOWN_PLAYBACK =
      new KeyBinding(() -> Main.GAME_INSTANCE.setPlaySpeedAndRescheduleWorldUpdate(Main.GAME_INSTANCE.playSpeed.prev()),
                     "binding.slow_down_playback");
  public static final KeyBinding RESET = new KeyBinding(() -> {
    if (Main.GAME_INSTANCE.isInShowMode()) {
      Main.GAME_INSTANCE.resetWorld();
    }
  }, "binding.reset");
  public static final KeyBinding CHANGE_MODE = new KeyBinding(() -> Main.GAME_INSTANCE.startModeChange(), "binding.change_mode");
  public static final KeyBinding SHOW_COLLISIONS = new KeyBinding("binding.show_collisions");
  public static final KeyBinding SHOW_VELOCITIES = new KeyBinding("binding.show_velocities");
  public static final KeyBinding SHOW_KEYBINDS = new KeyBinding(() -> Main.GAME_INSTANCE.showKeybinds ^= true, "binding.show_keybinds");
  public static final KeyBinding RESET_CAMERA = new KeyBinding(() -> Main.GAME_INSTANCE.moveCameraToOrigin(), "binding.move_camera_to_origin");

  public KeyboardInputManager(WindowWrapper window) {
    super(window, Main.GAME_INSTANCE.configuration);
  }

  @Override
  protected void init(BiConsumer<KeyBinding, Key> consumer) {
    consumer.accept(SNAP, Key.LEFT_SHIFT);
    consumer.accept(CREATE_CANNON, Key.C);
    consumer.accept(CREATE_WALL, Key.W);
    consumer.accept(DELETE_OBJECT, Key.DELETE);
    consumer.accept(PLAY_PAUSE, Key.SPACE);
    consumer.accept(SPEED_UP_PLAYBACK, Key.RIGHT);
    consumer.accept(SLOW_DOWN_PLAYBACK, Key.LEFT);
    consumer.accept(RESET, Key.R);
    consumer.accept(CHANGE_MODE, Key.ENTER);
    consumer.accept(SHOW_COLLISIONS, Key.H);
    consumer.accept(SHOW_VELOCITIES, Key.V);
    consumer.accept(SHOW_KEYBINDS, Key.ESCAPE);
    consumer.accept(RESET_CAMERA, Key.M);
  }
}
