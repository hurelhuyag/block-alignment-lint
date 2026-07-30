package samples.negative;

import java.util.ArrayList;
import java.util.List;

/**
 * The "dangling tail": a closer parked at the end of a content line. Lines that should be
 * reported carry a VIOLATION marker, which the test matches against the reported lines.
 */
class DanglingTail {

    List<String> tails(List<String> names) {
        List<String> events = new ArrayList<>();
        events.add(describe(names.get(0),
                "trailing closer on a content line")); // VIOLATION
        events.add(describe(
                names.get(1),
                "this one is fine"
        ));
        return events;
    }

    String nestedTail(List<String> names) {
        return String.join(",", subList(names,
                0,
                names.size())); // VIOLATION
    }

    private static List<String> subList(List<String> names, int from, int to) {
        return names.subList(from, to);
    }

    private static String describe(String a, String b) {
        return a + b;
    }
}
