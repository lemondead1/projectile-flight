package lemondead.game.engine.render.obj;

import lemondead.game.engine.render.Image;
import lemondead.game.engine.render.TextureAtlas;
import lemondead.game.engine.render.buffers.*;
import lemondead.game.engine.render.ogl.*;
import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec3;
import lemondead.game.engine.util.vector.Vec4;
import org.lwjgl.opengl.GL20C;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.lang.Float.parseFloat;

public class ObjLoader {
  private static final Material defaultMaterial = new Material("default", Vec3.of(1, 1, 1), null, 1);
  private static final Pattern commentOrEmptyPattern = Pattern.compile("(^ *#.*|^ *)");

  private final List<ObjElement> elements = new ArrayList<>();
  private final Map<String, MtlFile> mtlLibs = new HashMap<>();
  private float[] posCols = new float[7 * 64];
  private int posCount = 0;
  //private final FloatList normals = new FloatArrayList();
  private int normalCount = 0;
  private float[] texPos = new float[2 * 64];
  private int texPosCount = 0;
  private final String objFile;
  private MtlFile currentMtlLib;
  private Material currentMaterial = defaultMaterial;
  private CurveType currentCurveType;
  private int uDegree;
  private CurveBuilder curveBuilder;

  private ObjLoader(String objFile) {
    this.objFile = objFile;
  }

  public static BakedObj loadSafe(OGLBufferAllocator allocator, ModelTransformer transformer, String file) {
    try {
      return load(allocator, transformer, file);
    } catch (FileNotFoundException | ObjFormatException e) {
      e.printStackTrace();
      BufferRenderer renderer = new BufferRenderer() {
        @Override
        public void draw(Consumer<UniformSetter> uniformSetter) {
        }

        @Override
        public void close() {
        }
      };
      return new BakedObj(renderer, renderer, renderer, renderer, null);
    }
  }

  public static BakedObj load(OGLBufferAllocator allocator, ModelTransformer transformer, String file)
      throws ObjFormatException, FileNotFoundException {
    ObjLoader loader = new ObjLoader(file);

    ErrorHandler errorHandler = new ErrorHandler(file);

    InputStream stream = ClassLoader.getSystemResourceAsStream(file);
    if (stream == null) {
      throw new FileNotFoundException("Obj file with name " + file + " was not found.");
    }
    try (Scanner scanner = new Scanner(stream)) {
      while (scanner.hasNextLine()) {
        errorHandler.addLine();
        String line = scanner.nextLine();
        if (!commentOrEmptyPattern.matcher(line).matches()) {
          List<String> strings = Arrays.asList(line.split(" "));

          String tag = strings.get(0);

          if ("o".equals(tag) || "g".equals(tag) || "s".equals(tag)) {
            continue;
          }

          List<String> args = strings.subList(1, strings.size());
          try {
            if (loader.curveBuilder != null) {
              switch (strings.get(0)) {
                case "parm":
                  loader.processCurveParm(args);
                  break;
                case "end":
                  loader.processCurveEnd();
                  break;
                default:
                  errorHandler.error("Curve statement was interrupted.");
                  break;
              }
            } else {
              switch (strings.get(0)) {
                case "deg":
                  loader.processDegree(args);
                  break;
                case "cstype":
                  loader.processCstype(args, errorHandler);
                  break;
                case "mtllib":
                  loader.processMtlLib(args);
                  break;
                case "v":
                  loader.processVec(args);
                  break;
                case "vn":
                  loader.processNormal();
                  break;
                case "vt":
                  loader.processTex(args);
                  break;
                case "l":
                  loader.processLine(args);
                  break;
                case "f":
                  loader.processFace(args);
                  break;
                case "usemtl":
                  loader.processUseMtl(args, errorHandler);
                  break;
                case "curv":
                  loader.processCurve(args);
                  break;
                case "parm":
                  errorHandler.error("Tag parm was used outside curve statement.");
                  break;
                case "end":
                  errorHandler.error("Tag end was used outside curve statement.");
                  break;
                default:
                  OGLManager.RENDER_LOGGER.warning("Tag " + strings.get(0) + " is undefined");
                  continue;
              }
            }
          } catch (Exception exception) {
            errorHandler.error(exception);
          }
          errorHandler.throwCaught();
        }
      }
    }

    //Collections.shuffle(loader.elements); Why did I add this???

    TextureAtlas.Builder<Material> builder = new TextureAtlas.Builder<>();
    boolean hasTextures = false;
    for (MtlFile m : loader.mtlLibs.values()) {
      for (Material material : m.getMaterials()) {
        if (material.getDiffuseTexture() != null) {
          hasTextures = true;
          builder.addTexture(material, new Image(material.getDiffuseTexture()));
        }
      }
    }
    TextureAtlas<Material> atlas = null;
    if (hasTextures) {
      atlas = builder.build();
    }

    ObjBaker baker = new ObjBaker(atlas);

    loader.elements.forEach(e -> e.bake(baker));

    return baker.bake(allocator, transformer);
  }

  private void processMtlLib(List<String> a) {
    String name = a.get(0);
    MtlFile file = mtlLibs.get(name);
    if (file == null) {
      String[] path = objFile.split("/");
      path[path.length - 1] = name;
      String str = String.join("/", path);
      file = new MtlFile(str);
      mtlLibs.put(str, file);
    }
    currentMtlLib = file;
  }

  private void processUseMtl(List<String> a, ErrorHandler handler) {
    String name = a.get(0);
    if (currentMtlLib == null) {
      handler.error("Material library has not been defined.");
    }
    currentMaterial = currentMtlLib.getMaterial(name);
    if (currentMaterial == null) {
      handler.error("Material with name " + name + " has not been defined in file " + currentMtlLib.getFile());
    }
  }

  private void processCstype(List<String> a, ErrorHandler handler) {
    int index = 0;
    if (a.size() > 1) {
      index = 1;
    }
    switch (a.get(index)) {
      case "bspline":
        currentCurveType = CurveType.B_SPLINE;
        break;
      case "bezier":
        currentCurveType = CurveType.BEZIER;
        break;
      case "cardinal":
        currentCurveType = CurveType.CARDINAL;
        break;
      default:
        handler.error("Only b-spline, bezier and cardinal curves are supported.");
    }
  }

  private void processDegree(List<String> a) {
    uDegree = Integer.parseInt(a.get(0));
  }

  private void processCurve(List<String> a) {
    float start = Float.parseFloat(a.get(0));
    float end = Float.parseFloat(a.get(1));
    int[] indices = a.stream().skip(2).mapToInt(Integer::parseInt).map(i -> {
      if (i < 0) {
        return posCount + i;
      }
      return i - 1;
    }).toArray();
    curveBuilder = new CurveBuilder(start, end, indices);
  }

  private void processCurveParm(List<String> a) {
    if ("u".equals(a.get(0))) {
      float[] floats = new float[a.size() - 1];
      for (int i = 0; i < a.size() - 1; i++) {
        floats[i] = Float.parseFloat(a.get(i + 1));
      }
      curveBuilder.setKnot(floats);
    }
  }

  private void processCurveEnd() {
    CurveBuilder curve = curveBuilder;
    CurveType type = currentCurveType;
    int degree = uDegree;
    elements.add(baker -> {
      SplineBuilder drawBuilder = baker.splines();
      switch (type) {
        case BEZIER:
          drawBuilder.beginBezier(degree, curve.start, curve.end, builder -> {
            for (int i : curve.indices) {
              float x = posCols[i * 7];
              float y = posCols[i * 7 + 1];
              float z = posCols[i * 7 + 2];
              float w = posCols[i * 7 + 3];
              float r = posCols[i * 7 + 4];
              float g = posCols[i * 7 + 5];
              float b = posCols[i * 7 + 6];
              builder.position(x, y, z).weight(w).color(r, g, b, 1).end();
            }
          });
          break;
        case B_SPLINE:
          drawBuilder.beginNurbs(curve.knot, degree, curve.start, curve.end, builder -> {
            for (int i : curve.indices) {
              float x = posCols[i * 7];
              float y = posCols[i * 7 + 1];
              float z = posCols[i * 7 + 2];
              float w = posCols[i * 7 + 3];
              float r = posCols[i * 7 + 4];
              float g = posCols[i * 7 + 5];
              float b = posCols[i * 7 + 6];
              builder.position(x, y, z).weight(w).color(r, g, b, 1).end();
            }
          });
          break;
        case CARDINAL:
          drawBuilder.beginCardinal(degree, curve.start, curve.end, builder -> {
            for (int i : curve.indices) {
              float x = posCols[i * 7];
              float y = posCols[i * 7 + 1];
              float z = posCols[i * 7 + 2];
              float r = posCols[i * 7 + 4];
              float g = posCols[i * 7 + 5];
              float b = posCols[i * 7 + 6];
              builder.position(x, y, z).color(r, g, b, 1).end();
            }
          });
      }
    });
    curveBuilder = null;
  }

  private void processVec(List<String> a) {
    float x = parseFloat(a.get(0));
    float y = parseFloat(a.get(1));
    float z = parseFloat(a.get(2));
    float w = 1;
    float r = 1;
    float g = 1;
    float b = 1;
    switch (a.size()) {
      case 7:
        r = parseFloat(a.get(4));
        g = parseFloat(a.get(5));
        b = parseFloat(a.get(6));
      case 4:
        w = parseFloat(a.get(3));
        break;
      case 6:
        r = parseFloat(a.get(3));
        g = parseFloat(a.get(4));
        b = parseFloat(a.get(5));
        break;
    }

    if (posCount * 7 >= posCols.length) {
      posCols = Arrays.copyOf(posCols, Math.max(posCount + 1, posCols.length / 14 * 3) * 7);
    }

    posCols[posCount * 7] = x;
    posCols[posCount * 7 + 1] = y;
    posCols[posCount * 7 + 2] = z;
    posCols[posCount * 7 + 3] = w;
    posCols[posCount * 7 + 4] = r;
    posCols[posCount * 7 + 5] = g;
    posCols[posCount * 7 + 6] = b;
    posCount++;
  }

  private void processNormal() {
    normalCount++;
  }

  private void processTex(List<String> a) {
    if (texPosCount >= texPos.length / 2) {
      texPos = Arrays.copyOf(texPos, Math.max(texPosCount + 1, texPos.length * 3 / 4) * 2);
    }
    texPos[texPosCount * 2] = parseFloat(a.get(0));
    texPos[texPosCount * 2 + 1] = parseFloat(a.get(1));
    texPosCount++;
  }

  private void processLine(List<String> a) {
    int[] indices = a.stream().mapToInt(Integer::parseInt).map(i -> {
      if (i < 0) {
        return posCount + i;
      }
      return i - 1;
    }).toArray();
    elements.add(baker -> baker.lines().startStrip(1, builder -> {
      for (int i : indices) {
        float x = posCols[i * 7];
        float y = posCols[i * 7 + 1];
        float z = posCols[i * 7 + 2];
        float r = posCols[i * 7 + 4];
        float g = posCols[i * 7 + 5];
        float b = posCols[i * 7 + 6];
        builder.pos(x, y, z).color(r, g, b, 1).end();
      }
    }));
  }

  private void processFace(List<String> a) {
    int[] indices = a.stream().map(this::parseIndex).flatMapToInt(Arrays::stream).toArray();
    Material material = currentMaterial;
    elements.add(baker -> {
      if (material.getDiffuseTexture() == null) {
        baker.untextured().startPolygon(builder -> {
          for (int i = 0; i < indices.length; i += 3) {
            int posIndex = indices[i];
            float x = posCols[posIndex * 7];
            float y = posCols[posIndex * 7 + 1];
            float z = posCols[posIndex * 7 + 2];
            float r = posCols[posIndex * 7 + 4];
            float g = posCols[posIndex * 7 + 5];
            float b = posCols[posIndex * 7 + 6];
            r *= material.getDiffuseColor().x();
            g *= material.getDiffuseColor().y();
            b *= material.getDiffuseColor().z();
            builder.pos(x, y, z).color(r, g, b, material.getDensity()).end();
          }
        });
      } else {
        baker.textured().startPolygon(builder -> {
          for (int i = 0; i < indices.length; i += 3) {
            int posIndex = indices[i];
            float x = posCols[posIndex * 7];
            float y = posCols[posIndex * 7 + 1];
            float z = posCols[posIndex * 7 + 2];
            float r = posCols[posIndex * 7 + 4];
            float g = posCols[posIndex * 7 + 5];
            float b = posCols[posIndex * 7 + 6];
            r *= material.getDiffuseColor().x();
            g *= material.getDiffuseColor().y();
            b *= material.getDiffuseColor().z();
            int texIndex = indices[i + 1];
            TextureAtlas.TexPart part = baker.diffuseAtlas().getTexPart(material);
            float u = part.getU(texPos[texIndex * 2]);
            float v = part.getV(texPos[texIndex * 2 + 1]);
            builder.pos(x, y, z).color(r, g, b, material.getDensity()).texPos(u, v).end();
          }
        });
      }
    });
  }

  private int[] parseIndex(String string) {
    String[] s = string.split("/");
    int pos = Integer.parseInt(s[0]);
    int tex = 0;
    int norm = 0;
    if (s.length > 1) {
      if (!s[1].isEmpty()) {
        tex = Integer.parseInt(s[1]);
      }
      if (s.length > 2) {
        norm = Integer.parseInt(s[2]);
      }
    }
    if (pos < 0) {
      pos = posCount + pos + 1;
    }
    if (tex < 0) {
      tex = texPosCount + tex + 1;
    }
    if (norm < 0) {
      norm = normalCount + norm + 1;
    }
    return new int[] { pos - 1, tex - 1, norm - 1 };
  }

  private static class CurveBuilder {
    private final float start;
    private final float end;
    private final int[] indices;
    private float[] knot;

    private CurveBuilder(float start, float end, int[] indices) {
      this.start = start;
      this.end = end;
      this.indices = indices;
    }

    public void setKnot(float[] knot) {
      this.knot = knot;
    }
  }

  private static class ErrorHandler {
    private int line;
    private final String fileName;
    private ObjFormatException exception;

    public ErrorHandler(String fileName) {
      this.fileName = fileName;
    }

    public void error(String message) {
      exception = new ObjFormatException(fileName + " is malformed. First error is at line " + line + ". " + message);
    }

    public void error(Throwable throwable) {
      exception = new ObjFormatException(fileName + " is malformed. First error is at line " + line + ". ", throwable);
    }

    public void throwCaught() throws ObjFormatException {
      if (exception != null) {
        throw exception;
      }
    }

    public void addLine() {
      line++;
    }
  }

  private static class ObjBaker {
    private SimpleBuilder texturedTriangles;
    private SimpleBuilder untexturedTriangles;
    private SimpleBuilder lines;
    private SplineBuilder splines;
    private final TextureAtlas<Material> diffuseAtlas;

    public ObjBaker(TextureAtlas<Material> atlas) {
      this.diffuseAtlas = atlas;
    }

    public SimpleBuilder textured() {
      if (texturedTriangles == null) {
        texturedTriangles = new SimpleBuilder(RenderMode.SOLID, Program.POSITION_COLOR_TEXTURE);
      }
      return texturedTriangles;
    }

    public SimpleBuilder untextured() {
      if (untexturedTriangles == null) {
        untexturedTriangles = new SimpleBuilder(RenderMode.SOLID, Program.POSITION_COLOR);
      }
      return untexturedTriangles;
    }

    public SimpleBuilder lines() {
      if (lines == null) {
        lines = new SimpleBuilder(RenderMode.LINES, Program.POSITION_COLOR);
      }
      return lines;
    }

    public SplineBuilder splines() {
      if (splines == null) {
        splines = new SplineBuilder(Program.POSITION_COLOR);
      }
      return splines;
    }

    public TextureAtlas<Material> diffuseAtlas() {
      return diffuseAtlas;
    }

    public BakedObj bake(OGLBufferAllocator allocator, ModelTransformer transformer) {
      BufferRenderer noop = new BufferRenderer() {
        @Override
        public void draw(Consumer<UniformSetter> uniformSetter) {

        }

        @Override
        public void close() {

        }
      };
      BufferRenderer lines = this.lines == null ? noop : this.lines.upload(allocator, transformer);
      BufferRenderer splines = this.splines == null ? noop : this.splines.upload(allocator, transformer);
      BufferRenderer textured = this.texturedTriangles == null ? noop : this.texturedTriangles.upload(allocator, transformer);
      BufferRenderer untextured = this.untexturedTriangles == null ? noop : this.untexturedTriangles.upload(allocator, transformer);

      Texture texture = diffuseAtlas == null ? null : diffuseAtlas.getTexture();

      return new BakedObj(lines, splines, textured, untextured, texture);
    }
  }

  public static class BakedObj {
    private final BufferRenderer lines;
    private final BufferRenderer splines;
    private final BufferRenderer textured;
    private final BufferRenderer untextured;
    private final Texture diffuseTexture;

    public BakedObj(BufferRenderer lines, BufferRenderer splines, BufferRenderer textured, BufferRenderer untextured,
                    Texture diffuseTexture) {
      this.lines = lines;
      this.splines = splines;
      this.textured = textured;
      this.untextured = untextured;
      this.diffuseTexture = diffuseTexture;
    }

    public void drawLines(Mat4 transform, Vec4 color) {
      lines.draw(setter -> {
        setter.setUniform(Program.Uniform.TRANSFORM_MATRIX, transform);
        setter.setUniform(Program.Uniform.TINT, color);
      });
      splines.draw(setter -> {
        setter.setUniform(Program.Uniform.TRANSFORM_MATRIX, transform);
        setter.setUniform(Program.Uniform.TINT, color);
      });
    }

    public void drawSolid(Mat4 transform, Vec4 color) {
      untextured.draw(setter -> {
        setter.setUniform(Program.Uniform.TRANSFORM_MATRIX, transform);
        setter.setUniform(Program.Uniform.TINT, color);
      });
      if (diffuseTexture != null) {
        diffuseTexture.activate(GL20C.GL_TEXTURE0 + 1);
      }
      textured.draw(setter -> {
        setter.setUniform(Program.Uniform.TRANSFORM_MATRIX, transform);
        setter.setUniform(Program.Uniform.TINT, color);
        setter.setUniform(Program.Uniform.DIFFUSE_TEXTURE, 1);
      });
    }
  }

  private interface ObjElement {
    void bake(ObjBaker baker);
  }

  public enum CurveType {
    BEZIER,
    B_SPLINE,
    CARDINAL,
  }
}
