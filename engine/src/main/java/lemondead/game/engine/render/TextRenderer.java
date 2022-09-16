package lemondead.game.engine.render;

import lemondead.game.engine.render.buffers.BufferRenderer;
import lemondead.game.engine.render.buffers.ModelTransformer;
import lemondead.game.engine.render.buffers.SimpleBuilder;
import lemondead.game.engine.render.ogl.OGLManager;
import lemondead.game.engine.render.ogl.Program;
import lemondead.game.engine.render.ogl.RenderMode;
import lemondead.game.engine.render.ogl.Texture;
import lemondead.game.engine.util.MathUtil;
import lemondead.game.engine.util.vector.Mat4;
import lemondead.game.engine.util.vector.Vec2;
import lemondead.game.engine.util.vector.Vec4;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.stb.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TextRenderer {
  private static final int[] defaultChars = IntStream.concat(IntStream.range('\u0000', '\u00FF'), IntStream.range('\u0400', '\u04FF')).toArray();
  private static final int fontAtlasSize = 1024;

  private final Map<GlyphKey, PackedGlyph> glyphTextures = new HashMap<>();
  private final Map<FontSize, PackedGlyph> defaultGlyphs;
  private final STBTTFontinfo info;
  private final List<Texture> textures = new ArrayList<>();

  private final int lineGap;

  public void testTextures() {
    int size = (int) Math.ceil(Math.sqrt(textures.size()));

    float cellSize = 2f / size;

    SimpleBuilder builder = new SimpleBuilder(RenderMode.SOLID, Program.TEXT);
    builder.startFan(2, f -> {
      f.pos(0, 0, 0).texPos(0, 0).color(1, 1, 1, 1).end();
      f.pos(cellSize, 0, 0).texPos(1, 0).color(1, 1, 1, 1).end();
      f.pos(cellSize, cellSize, 0).texPos(1, 1).color(1, 1, 1, 1).end();
      f.pos(0, cellSize, 0).texPos(0, 1).color(1, 1, 1, 1).end();
    });
    BufferRenderer renderer = builder.upload(OGLManager.STREAM_DRAW_ALLOCATOR, ModelTransformer.IDENTITY);

    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        int texIndex = y * size + x;
        if (texIndex >= textures.size()) {
          break;
        }
        int finalX = x;
        int finalY = y;
        textures.get(texIndex).activate(GL20C.GL_TEXTURE0 + 1);
        renderer.draw(s -> {
          s.setUniform(Program.Uniform.TINT, Vec4.of(0, 0, 0, 1));
          s.setUniform(Program.Uniform.TRANSFORM_MATRIX,
                       Mat4.scale(1, -1, 1, 1).multiply(Mat4.translate(finalX * cellSize - 1, finalY * cellSize - 1, 0)));
          s.setUniform(Program.Uniform.DIFFUSE_TEXTURE, 1);
        });
      }
    }

    renderer.close();
  }

  private static ByteBuffer getDefaultFont() {
    try (InputStream stream = ClassLoader.getSystemResourceAsStream("com/cathive/fonts/roboto/Roboto-Regular.ttf")) {
      ByteBuffer data = MemoryUtil.memAlloc(Objects.requireNonNull(stream).available());
      Channels.newChannel(stream).read(data);
      data.rewind();
      return data;
    } catch (IOException e) {
      throw new RuntimeException("Could not load font.", e);
    }
  }

  public TextRenderer() {
    this(getDefaultFont());
  }

  private TextRenderer(ByteBuffer data) {
    info = STBTTFontinfo.malloc();
    data.rewind();
    if (!STBTruetype.stbtt_InitFont(info, data)) {
      MemoryUtil.memFree(data);
      info.free();
      throw new IllegalStateException("Could not initialize font.");
    }

    try (MemoryStack stack = MemoryStack.stackPush()) {
      final int defaultCount = defaultChars.length;

      STBTTPackRange.Buffer ranges = STBTTPackRange.mallocStack(FontSize.VALUES.length, stack);
      int fullHeight;
      {
        IntBuffer ascent = stack.mallocInt(1);
        IntBuffer descent = stack.mallocInt(1);
        STBTruetype.stbtt_GetFontVMetrics(info, ascent, descent, null);
        fullHeight = ascent.get(0) - descent.get(0);
      }
      lineGap = fullHeight;

      for (int i = 0; i < FontSize.VALUES.length; i++) {
        FontSize size = FontSize.VALUES[i];
        STBTTPackedchar.Buffer chars = new STBTTPackedchar.Buffer(stack.malloc(STBTTPackedchar.SIZEOF * defaultCount));
        IntBuffer buffer = stack.mallocInt(defaultCount);
        Arrays.stream(defaultChars).forEach(buffer::put);
        buffer.rewind();
        ranges.position(i)
              .chardata_for_range(chars)
              .array_of_unicode_codepoints(buffer)
              .font_size(MathUtil.ceil(fullHeight * STBTruetype.stbtt_ScaleForPixelHeight(info, size.pixelSize)))
              .num_chars(defaultCount);
      }


      FontSize[] incompleteSizes = FontSize.VALUES.clone();

      STBTTPackContext packContext = STBTTPackContext.mallocStack(stack);
      IntBuffer advance = stack.mallocInt(1);
      IntBuffer x0 = stack.mallocInt(1);
      IntBuffer y0 = stack.mallocInt(1);
      IntBuffer x1 = stack.mallocInt(1);
      IntBuffer y1 = stack.mallocInt(1);

      boolean allPacked = false;

      while (!allPacked) {
        Image image = new Image(fontAtlasSize, fontAtlasSize, STBImage.STBI_grey);

        ranges.rewind();

        STBTruetype.stbtt_PackBegin(packContext, image.getBuffer(), image.getWidth(), image.getHeight(), 0, 2);
        allPacked = STBTruetype.stbtt_PackFontRanges(packContext, data, 0, ranges);
        STBTruetype.stbtt_PackEnd(packContext);

        Texture texture = new Texture();
        texture.loadImage(image);
        textures.add(texture);

        for (int sizeInd = 0; sizeInd < ranges.limit(); sizeInd++) {
          int left = 0;
          FontSize fontSize = incompleteSizes[sizeInd];
          float s = STBTruetype.stbtt_ScaleForPixelHeight(info, fontSize.pixelSize);
          STBTTPackRange range = ranges.get(sizeInd);
          STBTTPackedchar.Buffer packedChars = range.chardata_for_range();
          IntBuffer codepointBuffer = Objects.requireNonNull(range.array_of_unicode_codepoints());
          int charNum = range.num_chars();
          for (int i = 0; i < charNum; i++) {
            packedChars.position(i);
            if (packedChars.x0() != 0 || packedChars.y0() != 0 || packedChars.x1() != 0 || packedChars.y1() != 0) {
              int codepoint = codepointBuffer.get(i);
              STBTruetype.stbtt_GetCodepointHMetrics(info, codepoint, advance, null);
              STBTruetype.stbtt_GetCodepointBox(info, codepoint, x0, y0, x1, y1);
              PackedGlyph g = new PackedGlyph(packedChars.x0() / (float) fontAtlasSize, packedChars.y0() / (float) fontAtlasSize,
                                              packedChars.x1() / (float) fontAtlasSize, packedChars.y1() / (float) fontAtlasSize,
                                              x0.get(0) * s, y0.get(0) * s, x1.get(0) * s, y1.get(0) * s, advance.get(0) * s, texture);
              glyphTextures.put(new GlyphKey(fontSize, codepoint), g);
            } else {
              codepointBuffer.put(left++, codepointBuffer.get(i));
            }
          }
          range.num_chars(left);
        }
      }
      defaultGlyphs = Arrays.stream(FontSize.VALUES).collect(Collectors.toMap(Function.identity(), s -> glyphTextures.get(new GlyphKey(s, 0)),
                                                                              (g1, g2) -> {throw new IllegalStateException();},
                                                                              () -> new EnumMap<>(FontSize.class)));
    }
  }

  public void drawString(String string, Vec2 pos, Vec4 color, FontSize size, Alignment alignment, Camera camera) {
    if (defaultGlyphs == null) {
      return;
    }
    Map<Texture, SimpleBuilder> builderMap = new HashMap<>();
    char prev = 0;
    double scale1 = STBTruetype.stbtt_ScaleForPixelHeight(info, size.pixelSize);
    double xPos;
    long y = Math.round(pos.y());
    double lineWidth = 0;

    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      lineWidth += STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale1;
      lineWidth += glyphTextures.getOrDefault(new GlyphKey(size, ch), defaultGlyphs.get(size)).advance;
      prev = ch;
    }

    prev = 0;

    switch (alignment) {
      case LEFT:
        xPos = pos.x();
        break;
      case JUSTIFIED:
      case CENTERED:
        xPos = pos.x() - lineWidth / 2;
        break;
      case RIGHT:
        xPos = pos.x() - lineWidth;
        break;
      default:
        throw new NullPointerException();
    }

    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      xPos += STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale1;

      PackedGlyph glyphTex = glyphTextures.getOrDefault(new GlyphKey(size, ch), defaultGlyphs.get(size));

      int roundedX = (int) Math.round(xPos);

      float x0 = roundedX + glyphTex.x0;
      float x1 = roundedX + glyphTex.x1;
      float y0 = y - glyphTex.y0;
      float y1 = y - glyphTex.y1;

      SimpleBuilder dataBuilder = builderMap.computeIfAbsent(glyphTex.texture, t -> new SimpleBuilder(RenderMode.SOLID, Program.TEXT));

      dataBuilder.startFan(2, builder -> {
        builder.pos(x1, y0, 0).texPos(glyphTex.u1, glyphTex.v1).color(color).end();
        builder.pos(x0, y0, 0).texPos(glyphTex.u0, glyphTex.v1).color(color).end();
        builder.pos(x0, y1, 0).texPos(glyphTex.u0, glyphTex.v0).color(color).end();
        builder.pos(x1, y1, 0).texPos(glyphTex.u1, glyphTex.v0).color(color).end();
      });
      xPos += glyphTex.advance;
      prev = ch;
    }

    builderMap.forEach((texture, builder) -> {
      try (BufferRenderer renderer = builder.upload(OGLManager.STREAM_DRAW_ALLOCATOR, ModelTransformer.IDENTITY)) {
        texture.activate(GL20C.GL_TEXTURE0 + 1);
        renderer.draw(c -> {
          c.setUniform(Program.Uniform.DIFFUSE_TEXTURE, 1);
          c.setUniform(Program.Uniform.TRANSFORM_MATRIX, camera.getScreenMatrix());
        });
      }
    });
  }

  public void drawMultilineString(String string, Vec2 start, Vec2 end, Vec4 color, Alignment alignment, FontSize fontSize,
                                  float indent, float scale, float lineSpaceMultiplier, float paragraphLineSpaceMultiplier, Camera camera) {
    List<String> lines = new ArrayList<>();

    double scale1 = STBTruetype.stbtt_ScaleForPixelHeight(info, fontSize.pixelSize);

    StringBuilder currentLine = new StringBuilder();

    int lastSpaceAbsoluteIndex = -1;
    int lastSpaceIndex = -1;

    double length = indent;

    double maxWidth = end.x() - start.x();

    char prev = 0;

    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (ch == '\n') {
        lines.add(currentLine.toString());
        currentLine.setLength(0);
        currentLine.append(ch);
        length = indent;
        continue;
      } else if (ch == ' ') {
        lastSpaceIndex = currentLine.length();
        lastSpaceAbsoluteIndex = i;
      }

      double kern = STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale * scale1;
      double charWidth = glyphTextures.getOrDefault(new GlyphKey(fontSize, ch), defaultGlyphs.get(fontSize)).advance * scale;

      if (length + kern + charWidth > maxWidth) {
        if (lastSpaceIndex == -1) {
          lines.add(currentLine.toString());
          i--;
        } else {
          currentLine.setLength(lastSpaceIndex);
          lines.add(currentLine.toString());
          i = lastSpaceAbsoluteIndex;
        }
        lastSpaceAbsoluteIndex = -1;
        lastSpaceIndex = -1;
        currentLine.setLength(0);
        length = 0;
        prev = 0;
        continue;
      }

      currentLine.append(ch);
      length += kern + charWidth;
      prev = ch;
    }
    lines.add(currentLine.toString());

    Map<Texture, SimpleBuilder> builderMap = new HashMap<>();
    double yPos = start.y();

    switch (alignment) {
      case LEFT:
        for (String string1 : lines) {
          yPos += lineGap * scale1 * (string1.startsWith("\n") ? paragraphLineSpaceMultiplier : lineSpaceMultiplier);
          prev = 0;
          double xPos = start.x();
          for (int i = 0; i < string1.length(); i++) {
            char ch = string1.charAt(i);

            if (ch == '\n') {
              xPos += indent;
            } else {
              xPos += STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale * scale1;

              PackedGlyph glyphTex = glyphTextures.getOrDefault(new GlyphKey(fontSize, ch), defaultGlyphs.get(fontSize));

              long roundedX = Math.round(xPos);

              float x0 = (roundedX + glyphTex.x0 * scale);
              float x1 = (roundedX + glyphTex.x1 * scale);
              float y0 = (float) ((yPos - glyphTex.y0 * scale));
              float y1 = (float) ((yPos - glyphTex.y1 * scale));

              SimpleBuilder dataBuilder = builderMap.computeIfAbsent(glyphTex.texture, t -> new SimpleBuilder(RenderMode.SOLID, Program.TEXT));

              dataBuilder.startFan(2, builder -> {
                builder.pos(x1, y0, 0).texPos(glyphTex.u1, glyphTex.v1).color(color).end();
                builder.pos(x0, y0, 0).texPos(glyphTex.u0, glyphTex.v1).color(color).end();
                builder.pos(x0, y1, 0).texPos(glyphTex.u0, glyphTex.v0).color(color).end();
                builder.pos(x1, y1, 0).texPos(glyphTex.u1, glyphTex.v0).color(color).end();
              });
              xPos += glyphTex.advance * scale;
              prev = ch;
            }
          }
        }

        break;
      case CENTERED:
        for (String string1 : lines) {
          yPos += lineGap * scale1 * (string1.startsWith("\n") ? paragraphLineSpaceMultiplier : lineSpaceMultiplier);

          double lineLength = 0;

          prev = 0;

          for (int i = 0; i < string1.length(); i++) {
            char ch = string1.charAt(i);
            lineLength += STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale * scale1;
            PackedGlyph glyphTex = glyphTextures.getOrDefault(new GlyphKey(fontSize, ch), defaultGlyphs.get(fontSize));
            lineLength += glyphTex.advance * scale;
            prev = ch;
          }

          prev = 0;
          double xPos = start.x() + (maxWidth - lineLength) / 2;
          for (int i = 0; i < string1.length(); i++) {
            char ch = string1.charAt(i);
            if (ch != '\n') {
              xPos += STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale * scale1;

              PackedGlyph glyphTex = glyphTextures.getOrDefault(new GlyphKey(fontSize, ch), defaultGlyphs.get(fontSize));

              long roundedX = Math.round(xPos);

              float x0 = (roundedX + glyphTex.x0 * scale);
              float x1 = (roundedX + glyphTex.x1 * scale);
              float y0 = (float) ((yPos - glyphTex.y0 * scale));
              float y1 = (float) ((yPos - glyphTex.y1 * scale));

              SimpleBuilder dataBuilder = builderMap.computeIfAbsent(glyphTex.texture, t -> new SimpleBuilder(RenderMode.SOLID, Program.TEXT));

              dataBuilder.startFan(2, builder -> {
                builder.pos(x1, y0, 0).texPos(glyphTex.u1, glyphTex.v1).color(color).end();
                builder.pos(x0, y0, 0).texPos(glyphTex.u0, glyphTex.v1).color(color).end();
                builder.pos(x0, y1, 0).texPos(glyphTex.u0, glyphTex.v0).color(color).end();
                builder.pos(x1, y1, 0).texPos(glyphTex.u1, glyphTex.v0).color(color).end();
              });
              xPos += glyphTex.advance * scale;
              prev = ch;
            }
          }
        }
        break;
      case RIGHT:
        for (String string1 : lines) {
          double xPos = end.x();
          yPos += lineGap * scale1 * (string1.startsWith("\n") ? paragraphLineSpaceMultiplier : lineSpaceMultiplier);
          for (int i = string1.length() - 1; i >= 0; i--) {
            char ch = string1.charAt(i);

            if (ch != '\n') {
              PackedGlyph glyphTex = glyphTextures.getOrDefault(new GlyphKey(fontSize, ch), defaultGlyphs.get(fontSize));

              prev = i == 0 ? 0 : string1.charAt(i - 1);

              xPos -= scale * (STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale1 + glyphTex.advance);

              long roundedX = Math.round(xPos);

              float x0 = (roundedX + glyphTex.x0 * scale);
              float x1 = (roundedX + glyphTex.x1 * scale);
              float y0 = (float) ((yPos - glyphTex.y0 * scale));
              float y1 = (float) ((yPos - glyphTex.y1 * scale));

              SimpleBuilder dataBuilder = builderMap.computeIfAbsent(glyphTex.texture, t -> new SimpleBuilder(RenderMode.SOLID, Program.TEXT));

              dataBuilder.startFan(2, builder -> {
                builder.pos(x1, y0, 0).texPos(glyphTex.u1, glyphTex.v1).color(color).end();
                builder.pos(x0, y0, 0).texPos(glyphTex.u0, glyphTex.v1).color(color).end();
                builder.pos(x0, y1, 0).texPos(glyphTex.u0, glyphTex.v0).color(color).end();
                builder.pos(x1, y1, 0).texPos(glyphTex.u1, glyphTex.v0).color(color).end();
              });
            }
          }
        }
        break;
      case JUSTIFIED:
        for (int l = 0; l < lines.size(); l++) {
          String string1 = lines.get(l);

          yPos += lineGap * scale1 * (string1.startsWith("\n") ? paragraphLineSpaceMultiplier : lineSpaceMultiplier);

          double lineLength = 0;
          int spaceCount = 0;

          prev = 0;

          for (int i = 0; i < string1.length(); i++) {
            char ch = string1.charAt(i);
            if (ch == '\n') {
              lineLength += indent;
            } else if (ch != ' ') {
              lineLength += STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale * scale1;
              PackedGlyph glyphTex = glyphTextures.getOrDefault(new GlyphKey(fontSize, ch), defaultGlyphs.get(fontSize));
              lineLength += glyphTex.advance * scale;
            } else {
              spaceCount++;
            }
            prev = ch;
          }

          double spaceLength = (maxWidth - lineLength) / spaceCount;
          double spaceBetweenGlyphs = spaceCount == 0 ? (maxWidth - lineLength) / (string1.length() - 1) : 0;

          if (l + 1 >= lines.size() || lines.get(l + 1).startsWith("\n")) {
            spaceLength = glyphTextures.getOrDefault(new GlyphKey(fontSize, ' '), defaultGlyphs.get(fontSize)).advance * scale;
            spaceBetweenGlyphs = 0;
          }

          prev = 0;
          double xPos = start.x();
          for (int i = 0; i < string1.length(); i++) {
            char ch = string1.charAt(i);
            if (ch == '\n') {
              xPos += indent;
            } else if (ch == ' ') {
              xPos += spaceLength;
            } else {
              xPos += STBTruetype.stbtt_GetCodepointKernAdvance(info, prev, ch) * scale * scale1;
              PackedGlyph glyphTex = glyphTextures.getOrDefault(new GlyphKey(fontSize, ch), defaultGlyphs.get(fontSize));

              long roundedX = Math.round(xPos);

              float x0 = roundedX + glyphTex.x0 * scale;
              float x1 = roundedX + glyphTex.x1 * scale;
              float y0 = (float) (yPos - glyphTex.y0 * scale);
              float y1 = (float) (yPos - glyphTex.y1 * scale);

              SimpleBuilder dataBuilder = builderMap.computeIfAbsent(glyphTex.texture, t -> new SimpleBuilder(RenderMode.SOLID, Program.TEXT));

              dataBuilder.startFan(2, builder -> {
                builder.pos(x1, y0, 0).texPos(glyphTex.u1, glyphTex.v1).color(color).end();
                builder.pos(x0, y0, 0).texPos(glyphTex.u0, glyphTex.v1).color(color).end();
                builder.pos(x0, y1, 0).texPos(glyphTex.u0, glyphTex.v0).color(color).end();
                builder.pos(x1, y1, 0).texPos(glyphTex.u1, glyphTex.v0).color(color).end();
              });
              xPos += glyphTex.advance * scale + spaceBetweenGlyphs;
              prev = ch;
            }
          }
        }
        break;
    }

    builderMap.forEach((texture, builder) -> {
      try (BufferRenderer renderer = builder.upload(OGLManager.STREAM_DRAW_ALLOCATOR, ModelTransformer.IDENTITY)) {
        texture.activate(GL20C.GL_TEXTURE0 + 1);
        renderer.draw(c -> {
          c.setUniform(Program.Uniform.DIFFUSE_TEXTURE, 1);
          c.setUniform(Program.Uniform.TRANSFORM_MATRIX, camera.getScreenMatrix());
        });
      }
    });
  }

  public enum Alignment {
    LEFT,
    CENTERED,
    RIGHT,
    JUSTIFIED,
  }

  private static class GlyphKey {
    private final FontSize size;
    private final int codepoint;

    private GlyphKey(FontSize size, int codepoint) {
      this.size = size;
      this.codepoint = codepoint;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      GlyphKey glyphKey = (GlyphKey) o;
      return size == glyphKey.size && codepoint == glyphKey.codepoint;
    }

    @Override
    public int hashCode() {
      return (31 * size.hashCode()) ^ codepoint;
    }
  }

  private static class PackedGlyph {
    private final Texture texture;
    private final float u0, v0;
    private final float u1, v1;
    private final float x0, y0;
    private final float x1, y1;
    private final float advance;

    private PackedGlyph(float u0, float v0, float u1, float v1,
                        float x0, float y0, float x1, float y1,
                        float advance, Texture texture) {
      this.texture = texture;
      this.u0 = u0;
      this.v0 = v0;
      this.u1 = u1;
      this.v1 = v1;
      this.advance = advance;
      this.x0 = x0;
      this.y0 = y0;
      this.x1 = x1;
      this.y1 = y1;
    }
  }

  public enum FontSize {
    PT_11(18),
    PT_14(24),
    PT_18(30),
    PT_22(36);

    public static final FontSize[] VALUES = values();

    private final float pixelSize;

    FontSize(float pixelSize) {
      this.pixelSize = pixelSize;
    }

    public float getPixelSize() {
      return pixelSize;
    }
  }

  public void free() {
    info.free();
  }
}