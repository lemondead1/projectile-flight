package lemondead.game.engine.util;

import lemondead.game.engine.util.vector.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class GeomUtil {
  /**
   * Very slow (probably) ear-clipping triangulation algorithm. It seems to work, however.
   *
   * @return vertex indices
   */
  public static int[] triangulate(List<Vec3> vertices) {
    int size = vertices.size();
    List<Integer> indices = IntStream.range(0, size).collect(() -> new ArrayList<>(size), List::add, (a1, a2) -> {});

    List<Vec3> normals = new ArrayList<>(size);

    int[] result = new int[(vertices.size() - 2) * 3];
    for (int i = 0; i < vertices.size() - 3; i++) {
      double x = 0;
      double y = 0;
      double z = 0;
      for (int l = 0; l < indices.size(); l++) {
        Vec3 prev = vertices.get(Util.getCircular(indices, l - 1));
        Vec3 curr = vertices.get(indices.get(l));
        Vec3 next = vertices.get(Util.getCircular(indices, l + 1));
        Vec3 vec = curr.subtract(prev).cross(next.subtract(curr));
        normals.add(vec);
        x += vec.x();
        y += vec.y();
        z += vec.z();
      }
      Vec3 normalSum = Vec3.of(x, y, z);

      int clipped = clipEar(i, result, normalSum, indices, normals, vertices);
      if (clipped == -1) {
        return result;
      }
      indices.remove(clipped);
      normals.clear();
    }
    result[result.length - 3] = indices.remove(0);
    result[result.length - 2] = indices.remove(0);
    result[result.length - 1] = indices.remove(0);
    return result;
  }

  private static int clipEar(int index, int[] indices, Vec3 normalSum, List<Integer> availableVertices, List<Vec3> normals, List<Vec3> vertices) {
    int size = availableVertices.size();
    int bestI = -1;
    double bestDot = Float.MAX_VALUE;
    outerLoop:
    for (int i = 0; i < size; i++) {
      double dot = normals.get(i).dot(normalSum);
      if (dot < 0 || dot > bestDot) {
        continue;
      }
      int leftIndex = Util.getCircular(availableVertices, i - 1);
      int rightIndex = Util.getCircular(availableVertices, i + 1);
      Vec3 A = vertices.get(leftIndex);
      Vec3 C = vertices.get(rightIndex);
      for (int l = i + 1; l < i + size; l++) {
        Vec3 p1 = vertices.get(availableVertices.get(Math.floorMod(l, size)));
        Vec3 p2 = vertices.get(availableVertices.get(Math.floorMod(l + 1, size)));
        if (checkIntersect(A, C, p1, p2)) {
          continue outerLoop;
        }
      }
      bestI = i;
      bestDot = dot;
    }
    if (bestI != -1) {
      indices[index * 3] = Util.getCircular(availableVertices, bestI - 1);
      indices[index * 3 + 1] = availableVertices.get(bestI);
      indices[index * 3 + 2] = Util.getCircular(availableVertices, bestI + 1);
    }
    return bestI;
  }

  private static boolean checkIntersect(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
    Vec3 ab = b.subtract(a);
    Vec3 cd = d.subtract(c);
    Vec3 ac = c.subtract(a);

    if (Math.abs(ac.dot(ab.cross(cd))) > 1E-5) {
      return false;
    }

    double f = ac.cross(cd).dot(ab.cross(cd)) / ab.cross(cd).lengthSq();
    double g = -ac.cross(ab).dot(cd.cross(ab)) / cd.cross(ab).lengthSq();

    return f > 0 && f < 1 && g > 0 && g < 1;
  }
}
