package lemondead.game.engine.render.buffers;

import lemondead.game.engine.render.ogl.*;
import lemondead.game.engine.util.MathUtil;
import lemondead.game.engine.util.SplineUtil;
import lemondead.game.engine.util.Util;
import lemondead.game.engine.util.vector.Vec3;
import org.lwjgl.opengl.GL20C;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public final class SplineBuilder implements BufferDataBuilder {
  private static final int detailLevels = 3;

  private static final float[] fEmpty = { };

  private final int vertexValueCount;
  private final Program program;
  private final RenderMode mode = RenderMode.LINES;

  private final List<Spline> splines = new ArrayList<>();

  private boolean isBuilding = false;

  private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
  private float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

  public SplineBuilder(Program program) {
    this.program = program;
    vertexValueCount = Arrays.stream(program.getVertexFormat().getAttributes()).mapToInt(VertexFormat.VertexAttribute::getValueCount).sum();
  }

  private void checkBuilding() {
    if (isBuilding) {
      throw new IllegalStateException("Already building.");
    }
  }

  public void beginNurbs(float[] knot, int degree, float start, float end, Consumer<NURBS> builder) {
    checkBuilding();
    isBuilding = true;
    NURBS nurbs = new NURBS(knot, degree, start, end);
    builder.accept(nurbs);
    nurbs.closed = true;
    splines.add(nurbs.createSpline());
    isBuilding = false;
  }

  public void beginBezier(int degree, float start, float end, Consumer<Bezier> builder) {
    checkBuilding();
    isBuilding = true;
    Bezier bezier = new Bezier(degree, start, end);
    builder.accept(bezier);
    bezier.closed = true;
    splines.add(bezier.createSpline());
    isBuilding = false;
  }

  public void beginCardinal(float t, float start, float end, Consumer<Cardinal> builder) {
    checkBuilding();
    isBuilding = true;
    Cardinal cardinal = new Cardinal(t, start, end);
    builder.accept(cardinal);
    cardinal.closed = true;
    splines.add(cardinal.createSpline());
    isBuilding = false;
  }


  @Override
  public BufferRenderer upload(OGLBufferAllocator alloc, ModelTransformer transformer) {
    splines.forEach(s -> s.prepareForTessellation(transformer, program.getVertexFormat()));
    int vertexCount = splines.stream().mapToInt(Spline::getDataVertexCount).sum();
    int indexCount = splines.stream().flatMapToInt(s -> IntStream.range(0, detailLevels).map(s::getIndexCount)).sum();
    if (vertexCount == 0 || indexCount == 0) {
      return new BufferRenderer() {
        @Override
        public void close() {
        }

        @Override
        public void draw(Consumer<UniformSetter> uniformSetter) {
        }
      };
    }

    int vertexSize = program.getVertexFormat().getVertexByteSize();
    OGLBuffer vertexBuffer = alloc.create(vertexCount * vertexSize);

    try (OGLBuffer.MappedBuffer mappedBuffer = vertexBuffer.map(GL20C.GL_WRITE_ONLY)) {
      splines.forEach(s -> s.generateData(program, mappedBuffer.getBuffer(), transformer));
    }

    ValueType indexType = vertexCount < 1 << 16 ? ValueType.UNSIGNED_SHORT : ValueType.UNSIGNED_INT;
    OGLBuffer indexBuffer = alloc.create(indexCount * indexType.getByteSize());

    int[] lods = new int[detailLevels + 1];

    try (OGLBuffer.MappedBuffer mappedBuffer = indexBuffer.map(GL20C.GL_WRITE_ONLY)) {
      ByteBuffer indexData = mappedBuffer.getBuffer();
      IntConsumer indexConsumer = indexType == ValueType.UNSIGNED_SHORT ? i -> indexData.putShort((short) i) : indexData::putInt;

      for (int level = 0; level < detailLevels; level++) {
        lods[level + 1] = lods[level];
        int indexOffset = 0;
        for (Spline s : splines) {
          lods[level + 1] += s.getIndexCount(level);
          s.generateIndices(indexConsumer, indexOffset, level);
          indexOffset += s.getDataVertexCount();
        }
      }
    }

    float[] minPos = new float[program.getVertexFormat().getVertexValueCount()];
    float[] maxPos = new float[program.getVertexFormat().getVertexValueCount()];

    int off = program.getVertexFormat().getOffset(VertexFormat.VertexAttribute.POSITION);

    minPos[off] = minX;
    minPos[off + 1] = minY;
    minPos[off + 2] = minZ;

    maxPos[off] = maxX;
    maxPos[off + 1] = maxY;
    maxPos[off + 2] = maxZ;

    transformer.transform(minPos, program.getVertexFormat());
    transformer.transform(maxPos, program.getVertexFormat());

    return new TessellatedRenderer(vertexBuffer, indexBuffer, lods,
                                   new Vec3[] { Vec3.of(minPos[off], minPos[off + 1], 0), Vec3.of(maxPos[off], maxPos[off + 1], 0) },
                                   indexType, program, mode);
  }

  private static int getMaxSubdivision(float length) {
    if (length > 32) {
      return 11;
    } else if (length > 16) {
      return 10;
    } else if (length > 8) {
      return 9;
    } else if (length > 4) {
      return 8;
    } else if (length > 2) {
      return 7;
    } else if (length > 1) {
      return 6;
    } else if (length > 0.5) {
      return 5;
    }
    return 4;
  }

  private static class Spline {
    private final float[] data;
    private final float[] transformedData;
    private final float[] weights;
    private final int dimensionCount;
    private final float[] knot;
    private final int degree;
    private final float start;
    private final float end;

    private Spline(float[] data, float[] weights, int vertexCount, int dimensionCount, float[] knot, int degree,
                   float start, float end) {
      this.dimensionCount = dimensionCount;
      if (vertexCount <= degree) {
        throw new IllegalStateException("Not enough points to construct a curve.");
      }
      if (knot.length != vertexCount + degree + 1) {
        throw new IllegalArgumentException("Number of knots must be equal to number of control points + degree + 1.");
      }
      if (!Util.isSorted(knot)) {
        throw new IllegalArgumentException("Knot vector must be in nondecreasing order.");
      }
      if (start < knot[0] || end > knot[knot.length - 1]) {
        throw new IllegalArgumentException("Start and end values exceed knot vector boundaries.");
      }
      this.data = data;
      transformedData = new float[data.length];
      this.weights = weights;
      this.knot = knot;
      this.degree = degree;
      this.start = start;
      this.end = end;
    }

    private static Spline bezier(float[] data, float[] weights, int vertexCount, int dimensionCount, int degree,
                                 float start, float end) {
      if (vertexCount == 0 || (vertexCount - 1) % degree != 0) {
        throw new IllegalStateException("Not enough control points for a bezier spline.");
      }

      float[] knot = new float[vertexCount + degree + 1];
      int partCount = (vertexCount - 1) / degree;
      for (int i = 1; i < partCount; i++) {
        Arrays.fill(knot, i * degree + 1, (i + 1) * degree + 1, 1f * i / partCount);
      }
      Arrays.fill(knot, vertexCount, vertexCount + degree + 1, 1);

      return new Spline(data, weights, vertexCount, dimensionCount, knot, degree, start, end);
    }

    public static Spline cardinal(float t, float[] data, int vertexCount, int dimensionCount, float start, float end) {
      int newVertexCount = (vertexCount - 3) * 3 + 1;
      float[] newData = new float[newVertexCount * dimensionCount];

      for (int i = 1; i < vertexCount - 2; i++) {
        for (int dim = 0; dim < dimensionCount; dim++) {
          float left = data[(i - 1) * dimensionCount + dim];
          float current = data[i * dimensionCount + dim];
          float right = data[(i + 1) * dimensionCount + dim];

          float normal = (right - left) * (1 - t);

          if (i != 1) {
            newData[(i * 3 - 1) * dimensionCount + dim] = current - normal;
          }
          newData[i * 3 * dimensionCount + dim] = current;
          if (i != vertexCount - 2) {
            newData[(i * 3 + 1) * dimensionCount + dim] = current - normal;
          }
        }
      }

      float[] weights = new float[newVertexCount];
      Arrays.fill(weights, 1);

      return bezier(newData, weights, newVertexCount, dimensionCount, 3, start, end);
    }

    public void evaluate(float[] result, float u) {
      SplineUtil.evalNURBS(u, transformedData, weights, dimensionCount, knot, degree, result);
    }

    private int[] spanSubdivisions;
    private float[] spans;

    public void prepareForTessellation(ModelTransformer transformer, VertexFormat format) {
      float[] spans = new float[10];
      int spanCount = 0;
      float prev = start - 1;
      for (float d : knot) {
        float clamp = MathUtil.clamp(start, d, end);
        if (clamp != prev) {
          if (spanCount >= spans.length) {
            spans = Arrays.copyOf(spans, spans.length * 3 / 2);
          }
          spans[spanCount++] = clamp;
          prev = clamp;
        }
      }
      int vertexValueCount = format.getVertexValueCount();
      float[] vertex = new float[vertexValueCount];
      for (int i = 0; i < data.length; i += vertexValueCount) {
        System.arraycopy(data, i, vertex, 0, vertexValueCount);
        transformer.transform(vertex, format);
        System.arraycopy(vertex, 0, transformedData, i, vertexValueCount);
      }
      this.spans = Arrays.copyOfRange(spans, 0, spanCount);
      spanSubdivisions = new int[spanCount - 1];
      for (int i = 0; i < spanCount - 1; i++) {
        float start = spans[i];
        float end = spans[i + 1];

        float f = MathUtil.lerp(start, 1 / 3f, end);
        float f1 = MathUtil.lerp(start, 2 / 3f, end);

        float dis = 0;

        float x;
        float y;
        float z;

        float[] vector = new float[dimensionCount];
        evaluate(vector, start);
        x = vector[0];
        y = vector[1];
        z = vector[2];
        evaluate(vector, f);
        dis += Math.sqrt(Math.pow(x - vector[0], 2) + Math.pow(y - vector[1], 2) + Math.pow(z - vector[2], 2));
        x = vector[0];
        y = vector[1];
        z = vector[2];
        evaluate(vector, f1);
        dis += Math.sqrt(Math.pow(x - vector[0], 2) + Math.pow(y - vector[1], 2) + Math.pow(z - vector[2], 2));
        x = vector[0];
        y = vector[1];
        z = vector[2];
        evaluate(vector, end);
        dis += Math.sqrt(Math.pow(x - vector[0], 2) + Math.pow(y - vector[1], 2) + Math.pow(z - vector[2], 2));

        spanSubdivisions[i] = getMaxSubdivision(dis);
      }
    }

    public int getIndexCount(int geometryReduction) {
      int sum = 0;
      for (int i : spanSubdivisions) {
        sum += 1 << Math.max(0, i - geometryReduction);
      }
      return sum * 2;
    }

    public void generateIndices(IntConsumer buffer, int indexOffset, int geometryReduction) {
      int prevValue = indexOffset;
      for (int spanSubdiv : spanSubdivisions) {
        int r = Math.min(geometryReduction, spanSubdiv);
        int reducedVertices = 1 << (spanSubdiv - geometryReduction);
        int skipVertices = 1 << r;
        for (int l = 0; l < reducedVertices; l++) {
          buffer.accept(prevValue);
          prevValue += skipVertices;
          buffer.accept(prevValue);
        }
      }
    }

    public int getDataVertexCount() {
      int sum = 1;
      for (int i : spanSubdivisions) {
        sum += 1 << i;
      }
      return sum;
    }

    public void generateData(Program program, ByteBuffer buffer, ModelTransformer transformer) {
      float[] vertex = new float[dimensionCount];
      evaluate(vertex, spans[0]);
      putVertex(buffer, program, vertex);
      for (int i = 0; i < spanSubdivisions.length; i++) {
        float start = spans[i];
        float end = spans[i + 1];
        int count = 1 << spanSubdivisions[i];
        for (int l = 1; l <= count; l++) {
          float f = MathUtil.lerp(start, l / (float) count, end);
          evaluate(vertex, f);
          putVertex(buffer, program, vertex);
        }
      }
    }

    private void putVertex(ByteBuffer buffer, Program program, float[] vertex) {
      int valueCount = 0;
      for (VertexFormat.VertexAttribute a : program.getVertexFormat().getAttributes()) {
        switch (a) {
          case POSITION:
            buffer.putFloat(vertex[valueCount]).putFloat(vertex[valueCount + 1]).putFloat(vertex[valueCount + 2]);
            break;
          case TEXTURE_POS:
            final float f = (1 << 16) - 1;
            buffer.putShort((short) (vertex[valueCount] * f)).putShort((short) (vertex[valueCount + 1] * f));
            break;
          case COLOR:
            final float j = (1 << 8) - 1;
            buffer
                .put((byte) (vertex[valueCount] * j))
                .put((byte) (vertex[valueCount + 1] * j))
                .put((byte) (vertex[valueCount + 2] * j))
                .put((byte) (vertex[valueCount + 3] * j));
            break;
        }
        valueCount += a.getValueCount();
      }
    }
  }

  public class NURBS {
    private float[] data = fEmpty;
    private float[] weights = fEmpty;
    private int vertexCount = 0;
    private int allocatedVertices = 0;
    private final float[] knot;
    private final int degree;
    private final float start;
    private final float end;
    private boolean closed = false;

    private NURBS(float[] knot, int degree, float start, float end) {
      this.knot = knot;
      this.degree = degree;
      this.start = start;
      this.end = end;
    }

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("Builder has already been closed.");
      }
    }

    private void ensureCapacity(int newVertexCount) {
      if (newVertexCount > allocatedVertices) {
        int newCount = MathUtil.max(newVertexCount, 4, allocatedVertices * 3 / 2);
        data = Arrays.copyOf(data, newCount * vertexValueCount);
        int oldCount = weights.length;
        weights = Arrays.copyOf(weights, newCount);
        Arrays.fill(weights, oldCount, newCount, 1);
        allocatedVertices = newCount;
      }
    }

    public NURBS weight(float w) {
      checkClosed();
      ensureCapacity(vertexCount + 1);
      weights[vertexCount] = w;
      return this;
    }

    public NURBS position(float x, float y, float z) {
      checkClosed();
      minX = Math.min(x, minX);
      minY = Math.min(y, minY);
      minZ = Math.min(z, minZ);
      maxX = Math.max(x, maxX);
      maxY = Math.max(y, maxY);
      maxZ = Math.max(z, maxZ);
      ensureCapacity(vertexCount + 1);
      int offset = vertexCount * vertexValueCount;
      for (VertexFormat.VertexAttribute vertexAttribute : program.getVertexFormat().getAttributes()) {
        if (vertexAttribute == VertexFormat.VertexAttribute.POSITION) {
          data[offset] = x;
          data[offset + 1] = y;
          data[offset + 2] = z;
          return this;
        }
        offset += vertexAttribute.getValueCount();
      }
      throw new IllegalArgumentException(
          "Vertex attribute " + VertexFormat.VertexAttribute.POSITION + " is not present in the render mode.");
    }

    public NURBS color(float r, float g, float b, float a) {
      checkClosed();
      ensureCapacity(vertexCount + 1);
      int offset = vertexCount * vertexValueCount;
      for (VertexFormat.VertexAttribute vertexAttribute : program.getVertexFormat().getAttributes()) {
        if (vertexAttribute == VertexFormat.VertexAttribute.COLOR) {
          data[offset] = r;
          data[offset + 1] = g;
          data[offset + 2] = b;
          data[offset + 3] = a;
          return this;
        }
        offset += vertexAttribute.getValueCount();
      }
      throw new IllegalArgumentException(
          "Vertex attribute " + VertexFormat.VertexAttribute.COLOR + " is not present in the render mode.");
    }

    public void end() {
      checkClosed();
      vertexCount++;
    }

    private Spline createSpline() {
      return new Spline(Arrays.copyOfRange(data, 0, vertexCount * vertexValueCount),
                        Arrays.copyOfRange(weights, 0, vertexCount), vertexCount, allocatedVertices, knot,
                        degree, start, end);
    }
  }

  public class Bezier {
    private float[] data = fEmpty;
    private float[] weights = fEmpty;
    private int vertexCount = 0;
    private int allocatedVertices = 0;
    private final int degree;
    private final float start;
    private final float end;
    private boolean closed = false;

    private Bezier(int degree, float start, float end) {
      this.degree = degree;
      this.start = start;
      this.end = end;
    }

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("Builder has already been closed.");
      }
    }

    private void ensureCapacity(int newVertexCount) {
      if (newVertexCount > allocatedVertices) {
        int newCount = MathUtil.max(newVertexCount, 4, allocatedVertices * 3 / 2);
        data = Arrays.copyOf(data, newCount * vertexValueCount);
        int oldCount = weights.length;
        weights = Arrays.copyOf(weights, newCount);
        Arrays.fill(weights, oldCount, newCount, 1);
        allocatedVertices = newCount;
      }
    }

    public Bezier weight(float w) {
      checkClosed();
      ensureCapacity(vertexCount + 1);
      weights[vertexCount] = w;
      return this;
    }

    public Bezier position(float x, float y, float z) {
      checkClosed();
      minX = Math.min(x, minX);
      minY = Math.min(y, minY);
      minZ = Math.min(z, minZ);
      maxX = Math.max(x, maxX);
      maxY = Math.max(y, maxY);
      maxZ = Math.max(z, maxZ);
      ensureCapacity(vertexCount + 1);
      int offset = vertexCount * vertexValueCount;
      for (VertexFormat.VertexAttribute vertexAttribute : program.getVertexFormat().getAttributes()) {
        if (vertexAttribute == VertexFormat.VertexAttribute.POSITION) {
          data[offset] = x;
          data[offset + 1] = y;
          data[offset + 2] = z;
          return this;
        }
        offset += vertexAttribute.getValueCount();
      }
      throw new IllegalArgumentException(
          "Vertex attribute " + VertexFormat.VertexAttribute.POSITION + " is not present in the render mode.");
    }

    public Bezier color(float r, float g, float b, float a) {
      checkClosed();
      ensureCapacity(vertexCount + 1);
      int offset = vertexCount * vertexValueCount;
      for (VertexFormat.VertexAttribute vertexAttribute : program.getVertexFormat().getAttributes()) {
        if (vertexAttribute == VertexFormat.VertexAttribute.COLOR) {
          data[offset] = r;
          data[offset + 1] = g;
          data[offset + 2] = b;
          data[offset + 3] = a;
          return this;
        }
        offset += vertexAttribute.getValueCount();
      }
      throw new IllegalArgumentException(
          "Vertex attribute " + VertexFormat.VertexAttribute.COLOR + " is not present in the render mode.");
    }

    public void end() {
      checkClosed();
      vertexCount++;
    }

    private Spline createSpline() {
      return Spline.bezier(Arrays.copyOfRange(data, 0, vertexCount * vertexValueCount),
                           Arrays.copyOfRange(weights, 0, vertexCount), vertexCount, vertexValueCount, degree,
                           start, end);
    }
  }

  public class Cardinal {
    private float[] data = fEmpty;
    private int vertexCount = 0;
    private int allocatedVertices = 0;
    private final float start;
    private final float end;
    private final float t;
    private boolean closed = false;

    private Cardinal(float start, float end, float t) {
      this.start = start;
      this.end = end;
      this.t = t;
    }

    private void checkClosed() {
      if (closed) {
        throw new IllegalStateException("Builder has already been closed.");
      }
    }

    private void ensureCapacity(int newVertexCount) {
      if (newVertexCount > allocatedVertices) {
        int newCount = MathUtil.max(newVertexCount, 4, allocatedVertices * 3 / 2);
        data = Arrays.copyOf(data, newCount * vertexValueCount);
        allocatedVertices = newCount;
      }
    }

    public Cardinal position(float x, float y, float z) {
      checkClosed();
      minX = Math.min(x, minX);
      minY = Math.min(y, minY);
      minZ = Math.min(z, minZ);
      maxX = Math.max(x, maxX);
      maxY = Math.max(y, maxY);
      maxZ = Math.max(z, maxZ);
      ensureCapacity(vertexCount + 1);
      int offset = vertexCount * vertexValueCount;
      for (VertexFormat.VertexAttribute vertexAttribute : program.getVertexFormat().getAttributes()) {
        if (vertexAttribute == VertexFormat.VertexAttribute.POSITION) {
          data[offset] = x;
          data[offset + 1] = y;
          data[offset + 2] = z;
          return this;
        }
        offset += vertexAttribute.getValueCount();
      }
      throw new IllegalArgumentException(
          "Vertex attribute " + VertexFormat.VertexAttribute.POSITION + " is not present in the render mode.");
    }

    public Cardinal color(float r, float g, float b, float a) {
      checkClosed();
      ensureCapacity(vertexCount + 1);
      int offset = vertexCount * vertexValueCount;
      for (VertexFormat.VertexAttribute vertexAttribute : program.getVertexFormat().getAttributes()) {
        if (vertexAttribute == VertexFormat.VertexAttribute.COLOR) {
          data[offset] = r;
          data[offset + 1] = g;
          data[offset + 2] = b;
          data[offset + 3] = a;
          return this;
        }
        offset += vertexAttribute.getValueCount();
      }
      throw new IllegalArgumentException(
          "Vertex attribute " + VertexFormat.VertexAttribute.COLOR + " is not present in the render mode.");
    }

    public void end() {
      checkClosed();
      vertexCount++;
    }

    public Spline createSpline() {
      return Spline.cardinal(t, Arrays.copyOfRange(data, 0, vertexCount * vertexValueCount), vertexCount, vertexValueCount, start, end);
    }
  }
}
