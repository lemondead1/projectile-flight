package lemondead.game.engine.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class Configuration {
  private final Map<String, Integer> values = new HashMap<>();

  private final List<String> lines = new ArrayList<>();

  private final File file;

  public Configuration(String name) {
    try {
      file = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI().resolve(name));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    if (file.exists()) {
      try (Scanner scanner = new Scanner(file)) {
        while (scanner.hasNextLine()) {
          String line = scanner.nextLine();
          String[] a = line.split("=");
          if (a.length == 2) {
            values.put(a[0].trim(), lines.size());
          }
          lines.add(line);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public String getValue(String key, String defaultValue, Predicate<String> isValid) {
    if (values.containsKey(key)) {
      Integer index = values.get(key);
      String line = lines.get(index).trim();
      if (!line.startsWith("#")) {
        String[] strs = line.split("=");
        String value = strs[1].trim();
        if (isValid.test(value)) {
          return value;
        } else {
          lines.set(index, "#" + line);
        }
      }
    }
    values.put(key, lines.size());
    lines.add(key + "=" + defaultValue);
    return defaultValue;
  }

  public <T> T getValueParsed(String key, String defaultValue, Function<String, T> parser) {
    if (values.containsKey(key)) {
      Integer index = values.get(key);
      String line = lines.get(index).trim();
      if (!line.startsWith("#")) {
        String[] strs = line.split("=");
        String value = strs[1].trim();
        T t = null;
        try {
          t = parser.apply(value);
        } catch (Exception ignored) {
        }
        if (t == null) {
          lines.set(index, "#" + line);
        } else {
          return t;
        }
      }
    }
    values.put(key, lines.size());
    lines.add(key + "=" + defaultValue);
    return parser.apply(defaultValue);
  }

  @SuppressWarnings("unchecked")
  public <T extends Enum<T>> T getEnumValue(String key, T defaultValue) {
    return getValueParsed(key, defaultValue.name(), s -> Enum.valueOf((Class<T>) defaultValue.getClass(), s));
  }

  public void setValue(String key, String value) {
    if (values.containsKey(key)) {
      int lineIndex = values.get(key);
      String line = lines.get(lineIndex);
      String oldValue = line.split("=")[1];
      if (oldValue.equals(value)) {
        return;
      }
      lines.set(lineIndex, "#" + line);
    }
    values.put(key, lines.size());
    lines.add(key + "=" + value);
  }

  public void saveConfig() {
    try (FileWriter writer = new FileWriter(file)) {
      for (String line : lines) {
        writer.write(line + '\n');
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
