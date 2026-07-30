package samples.positive;

import java.io.IOException;
import java.util.List;

/**
 * Declaration headers that wrap. Every closer here belongs under the first character of the
 * declaration, not under whichever continuation line the opener happened to land on.
 *
 * <p>Each shape below was a false positive at some point, found by sweeping real projects
 * rather than by imagining cases. They are kept as samples so the anchoring cannot regress.
 */
class DeclarationHeaders {

    /** A wrapped throws clause. The body's brace belongs under `void`, not under `throws`. */
    void uploadBatch(String file)
            throws IOException {
        System.out.println(file);
    }

    /**
     * A block parameter list closing at the declaration's own column, with the throws clause
     * riding on the same line.
     */
    List<String> bulkDetail(
            String channel,
            String externalId
    ) throws IOException {
        return List.of(channel, externalId);
    }

    /**
     * A parameter annotation looks like the annotation above a declaration but is not one:
     * it sits inside the parameter list, so it must not stop the walk up to `List`.
     */
    List<String> annotated(
            @Deprecated String channel,
            @Deprecated String externalId
    )
            throws IOException {
        return List.of(channel, externalId);
    }

    /** The annotation above a declaration does stop it — this closer stays under `List`. */
    @Deprecated
    List<String> afterAnnotation(String channel)
            throws IOException {
        return List.of(channel);
    }
}

/**
 * The JAXB-generated shape: an annotation array closing with `})`, an `extends` clause on
 * its own line, and the class brace alone on the line after that.
 */
@SuppressWarnings({
    "unused"
})
class BraceOnItsOwnLine
    extends RuntimeException
{

    private String field;
}
