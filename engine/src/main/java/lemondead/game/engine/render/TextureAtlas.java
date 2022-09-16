package lemondead.game.engine.render;

import lemondead.game.engine.render.ogl.Texture;
import lemondead.game.engine.util.MathUtil;
import lemondead.game.engine.util.vector.Vec2;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBRPContext;
import org.lwjgl.stb.STBRPNode;
import org.lwjgl.stb.STBRPRect;
import org.lwjgl.stb.STBRectPack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TextureAtlas<KEY> {
  private final Map<KEY, TexPart> map;
  private final Texture texture;

  public TextureAtlas(Map<KEY, TexPart> map, Texture texture) {
    this.map = map;
    this.texture = texture;
  }

  public Texture getTexture() {
    return texture;
  }

  public TexPart getTexPart(KEY location) {
    return map.get(location);
  }

  private static final class TexEntry<KEY> {
    private final KEY location;
    private final Image image;

    private TexEntry(KEY location, Image image) {
      this.location = location;
      this.image = image;
    }

    public KEY getLocation() {
      return location;
    }

    public Image getImage() {
      return image;
    }

    public int getHeight() {
      return image.getHeight();
    }

    public int getWidth() {
      return image.getWidth();
    }

    public int getArea() {
      return getWidth() * getHeight();
    }
  }

  public static final class TexPart {
    private final float x;
    private final float width;
    private final float y;
    private final float height;

    private TexPart(float x, float width, float y, float height) {
      this.x = x;
      this.width = width;
      this.y = y;
      this.height = height;
    }

    public float getY() {
      return y;
    }

    public float getX() {
      return x;
    }

    public float getEndY() {
      return y + height;
    }

    public float getEndX() {
      return x + width;
    }

    public float getU(float u) {
      return x + u * width;
    }

    public float getV(float v) {
      return y + v * height;
    }
  }

  public static final class Builder<KEY> {
    private final List<TexEntry<KEY>> textures = new ArrayList<>();

    public void addTexture(KEY location, Image image) {
      if (textures.stream().noneMatch(e -> e.getLocation().equals(location))) {
        textures.add(new TexEntry<>(location, image));
      }
    }

    public TextureAtlas<KEY> build() {
      if (textures.size() == 0) {
        throw new IllegalStateException("Must have at least 1 texture.");
      }

      int count = textures.size();
      int totalArea = textures.stream().mapToInt(TexEntry::getArea).sum();
      int minWidth = textures.stream().mapToInt(TexEntry::getWidth).max().orElse(0);

      int width = MathUtil.ceil(textures.stream().mapToInt(TexEntry::getWidth).sum() / 4f) * 4;
      int height = MathUtil.ceil(textures.stream().mapToInt(TexEntry::getHeight).max().orElse(0) / 4f) * 4;

      STBRPRect.Buffer rectangles = new STBRPRect.Buffer(BufferUtils.createByteBuffer(count * STBRPRect.SIZEOF));
      for (int i = 0; i < count; i++) {
        TexEntry<KEY> entry = textures.get(i);
        rectangles.position(i);
        rectangles.id(i);
        rectangles.w((short) entry.getWidth());
        rectangles.h((short) entry.getHeight());
      }
      rectangles.position(0);

      STBRPNode.Buffer nodes = new STBRPNode.Buffer(BufferUtils.createByteBuffer(width * STBRPNode.SIZEOF));
      STBRPContext context = STBRPContext.create();

      List<Vec2.OfInt> minAreaVar = null;
      int currArea = Integer.MAX_VALUE;
      int currWidth = Integer.MAX_VALUE;
      int currHeight = Integer.MAX_VALUE;
      while (width >= minWidth) {
        if (width * height >= totalArea) {
          STBRectPack.stbrp_init_target(context, width, height, nodes);
          if (STBRectPack.stbrp_pack_rects(context, rectangles) == 1) {
            List<Vec2.OfInt> list = new ArrayList<>();
            for (int i = 0; i < textures.size(); i++) {
              rectangles.position(i);
              list.add(Vec2.of(rectangles.x(), rectangles.y()));
              rectangles.x((short) 0);
              rectangles.y((short) 0);
              rectangles.was_packed(true);
            }
            rectangles.position(0);
            int h = 0;
            int w = 0;
            for (int i = 0; i < count; i++) {
              Vec2.OfInt pos = list.get(i);
              TexEntry<KEY> entry = textures.get(i);
              h = Math.max(h, pos.y + entry.getHeight());
              w = Math.max(w, pos.x + entry.getWidth());
            }
            if (h * w < currArea) {
              currWidth = w;
              currHeight = h;
              currArea = h * w;
              minAreaVar = list;
            }
          }
        }
        width = MathUtil.ceil(width * 0.75f);
        height = MathUtil.ceil(height * 4f / 3f);
      }

      if (minAreaVar == null) {
        throw new IllegalStateException("Something went really wrong. Could not create texture atlas.");
      }

      boolean alpha = textures.stream().mapToInt(t -> t.getImage().getChannels()).anyMatch(i -> i == 2 || i == 4);

      Image image = new Image(currWidth, currHeight, alpha ? 4 : 3);
      Map<KEY, TexPart> map = new HashMap<>();

      for (int i = 0; i < count; i++) {
        Vec2.OfInt pos = minAreaVar.get(i);
        TexEntry<KEY> entry = textures.get(i);
        entry.getImage().blit(image, pos.x, pos.y);
        float x = pos.x / (float) currWidth;
        float w = entry.getWidth() / (float) currWidth;
        float y = pos.y / (float) currHeight;
        float h = entry.getHeight() / (float) currHeight;
        map.put(entry.getLocation(), new TexPart(x, w, y, h));
      }

      Texture texture = new Texture();
      texture.loadImage(image);

      return new TextureAtlas<>(map, texture);
    }
  }
}
