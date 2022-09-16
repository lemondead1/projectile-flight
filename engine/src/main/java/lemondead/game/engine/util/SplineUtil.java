package lemondead.game.engine.util;

import java.util.Arrays;

public class SplineUtil {
  public static void evalNURBS(float t, float[] points, float[] weights, int dimensionCount, float[] knot, int degree, float[] result) {
    if (t == 0) {
      System.arraycopy(points, 0, result, 0, dimensionCount);
      return;
    }

    Arrays.fill(result, 0);

    int pointCount = points.length / dimensionCount;

    float rationalWeight = 0;

    float[] T = new float[degree + 1];

    for (int i = 0; i < pointCount; i++) {
      float temp = getN(i, degree, knot, T, t);
      rationalWeight += temp * weights[i];
    }

    for (int i = 0; i < pointCount; i++) {
      float temp = getN(i, degree, knot, T, t);
      for (int k = 0; k < dimensionCount; k++) {
        result[k] += points[i * dimensionCount + k] * weights[i] * temp / rationalWeight;
      }
    }
  }

  private static float getN(int i, int p, float[] U, float[] T, float u) {
    if (u <= U[i] || u > U[i + p + 1]) {
      return 0;
    }

    Arrays.fill(T, 0);

    float saved;
    float temp;

    for (int j = 0; j <= p; j++) {
      if (u > U[i + j] && u <= U[i + j + 1]) {
        T[j] = 1;
      } else {
        T[j] = 0;
      }
    }

    for (int k = 1; k <= p; k++) {
      if (T[0] == 0) {
        saved = 0;
      } else {
        saved = ((u - U[i]) * T[0]) / (U[i + k] - U[i]);
      }

      for (int j = 0; j < p - k + 1; j++) {
        float Uleft = U[i + j + 1];
        float Uright = U[i + j + k + 1];

        if (T[j + 1] == 0) {
          T[j] = saved;
          saved = 0;
        } else {
          temp = T[j + 1] / (Uright - Uleft);
          T[j] = saved + (Uright - u) * temp;
          saved = (u - Uleft) * temp;
        }
      }
    }
    return T[0];
  }
}