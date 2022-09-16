package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.OGLBufferAllocator;
import lemondead.game.engine.render.ogl.Program;
import lemondead.game.engine.render.ogl.RenderMode;
import lemondead.game.engine.util.GeomUtil;
import lemondead.game.engine.util.vector.Vec3;
import lemondead.game.engine.util.vector.Vec4;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SimpleBuilder implements BufferDataBuilder {
  private final IndexedBuilder wrapped;
  private final int primSize;
  private int indexedVertices = 0;

  private boolean isBuilding = false;

  public SimpleBuilder(RenderMode mode, Program program) {
    wrapped = new IndexedBuilder(mode, program);
    primSize = mode.getVerticesPerPrimitive();
  }

  public SimpleBuilder(RenderMode mode, Program program, int initialVertexCount) {
    wrapped = new IndexedBuilder(mode, program, initialVertexCount,
                                 initialVertexCount * 2);
    primSize = mode.getVerticesPerPrimitive();
  }

  private void checkBuilding() {
    if (isBuilding) {
      throw new IllegalStateException("Already building.");
    }
  }

  public void startVertices(Consumer<Vertex> builder) {
    checkBuilding();
    isBuilding = true;
    Vertex vertex = new Vertex();
    builder.accept(vertex);
    vertex.endBuilder();
  }

  public void startStrip(int commonVertices, Consumer<Strip> builder) {
    checkBuilding();
    isBuilding = true;
    Strip strip = new Strip(commonVertices);
    builder.accept(strip);
    strip.endBuilder();
  }

  public void startFan(int commonVertices, Consumer<Fan> builder) {
    checkBuilding();
    isBuilding = true;
    Fan fan = new Fan(commonVertices);
    builder.accept(fan);
    fan.endBuilder();
  }

  public void startPolygon(Consumer<Polygon> builder) {
    checkBuilding();
    isBuilding = true;
    Polygon polygon = new Polygon();
    builder.accept(polygon);
    polygon.endBuilder();
  }

  public void startIndexed(Consumer<Indexed> builder) {
    checkBuilding();
    isBuilding = true;
    Indexed indexed = new Indexed();
    builder.accept(indexed);
    indexed.endBuilder();
  }


  @Override
  public BufferRenderer upload(OGLBufferAllocator allocator, ModelTransformer transformer) {
    return wrapped.upload(allocator, transformer);
  }

  public class Vertex {
    private boolean closed = false;

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("The builder has already been closed.");
      }
    }

    public Vertex pos(float x, float y, float z) {
      checkClosed();
      wrapped.pos(x, y, z);
      return this;
    }

    public Vertex color(float red, float green, float blue, float alpha) {
      checkClosed();
      wrapped.color(red, green, blue, alpha);
      return this;
    }

    public Vertex texPos(float u, float v) {
      checkClosed();
      wrapped.texPos(u, v);
      return this;
    }

    public void end() {
      checkClosed();
      wrapped.end();
      wrapped.index(indexedVertices++);
    }

    private void endBuilder() {
      checkClosed();
      closed = true;
      isBuilding = false;
    }
  }

  public class Indexed {
    private boolean closed = false;
    private int vertexCount = 0;

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("The builder has already been closed.");
      }
    }

    public Indexed pos(float x, float y, float z) {
      checkClosed();
      wrapped.pos(x, y, z);
      return this;
    }

    public Indexed color(float red, float green, float blue, float alpha) {
      checkClosed();
      wrapped.color(red, green, blue, alpha);
      return this;
    }

    public Indexed texPos(float u, float v) {
      checkClosed();
      wrapped.texPos(u, v);
      return this;
    }

    public Indexed index(int index) {
      checkClosed();
      wrapped.index(index + indexedVertices);
      return this;
    }

    public void end() {
      checkClosed();
      wrapped.end();
      vertexCount++;
    }

    private void endBuilder() {
      checkClosed();
      indexedVertices += vertexCount;
      closed = true;
      isBuilding = false;
    }
  }

  public class Strip {
    private final int primCommonVertices;
    int vertexCount = 0;
    private boolean closed = false;

    private Strip(int primCommonVertices) {
      if (primCommonVertices >= primSize) {
        throw new IllegalArgumentException(
            "Primitive common vertices count cannot be higher than count of vertices in primitive.");
      }
      this.primCommonVertices = primCommonVertices;
    }

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("The builder has already been closed.");
      }
    }

    public Strip pos(float x, float y, float z) {
      checkClosed();
      wrapped.pos(x, y, z);
      return this;
    }

    public Strip color(float red, float green, float blue, float alpha) {
      checkClosed();
      wrapped.color(red, green, blue, alpha);
      return this;
    }

    public Strip texPos(float u, float v) {
      checkClosed();
      wrapped.texPos(u, v);
      return this;
    }

    public void end() {
      checkClosed();
      wrapped.end();
      vertexCount++;
    }

    private void endBuilder() {
      checkClosed();
      closed = true;
      int verticesInStrip = vertexCount;
      if ((verticesInStrip - primCommonVertices) % (primSize - primCommonVertices) != 0) {
        throw new IllegalStateException("Not enough vertices for a strip.");
      }
      int primCount = (verticesInStrip - primCommonVertices) / (primSize - primCommonVertices);
      for (int i = 0; i < primCount; i++) {
        for (int l = 0; l < primSize; l++) {
          wrapped.index(indexedVertices + i * (primSize - primCommonVertices) + l);
        }
      }
      indexedVertices += verticesInStrip;
      isBuilding = false;
    }
  }

  public class Fan {
    private final int primCommonVertices;
    int vertexCount = 0;
    private boolean closed = false;

    private Fan(int primCommonVertices) {
      if (primCommonVertices >= primSize) {
        throw new IllegalArgumentException(
            "Primitive common vertices count cannot be higher than count of vertices in primitive.");
      }
      this.primCommonVertices = primCommonVertices;
    }

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("The builder has already been closed.");
      }
    }

    public Fan pos(float x, float y, float z) {
      checkClosed();
      wrapped.pos(x, y, z);
      return this;
    }

    public Fan pos(Vec3 pos) {
      return this.pos((float) pos.x(), (float) pos.y(), (float) pos.z());
    }

    public Fan color(float red, float green, float blue, float alpha) {
      checkClosed();
      wrapped.color(red, green, blue, alpha);
      return this;
    }

    public Fan color(Vec4 color) {
      return color((float) color.x(), (float) color.y(), (float) color.z(), (float) color.w());
    }

    public Fan texPos(float u, float v) {
      checkClosed();
      wrapped.texPos(u, v);
      return this;
    }

    public void end() {
      checkClosed();
      wrapped.end();
      vertexCount++;
    }

    private void endBuilder() {
      checkClosed();
      closed = true;
      int verticesInStrip = vertexCount;
      if ((verticesInStrip - primCommonVertices) % (primSize - primCommonVertices) != 0) {
        throw new IllegalStateException("Not enough vertices for a fan.");
      }
      int primCount = (verticesInStrip - primCommonVertices) / (primSize - primCommonVertices);
      for (int i = 0; i < primCount; i++) {
        wrapped.index(indexedVertices);
        for (int l = 1; l < primSize; l++) {
          wrapped.index(indexedVertices + i * (primSize - primCommonVertices) + l);
        }
      }
      indexedVertices += verticesInStrip;
      isBuilding = false;
    }
  }

  public class Polygon {
    private final List<Vec3> posList = new ArrayList<>();
    private Vec3 current = Vec3.ZERO;
    private boolean closed = false;

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("The builder has already been closed.");
      }
    }

    public Polygon pos(float x, float y, float z) {
      checkClosed();
      wrapped.pos(x, y, z);
      current = Vec3.of(x, y, z);
      return this;
    }

    public Polygon color(float red, float green, float blue, float alpha) {
      checkClosed();
      wrapped.color(red, green, blue, alpha);
      return this;
    }

    public Polygon texPos(float u, float v) {
      checkClosed();
      wrapped.texPos(u, v);
      return this;
    }

    public void end() {
      checkClosed();
      wrapped.end();
      posList.add(current);
    }

    private void endBuilder() {
      checkClosed();
      closed = true;
      int vertices = indexedVertices;
      if (posList.size() == 3) {
        wrapped.index(vertices).index(vertices + 1).index(vertices + 2);
      } else if (posList.size() == 4) {
        wrapped.index(vertices).index(vertices + 1).index(vertices + 2)
               .index(vertices).index(vertices + 2).index(vertices + 3);
      } else {
        int[] indices = GeomUtil.triangulate(posList);
        for (int index : indices) {
          wrapped.index(vertices + index);
        }
      }
      indexedVertices += posList.size();
      isBuilding = false;
    }
  }
}
