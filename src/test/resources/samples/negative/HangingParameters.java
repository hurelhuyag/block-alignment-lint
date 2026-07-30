package samples.negative;

import java.io.IOException;
import java.util.List;

/**
 * The hanging-indent parameter list, IntelliJ's "align when multiline" output and the most
 * common shape in the wild. The closing paren lands after the last parameter, so it is a
 * dangling tail and is reported.
 *
 * <p>What matters just as much is what is <em>not</em> reported: each method's closing brace
 * belongs at the declaration's own column, and an earlier version of the anchoring put it
 * eight columns deeper in 226 places. Only the parameter lines carry a marker, so this file
 * fails if those braces ever start being reported again.
 */
class HangingParameters {

    List<String> bulkDetail(String channel,
            String externalId) {          // VIOLATION
        return List.of(channel, externalId);
    }

    /** Same, with the throws clause wrapped onto its own line. */
    List<String> withThrows(String channel,
            String externalId)            // VIOLATION
            throws IOException {
        return List.of(channel, externalId);
    }

    /** Same, with parameter annotations, which must not read as declaration annotations. */
    List<String> annotated(@Deprecated String channel,
            @Deprecated String externalId) {   // VIOLATION
        return List.of(channel, externalId);
    }
}
