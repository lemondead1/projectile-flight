package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.Program;

public interface UniformSetter {
  <T> void setUniform(Program.Uniform<T> uniform, T value);
}
