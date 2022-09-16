package lemondead.game.engine.util;

import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractKeyboardInputManager {
  private final Map<Key, List<KeyBinding>> bindings = new EnumMap<>(Key.class);
  private final Map<KeyBinding, Key> reverseBindings = new LinkedHashMap<>();
  private final WindowWrapper window;
  private final Configuration config;

  public AbstractKeyboardInputManager(WindowWrapper window, Configuration config) {
    this.window = window;
    this.config = config;
    GLFW.glfwSetKeyCallback(window.location, (window1, key, scancode, action, mods) -> {
      for (KeyBinding binding : bindings.getOrDefault(Key.code2KeyMap.get(key), Collections.emptyList())) {
        if (binding.onPress != null && action == GLFW.GLFW_PRESS) {
          binding.onPress.onPress();
        } else if (binding.onRelease != null && action == GLFW.GLFW_RELEASE) {
          binding.onRelease.onRelease();
        }
      }
    });

    init(this::addBinding);
  }

  protected abstract void init(BiConsumer<KeyBinding, Key> consumer);

  public boolean isPressed(KeyBinding binding) {
    return GLFW.glfwGetKey(window.location, reverseBindings.get(binding).code) == GLFW.GLFW_PRESS;
  }

  public Map<KeyBinding, Key> getBindings() {
    return reverseBindings;
  }

  private void addBinding(KeyBinding binding, Key key) {
    key = config.getEnumValue(binding.getName(), key);
    bindings.computeIfAbsent(key, k -> new ArrayList<>()).add(binding);
    reverseBindings.put(binding, key);
  }

  public static class KeyBinding {
    private final OnPress onPress;
    private final OnRelease onRelease;
    public final String name;

    public KeyBinding(OnPress onPress, OnRelease onRelease, String name) {
      this.onPress = onPress;
      this.onRelease = onRelease;
      this.name = name;
    }

    public KeyBinding(OnRelease onRelease, String name) {
      this(() -> {}, onRelease, name);
    }

    public KeyBinding(String name) {
      this(() -> {}, name);
    }

    public String getName() {
      return name;
    }
  }

  public interface OnPress {
    void onPress();
  }

  public interface OnRelease {
    void onRelease();
  }

  public enum Key {
    UNKNOWN(-1, "UNKNOWN"),
    SPACE(32, "SPACE"),
    APOSTROPHE(39, "APOSTROPHE"),
    COMMA(44, "COMMA"),
    MINUS(45, "MINUS"),
    PERIOD(46, "PERIOD"),
    SLASH(47, "SLASH"),
    KEY0(48, "0"),
    KEY1(49, "1"),
    KEY2(50, "2"),
    KEY3(51, "3"),
    KEY4(52, "4"),
    KEY5(53, "5"),
    KEY6(54, "6"),
    KEY7(55, "7"),
    KEY8(56, "8"),
    KEY9(57, "9"),
    SEMICOLON(59, "SEMICOLON"),
    EQUAL(61, "EQUAL"),
    A(65, "A"),
    B(66, "B"),
    C(67, "C"),
    D(68, "D"),
    E(69, "E"),
    F(70, "F"),
    G(71, "G"),
    H(72, "H"),
    I(73, "I"),
    J(74, "J"),
    K(75, "K"),
    L(76, "L"),
    M(77, "M"),
    N(78, "N"),
    O(79, "O"),
    P(80, "P"),
    Q(81, "Q"),
    R(82, "R"),
    S(83, "S"),
    T(84, "T"),
    U(85, "U"),
    V(86, "V"),
    W(87, "W"),
    X(88, "X"),
    Y(89, "Y"),
    Z(90, "Z"),
    LEFT_BRACKET(91, "LEFT_BRACKET"),
    BACKSLASH(92, "BACKSLASH"),
    RIGHT_BRACKET(93, "RIGHT_BRACKET"),
    GRAVE_ACCENT(96, "GRAVE_ACCENT"),
    WORLD_1(161, "WORLD_1"),
    WORLD_2(162, "WORLD_2"),
    ESCAPE(256, "ESCAPE"),
    ENTER(257, "ENTER"),
    TAB(258, "TAB"),
    BACKSPACE(259, "BACKSPACE"),
    INSERT(260, "INSERT"),
    DELETE(261, "DELETE"),
    RIGHT(262, "RIGHT"),
    LEFT(263, "LEFT"),
    DOWN(264, "DOWN"),
    UP(265, "UP"),
    PAGE_UP(266, "PAGE_UP"),
    PAGE_DOWN(267, "PAGE_DOWN"),
    HOME(268, "HOME"),
    END(269, "END"),
    CAPS_LOCK(280, "CAPS_LOCK"),
    SCROLL_LOCK(281, "SCROLL_LOCK"),
    NUM_LOCK(282, "NUM_LOCK"),
    PRINT_SCREEN(283, "PRINT_SCREEN"),
    PAUSE(284, "PAUSE"),
    F1(290, "F1"),
    F2(291, "F2"),
    F3(292, "F3"),
    F4(293, "F4"),
    F5(294, "F5"),
    F6(295, "F6"),
    F7(296, "F7"),
    F8(297, "F8"),
    F9(298, "F9"),
    F10(299, "F10"),
    F11(300, "F11"),
    F12(301, "F12"),
    F13(302, "F13"),
    F14(303, "F14"),
    F15(304, "F15"),
    F16(305, "F16"),
    F17(306, "F17"),
    F18(307, "F18"),
    F19(308, "F19"),
    F20(309, "F20"),
    F21(310, "F21"),
    F22(311, "F22"),
    F23(312, "F23"),
    F24(313, "F24"),
    F25(314, "F25"),
    KP_0(320, "KP_0"),
    KP_1(321, "KP_1"),
    KP_2(322, "KP_2"),
    KP_3(323, "KP_3"),
    KP_4(324, "KP_4"),
    KP_5(325, "KP_5"),
    KP_6(326, "KP_6"),
    KP_7(327, "KP_7"),
    KP_8(328, "KP_8"),
    KP_9(329, "KP_9"),
    KP_DECIMAL(330, "KP_DECIMAL"),
    KP_DIVIDE(331, "KP_DIVIDE"),
    KP_MULTIPLY(332, "KP_MULTIPLY"),
    KP_SUBTRACT(333, "KP_SUBTRACT"),
    KP_ADD(334, "KP_ADD"),
    KP_ENTER(335, "KP_ENTER"),
    KP_EQUAL(336, "KP_EQUAL"),
    LEFT_SHIFT(340, "LEFT_SHIFT"),
    LEFT_CONTROL(341, "LEFT_CONTROL"),
    LEFT_ALT(342, "LEFT_ALT"),
    LEFT_SUPER(343, "LEFT_SUPER"),
    RIGHT_SHIFT(344, "RIGHT_SHIFT"),
    RIGHT_CONTROL(345, "RIGHT_CONTROL"),
    RIGHT_ALT(346, "RIGHT_ALT"),
    RIGHT_SUPER(347, "RIGHT_SUPER"),
    MENU(348, "MENU");

    public static final Map<Integer, Key> code2KeyMap = Arrays.stream(values()).collect(Collectors.toMap(Key::getCode, Function.identity(),
                                                                                                         (a, b) -> {throw new RuntimeException();},
                                                                                                         HashMap::new));
    public static final Map<String, Key> name2KeyMap = Arrays.stream(values()).collect(Collectors.toMap(Key::getName, Function.identity()));

    private final int code;
    private final String name;

    Key(int code, String name) {
      this.code = code;
      this.name = name;
    }

    public int getCode() {
      return code;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "Key{" +
             "code=" + code +
             ", name='" + name + '\'' +
             '}';
    }
  }
}
