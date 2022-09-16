package lemondead.game.engine.render.obj;

import lemondead.game.engine.util.StringUtil;
import lemondead.game.engine.util.vector.Vec3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.Float.parseFloat;

public class MtlFile {
  private static final Pattern commentOrEmptyPattern = Pattern.compile("(^ *#.*|^ *)");

  private final Map<String, Material> materialMap = new HashMap<>();
  private final String file;

  public MtlFile(String file) {
    this.file = file;
    InputStream stream = ClassLoader.getSystemResourceAsStream(file);
    if (stream == null) {
      throw new IllegalStateException("Could not find file " + file);
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
      Iterator<String> itr = reader.lines().filter(s -> !commentOrEmptyPattern.matcher(s).matches()).iterator();

      Material currMtl = null;

      while (itr.hasNext()) {
        String line = itr.next();
        String[] s = StringUtil.removeAndSplit(line, ' ');

        switch (s[0]) {
          case "newmtl":
            Material material = new Material(s[1]);
            materialMap.put(material.getName(), material);
            currMtl = material;
            break;
          case "Kd":
            if (currMtl == null) {
              throw new IllegalStateException("No material was defined previously.");
            }
            currMtl.setDiffuseColor(Vec3.of(parseFloat(s[1]), parseFloat(s[2]), parseFloat(s[3])));
            break;
          case "d":
            if (currMtl == null) {
              throw new IllegalStateException("No material was defined previously.");
            }
            currMtl.setDensity(parseFloat(s[1]));
            break;
          case "map_Kd":
            if (currMtl == null) {
              throw new IllegalStateException("No material was defined previously.");
            }
            currMtl.setDiffuseTexture(s[1]);
            break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getFile() {
    return file;
  }

  public Material getMaterial(String name) {
    return materialMap.get(name);
  }

  public Collection<Material> getMaterials() {
    return materialMap.values();
  }
}