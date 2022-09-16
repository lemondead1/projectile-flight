package lemondead.projectileflight;

import java.util.logging.Logger;

public class Main {
  public static final Logger logger = Logger.getLogger("ProjectileFlight");

  public static ProjectileFlight GAME_INSTANCE;

  public static final long START_TIME = System.nanoTime();

  public static void main(String[] args) {
    GAME_INSTANCE = new ProjectileFlight();
    GAME_INSTANCE.run();
  }
}