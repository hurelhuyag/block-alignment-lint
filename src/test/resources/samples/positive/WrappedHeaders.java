package samples.positive;

import java.util.List;
import java.util.UUID;

/**
 * Declarations whose header wraps. The opener lands on a continuation line, so the closer
 * anchors on the line the declaration started rather than on the continuation.
 */
public interface WrappedHeaders
        extends Comparable<WrappedHeaders> {

    List<String> findByPartnerId(UUID partnerId);
}

/** A wrapped record header: `) {` hops back to `record`, so the body's `}` sits at col 1. */
record PagedResponse(
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

class WrappedParameters {

    /** A wrapped parameter list: same hop, so the body's `}` lands under `void`. */
    void accept(
            String first,
            String second,
            String third
    ) {
        System.out.println(first + second + third);
    }

    /**
     * An assignment continuation. `=` does not end a statement, so the value expression is
     * part of the `private static final` declaration and its closer anchors under `private`.
     */
    private static final List<String> NAMES =
            List.of(
                    "first",
                    "second"
    );
}
