package lemondead.game.engine.render.buffers;

import java.io.Closeable;
import java.util.function.Consumer;

public interface BufferRenderer extends Closeable {
  void draw(Consumer<UniformSetter> uniformSetter);

  @Override
  void close();
}
