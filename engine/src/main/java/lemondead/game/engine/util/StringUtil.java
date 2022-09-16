package lemondead.game.engine.util;

import java.util.Arrays;

public class StringUtil {
  public static String[] removeAndSplit(String string, char character) {
    String[] arr = new String[4];
    StringBuilder builder = new StringBuilder();
    int index = 0;
    for (int i = 0; i < string.length(); i++) {
      char ch = string.charAt(i);
      if (ch != character) {
        for (; i < string.length(); i++) {
          ch = string.charAt(i);
          if (ch == character) {
            break;
          }
          builder.append(ch);
        }
        if (arr.length <= index + 1) {
          arr = Arrays.copyOf(arr, arr.length + 4);
        }
        arr[index++] = builder.toString();
        builder.setLength(0);
      }
    }
    return Arrays.stream(arr).limit(index).toArray(String[]::new);
  }
}