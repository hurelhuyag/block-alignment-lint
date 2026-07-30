package samples.positive;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Legal closer stacks, method chains and nested argument blocks. */
class StacksAndChains {

    void stacks(List<String> names) {
        // `(` and `{` open on one line, so their closers may stack.
        names.forEach(name -> {
            System.out.println(name);
        });

        // Three openers on one line, so a three-deep stack is legal too.
        run(() -> list(() -> {
            System.out.println("nested");
        }));
    }

    /** The canonical two-opener stack: `assertEquals(` and `queryForObject(` share a line. */
    void sharedOpenerLine(Map<String, String> template) {
        assertEquals("expected", template.get(
                "some-long-key-that-forces-a-wrap"
        ));
    }

    /** A method chain: each `.` line anchors itself, so its closer sits under the dot. */
    List<String> chain(List<String> names) {
        return names.stream()
                .filter(name -> !name.isBlank())
                .map(name -> {
                    return name.strip();
                })
                .collect(Collectors.toList());
    }

    /** A nested argument block anchors on itself, not on the call that contains it. */
    void nested(List<String> events) {
        events.add(
                describe(
                        "item",
                        "location"
                )
        );
        events.add(
                describe(
                        "second",
                        "entry"
                )
        );
    }

    /** A ternary: `?` and `:` lines anchor themselves. */
    String ternary(boolean flag, List<String> names) {
        return flag
                ? String.join(
                        ",",
                        names
                )
                : "";
    }

    private static String describe(String a, String b) {
        return a + b;
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(expected);
        }
    }

    private static void run(Runnable r) {
        r.run();
    }

    private static void list(Runnable r) {
        r.run();
    }
}
