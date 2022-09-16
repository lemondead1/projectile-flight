package lemondead.game.engine.render.ogl;

import org.lwjgl.opengl.GL20C;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.lwjgl.opengl.GL20.*;

public class ShaderUtils {
  private static final Logger logger = Logger.getLogger("Shader utils");

  public static int createProgram(String name, int... shaders) {
    int program = glCreateProgram();
    for (int shader : shaders) {
      glAttachShader(program, shader);
    }
    glLinkProgram(program);
    for (int shader : shaders) {
      GL20C.glDetachShader(program, shader);
    }
    String log = glGetProgramInfoLog(program);
    boolean successful = glGetProgrami(program, GL_LINK_STATUS) != GL_FALSE;
    if (successful && log.length() > 0) {
      logger.log(Level.WARNING, "A minor error occurred while linking program '" + name + "':\n" + log);
    } else if (!successful) {
      throw new IllegalStateException("A severe error occurred while linking program '" + name + "':\n" + log);
    }
    return program;
  }

  public static int loadVertexShader(String directory) {
    return loadShader(directory, GL20C.GL_VERTEX_SHADER);
  }

  public static int loadFragmentShader(String directory) {
    return loadShader(directory, GL20C.GL_FRAGMENT_SHADER);
  }

  public static int loadShader(String directory, int type) {
    int i = GL20C.glCreateShader(type);
    loadAndCompileShader(i, directory);
    return i;
  }

  private static void loadAndCompileShader(int shader, String directory) {
    try (InputStream stream = ClassLoader.getSystemResourceAsStream("shaders/" + directory)) {
      if (stream == null) {
        throw new IllegalStateException("Could not find shader at directory shaders/" + directory);
      }
      InputStreamReader reader = new InputStreamReader(stream);
      StringBuilder builder = new StringBuilder();
      char[] chars = new char[100];
      while (true) {
        int i = reader.read(chars);
        if (i == -1) {
          break;
        }
        builder.append(chars, 0, i);
      }
      GL20C.glShaderSource(shader, builder.toString());
      GL20C.glCompileShader(shader);

      boolean successful = glGetShaderi(shader, GL_COMPILE_STATUS) != GL_FALSE;
      String log = glGetShaderInfoLog(shader);
      if (successful && log.length() > 0) {
        logger.log(Level.SEVERE,
                   "A minor error occurred while compiling shader at directory shaders/" + directory + ":\n" + log);
      } else if (!successful) {
        throw new IllegalStateException(
            "A severe error occurred while compiling shader at directory shaders/" + directory + ":\n" + log);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Could not load shader with directory shaders/" + directory, e);
    }
  }
}
