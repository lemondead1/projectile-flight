package lemondead.game.engine.util;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Util {
  public static <T> T getCircular(List<T> list, int index) {
    return list.get(index == -1 ? list.size() - 1 : index == list.size() ? 0 : index);
  }

  public static boolean isSorted(int[] array) {
    for (int i = 0; i < array.length - 1; i++) {
      if (array[i + 1] < array[i]) {
        return false;
      }
    }
    return true;
  }

  public static boolean isSorted(float[] array) {
    for (int i = 0; i < array.length - 1; i++) {
      if (array[i + 1] < array[i]) {
        return false;
      }
    }
    return true;
  }

  public static boolean isSorted(long[] array) {
    for (int i = 0; i < array.length - 1; i++) {
      if (array[i + 1] < array[i]) {
        return false;
      }
    }
    return true;
  }

  public static boolean isSorted(double[] array) {
    for (int i = 0; i < array.length - 1; i++) {
      if (array[i + 1] < array[i]) {
        return false;
      }
    }
    return true;
  }

  public static int getNextPowerOf2(int v) {
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    v++;
    return v;
  }

  private static final Method enumConstantDirectory;

  static {
    try {
      enumConstantDirectory = Class.class.getDeclaredMethod("enumConstantDirectory");
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    enumConstantDirectory.setAccessible(true);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  public static <E extends Enum<E>> E getEnumConstIfPresent(Class<E> clazz, String name) {
    try {
      return ((Map<String, E>) enumConstantDirectory.invoke(clazz)).get(name);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
