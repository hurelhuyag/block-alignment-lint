package samples.negative;

import java.util.List;

/**
 * Stacked closers whose brackets were opened on different lines. Stacking is only legal
 * when every bracket in the run opened on one line, as in `});` or `));`.
 */
class IllegalStack {

    /** `describe(` opens on one line and `List.of(` on the next, so `))` may not stack. */
    String twoLevels(List<String> names) {
        return describe(names.get(0),
                List.of(
                        "a",
                        "b"
                )); // VIOLATION
    }

    /** Same shape one level deeper: the outermost `)` has to drop to its own line. */
    String threeLevels(List<String> names) {
        return describe(
                names.get(0),
                describe(names.get(1),
                        List.of(
                                "c"
                        )) // VIOLATION
        );
    }

    private static String describe(String a, Object b) {
        return a + b;
    }
}
