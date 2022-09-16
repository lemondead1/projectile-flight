package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.OGLBufferAllocator;

public interface BufferDataBuilder {
  BufferRenderer upload(OGLBufferAllocator allocator, ModelTransformer transformer);
}