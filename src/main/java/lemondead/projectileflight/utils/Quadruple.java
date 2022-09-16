package lemondead.projectileflight.utils;

public class Quadruple<T1, T2, T3, T4> {
  private final T1 object1;
  private final T2 object2;
  private final T3 object3;
  private final T4 object4;

  public Quadruple(T1 object1, T2 object2, T3 object3, T4 object4) {
    this.object1 = object1;
    this.object2 = object2;
    this.object3 = object3;
    this.object4 = object4;
  }

  public T1 getObject1() {
    return object1;
  }

  public T2 getObject2() {
    return object2;
  }

  public T3 getObject3() {
    return object3;
  }

  public T4 getObject4() {
    return object4;
  }
}
