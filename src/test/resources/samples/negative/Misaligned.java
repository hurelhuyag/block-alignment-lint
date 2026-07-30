package samples.negative;

import java.util.List;

/**
 * Closers that do start their own line, but at the wrong indent. Lines that should be
 * reported carry a VIOLATION marker.
 */
class Misaligned {

    /** Indented past the line that opened the block. */
    List<String> tooDeep() {
        return List.of(
                "a",
                "b"
            ); // VIOLATION
    }

    /** Outdented past the line that opened the block. */
    List<String> tooShallow() {
        return List.of(
                "a",
                "b"
);  // VIOLATION
    }

    /** A wrapped record header whose `)` never came back to col 1. */
    record Wrapped(
            int page,
            int size
        ) { // VIOLATION
    }

    /** A method body brace that drifted off the declaration's column. */
    void drifted() {
        System.out.println("body");
        }  // VIOLATION
}
