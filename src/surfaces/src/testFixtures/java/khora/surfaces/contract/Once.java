package khora.surfaces.contract;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A value made on first use and shared by the tests of one container. If making it throws a
 * {@link RuntimeException} or an {@link Error}, that is kept: it is not made again, and every use
 * fails with an {@link AssertionError} carrying its message.
 */
final class Once<T> {
    private final Supplier<T> make;
    private T value;
    private Throwable broken;

    Once(Supplier<T> make) {
        this.make = Objects.requireNonNull(make, "make");
    }

    synchronized T get() {
        if (value == null && broken == null) {
            try {
                value = Objects.requireNonNull(make.get(), "made nothing");
            } catch (RuntimeException | Error e) {
                broken = e;
            }
        }
        if (broken != null) {
            throw new AssertionError(broken.getMessage(), broken);
        }
        return value;
    }
}
