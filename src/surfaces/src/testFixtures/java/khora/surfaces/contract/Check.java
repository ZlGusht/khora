package khora.surfaces.contract;

/** Argument checks shared by the scenario records. */
final class Check {
    private Check() {}

    static void name(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a name is not blank");
        }
    }

    static void index(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("index is negative: " + index);
        }
    }
}
