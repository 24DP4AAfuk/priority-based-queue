package lv.priority.queue;

final class Ansi {
    private static final boolean ENABLED = System.getenv("NO_COLOR") == null
            && !"dumb".equalsIgnoreCase(System.getenv("TERM"));

    static final String RESET = code(0);
    static final String BOLD = code(1);
    static final String DIM = code(2);
    static final String RED = code(31);
    static final String GREEN = code(32);
    static final String YELLOW = code(33);
    static final String BLUE = code(34);
    static final String MAGENTA = code(35);
    static final String CYAN = code(36);
    static final String WHITE = code(37);

    private Ansi() {
    }

    static String color(String text, String code) {
        if (!ENABLED || text == null) {
            return text;
        }
        return code + text + RESET;
    }

    static String bold(String text) {
        return color(text, BOLD);
    }

    static String dim(String text) {
        return color(text, DIM);
    }

    static String success(String text) {
        return color(text, GREEN);
    }

    static String warning(String text) {
        return color(text, YELLOW);
    }

    static String error(String text) {
        return color(text, RED);
    }

    static String info(String text) {
        return color(text, CYAN);
    }

    static String accent(String text) {
        return color(text, MAGENTA);
    }

    private static String code(int value) {
        return ENABLED ? "\u001B[" + value + "m" : "";
    }
}