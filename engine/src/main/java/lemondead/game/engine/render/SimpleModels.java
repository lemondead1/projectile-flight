package lemondead.game.engine.render;

import lemondead.game.engine.render.buffers.*;
import lemondead.game.engine.render.ogl.OGLBufferAllocator;
import lemondead.game.engine.render.ogl.Program;
import lemondead.game.engine.render.ogl.RenderMode;

public class SimpleModels {
  public static BufferRenderer getWireframeSphere(OGLBufferAllocator allocator, float r) {
    SplineBuilder sphere = new SplineBuilder(Program.POSITION_COLOR);
    float f = (float) (Math.sqrt(2) / 2);
    sphere.beginBezier(2, 0, 1, builder -> {
      builder.position(r, 0, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(r, r, 0).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, r, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(-r, r, 0).weight(f).color(1, 1, 1, 1).end();
      builder.position(-r, 0, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(-r, -r, 0).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, -r, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(r, -r, 0).weight(f).color(1, 1, 1, 1).end();
      builder.position(r, 0, 0).weight(1).color(1, 1, 1, 1).end();
    });
    sphere.beginBezier(2, 0, 1, builder -> {
      builder.position(0, r, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(0, r, r).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, 0, r).weight(1).color(1, 1, 1, 1).end();
      builder.position(0, -r, r).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, -r, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(0, -r, -r).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, 0, -r).weight(1).color(1, 1, 1, 1).end();
      builder.position(0, r, -r).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, r, 0).weight(1).color(1, 1, 1, 1).end();
    });
    sphere.beginBezier(2, 0, 1, builder -> {
      builder.position(r, 0, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(r, 0, r).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, 0, r).weight(1).color(1, 1, 1, 1).end();
      builder.position(-r, 0, r).weight(f).color(1, 1, 1, 1).end();
      builder.position(-r, 0, 0).weight(1).color(1, 1, 1, 1).end();
      builder.position(-r, 0, -r).weight(f).color(1, 1, 1, 1).end();
      builder.position(0, 0, -r).weight(1).color(1, 1, 1, 1).end();
      builder.position(r, 0, -r).weight(f).color(1, 1, 1, 1).end();
      builder.position(r, 0, 0).weight(1).color(1, 1, 1, 1).end();
    });
    return sphere.upload(allocator, ModelTransformer.IDENTITY);
  }

  public static IndexedBuilder getCuboid(IndexedBuilder cube, float x, float y, float z, float w, float h, float d) {
    float x1 = x + w;
    float y1 = y + h;
    float z1 = z + d;
    switch (cube.getMode()) {
      case SOLID:
        cube.pos(x, y, z).color(1, 1, 1, 1).end();
        cube.pos(x, y, z1).color(1, 1, 1, 1).end();
        cube.pos(x, y1, z).color(1, 1, 1, 1).end();
        cube.pos(x, y1, z1).color(1, 1, 1, 1).end();
        cube.pos(x1, y, z).color(1, 1, 1, 1).end();
        cube.pos(x1, y, z1).color(1, 1, 1, 1).end();
        cube.pos(x1, y1, z).color(1, 1, 1, 1).end();
        cube.pos(x1, y1, z1).color(1, 1, 1, 1).end();
        cube.index(0).index(2).index(6).index(0).index(6).index(4);
        cube.index(1).index(3).index(7).index(1).index(7).index(5);
        cube.index(0).index(4).index(5).index(0).index(5).index(1);
        cube.index(2).index(6).index(7).index(2).index(7).index(3);
        cube.index(0).index(1).index(3).index(0).index(3).index(2);
        cube.index(4).index(5).index(7).index(4).index(7).index(6);
        break;
      case LINES:
        cube.pos(x, y, z).color(1, 1, 1, 1).end();
        cube.pos(x1, y, z).color(1, 1, 1, 1).end();
        cube.pos(x, y1, z).color(1, 1, 1, 1).end();
        cube.pos(x1, y1, z).color(1, 1, 1, 1).end();
        cube.pos(x, y, z1).color(1, 1, 1, 1).end();
        cube.pos(x1, y, z1).color(1, 1, 1, 1).end();
        cube.pos(x, y1, z1).color(1, 1, 1, 1).end();
        cube.pos(x1, y1, z1).color(1, 1, 1, 1).end();
        cube.index(0).index(1);
        cube.index(0).index(2);
        cube.index(0).index(4);
        cube.index(1).index(3);
        cube.index(1).index(5);
        cube.index(2).index(3);
        cube.index(2).index(6);
        cube.index(3).index(7);
        cube.index(4).index(5);
        cube.index(4).index(6);
        cube.index(5).index(7);
        cube.index(6).index(7);
        break;
      default:
        throw new NullPointerException();
    }
    return cube;
  }

  public static BufferRenderer getSquare(OGLBufferAllocator allocator, float x, float y, float z, float w, float h) {
    SimpleBuilder dataBuilder = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR, 4);
    dataBuilder.startIndexed(builder -> {
      builder.pos(x, y, z).color(1, 1, 1, 1).end();
      builder.pos(x + w, y, z).color(1, 1, 1, 1).end();
      builder.pos(x + w, y + h, z).color(1, 1, 1, 1).end();
      builder.pos(x, y + h, z).color(1, 1, 1, 1).end();
      builder.index(0).index(1).index(1).index(2).index(2).index(3).index(0);
    });
    return dataBuilder.upload(allocator, ModelTransformer.IDENTITY);
  }

  public static BufferRenderer getCircleModel(OGLBufferAllocator allocator, float xCenter, float yCenter, float radius,
                                              int subdivision, float r, float g, float b, float a) {
    SimpleBuilder dataBuilder = new SimpleBuilder(RenderMode.SOLID, Program.POSITION_COLOR);
    dataBuilder.startFan(2, builder -> {
      for (int i = 0; i <= subdivision; i++) {
        float x = (float) Math.cos(Math.PI * 2 / subdivision * i) * radius;
        float y = (float) Math.sin(Math.PI * 2 / subdivision * i) * radius;
        builder.pos(xCenter + x, yCenter + y, 0).color(r, g, b, a).end();
      }
    });
    return dataBuilder.upload(allocator, ModelTransformer.IDENTITY);
  }
}
