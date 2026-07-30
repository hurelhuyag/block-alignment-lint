package io.github.hurelhuyag.blockalign;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The language-independent half of the check. Once a {@link SourceStripper} has blanked
 * literals and comments, bracket alignment is the same question in every curly-brace
 * language, so this class is shared by Java and Dart.
 *
 * <p>For every bracket pair the closing delimiter must either share a line with its
 * opener, or start its own line indented to the first character of the line the block
 * began on — the closing brace of a <code>for (...) {</code> sits under its <code>f</code>.
 *
 * <p>Closers may stack (<code>}))</code>) only when the brackets they close were all
 * opened on the same line. That is what keeps <code>});</code> legal — the <code>(</code>
 * and <code>{</code> of <code>setState(() {</code> share a line — while a stack whose
 * openers are spread over several lines is not.
 */
public final class AlignmentAnalyzer {

    private static final String OPEN_CHARS = "([{";
    private static final String CLOSE_CHARS = ")]}";

    /** Characters that, at the end of a line, mean the next line starts something new. */
    private static final String TERMINATORS = ";{}()[],";

    /**
     * Keywords that continue a declaration header onto the next line. Everything else after
     * a terminator starts something new.
     */
    private static final Set<String> DECLARATION_CONTINUATIONS =
            Set.of("throws", "implements", "extends", "permits", "with", "on");

    private AlignmentAnalyzer() {
    }

    /**
     * Analyses one source file.
     *
     * @param label    name to report positions against
     * @param source   raw source text
     * @param stripper strips literals and comments for the language {@code source} is in
     * @return every misplaced closer, in the order encountered
     */
    public static List<Violation> analyze(String label, String source, SourceStripper stripper) {
        String[] raw = source.split("\n", -1);
        String[] code = stripper.strip(source).split("\n", -1);
        List<Violation> violations = new ArrayList<>();
        Deque<Opener> openers = new ArrayDeque<>();
        // For each line, the earliest line it closes a block back to. `) {` and the `{` of
        // `foo(int a,\n int b) {` both anchor where that `(` was opened, not on themselves.
        Map<Integer, Integer> lineOrigin = new HashMap<>();

        for (int li = 0; li < code.length; li++) {
            String line = code[li];
            // The closer handled immediately before, on this line, and the line its opener
            // sat on — together these decide whether stacking here is legal.
            int runCol = -1;
            int runOpenerLine = -1;
            for (int ci = 0; ci < line.length(); ci++) {
                char c = line.charAt(ci);
                if (OPEN_CHARS.indexOf(c) >= 0) {
                    openers.push(new Opener(li, ci, c));
                    continue;
                }
                if (CLOSE_CHARS.indexOf(c) < 0) {
                    continue;
                }
                Opener open = openers.pollFirst();
                if (open == null) {
                    continue;
                }
                if (!matches(open.type, c)) {
                    // The stripper missed something; re-push so the stack stays sane.
                    openers.push(open);
                    continue;
                }
                if (open.line < li) {
                    lineOrigin.merge(li, open.line, Math::min);
                }
                if (open.line == li) {
                    continue;
                }
                int wantCol = alignmentColumn(raw, code, lineOrigin, open.line);
                boolean stacked = ci > 0 && runCol == ci - 1;
                if (stacked && runOpenerLine != open.line) {
                    violations.add(illegalStack(label, li, ci, wantCol, open, runOpenerLine));
                } else if (!stacked && !line.substring(0, ci).isBlank()) {
                    violations.add(contentBefore(label, li, ci, wantCol, open, c));
                } else if (!stacked && ci + 1 != wantCol) {
                    violations.add(misaligned(label, li, ci, wantCol, open, c));
                }
                runCol = ci;
                runOpenerLine = open.line;
            }
        }
        return violations;
    }

    /**
     * The column a closer must line up with: the indent of the first line of the logical
     * declaration owning the bracket, not of the physical line the opener sits on. A
     * wrapped header puts the opener on a continuation line —
     *
     * <pre>
     * public interface Repo
     *         extends JpaRepository&lt;Entity, UUID&gt; {
     * </pre>
     *
     * and the body's closer belongs under {@code public}, not under {@code extends}.
     *
     * <p>A line continues the one above it when that previous line does not end in one of
     * <code>;{}()[],</code>. Each terminator earns its place: a comma ends a complete
     * element, so the sibling below starts fresh rather than chaining; an opener means the
     * next line is the first element of a nested block, which anchors itself.
     *
     * <p>The scan reads stripped source, so a comment or blank line above a declaration
     * stops it instead of dragging the anchor further up.
     *
     * @param raw        original source lines, which carry the real indentation
     * @param code       stripped source lines, used to reason about structure
     * @param lineOrigin earliest line each line closes a block back to
     * @param openerLine zero-based line the opener sits on
     * @return the 1-based column its closer must start at
     */
    private static int alignmentColumn(String[] raw, String[] code, Map<Integer, Integer> lineOrigin, int openerLine) {
        int line = openerLine;
        for (int hops = 0; lineOrigin.containsKey(line) && hops < code.length; hops++) {
            int origin = lineOrigin.get(line);
            if (origin >= line) {
                break;
            }
            line = origin;
        }
        // A declaration keyword puts the scan into header mode, and it stays there: the rest
        // of the header may itself be a wrapped parameter list, whose commas and parens must
        // not stop the walk the way they would inside an argument list.
        boolean header = false;
        while (line > 0) {
            if (startsWithContinuationOperator(code[line])) {
                break;
            }
            header |= continuesDeclaration(code[line]);
            String prev = code[line - 1].strip();
            if (prev.isEmpty()) {
                break;
            }
            if (header && CLOSE_CHARS.indexOf(prev.charAt(0)) >= 0) {
                // A closer-led line above a header either closes the declaration's own
                // wrapped parameter list — keep walking from where that opened — or closes
                // an annotation block sitting above the whole declaration, which stops it.
                Integer origin = lineOrigin.get(line - 1);
                if (origin == null || origin >= line - 1) {
                    break;
                }
                String originLine = code[origin].strip();
                if (!originLine.isEmpty() && originLine.charAt(0) == '@') {
                    break;
                }
                line = origin;
                continue;
            }
            char last = prev.charAt(prev.length() - 1);
            boolean stop = header ? endsHeader(prev, last) : TERMINATORS.indexOf(last) >= 0;
            if (stop) {
                break;
            }
            line--;
        }
        return indentOf(raw[line]) + 1;
    }

    /**
     * Whether {@code prev} sits above the whole declaration rather than inside its header.
     *
     * <p>Once the scan is walking a header it must cross the wrapped parameter list, so the
     * ordinary comma and paren terminators are off. What genuinely precedes a declaration is
     * a finished statement or block, a line that closes an earlier construct, or a complete
     * annotation.
     *
     * <p>Completeness is what separates the two annotation shapes, which otherwise both
     * begin with {@code @}:
     *
     * <pre>
     * &#64;GetMapping(value = "/bulk/{id}")      balanced   -&gt; above the declaration, stop
     *         &#64;PathVariable("id") String id)  unbalanced -&gt; inside the parameter list, keep going
     * </pre>
     *
     * @param prev the stripped line above, already trimmed and known non-empty
     * @param last its final character
     * @return whether the header starts at or below the current line
     */
    private static boolean endsHeader(String prev, char last) {
        if (";{}".indexOf(last) >= 0 || CLOSE_CHARS.indexOf(prev.charAt(0)) >= 0) {
            return true;
        }
        return prev.charAt(0) == '@' && isBracketBalanced(prev);
    }

    private static boolean isBracketBalanced(String text) {
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (OPEN_CHARS.indexOf(c) >= 0) {
                depth++;
            } else if (CLOSE_CHARS.indexOf(c) >= 0) {
                depth--;
            }
        }
        return depth == 0;
    }

    /**
     * Whether this line continues the declaration above rather than starting something new.
     *
     * <p>The distinction cannot be made from structure alone. These two lines above are
     * identical in shape — both end in a closing paren that opened on the same line —
     *
     * <pre>
     * public void upload(MultipartFile file)
     *         throws IOException {        &lt;- continues the declaration
     *
     * if (location != null &amp;&amp; location.floorId == selected)
     *   Positioned(                       &lt;- a body; starts something new
     * </pre>
     *
     * so the answer comes from this line instead: only a declaration keyword carries a
     * header onward. Reading it off the line above put method braces 8 columns too deep in
     * 230 places; ignoring it dragged widget-tree closers up into their enclosing
     * <code>if</code> in 320 more.
     *
     * @param line the stripped line being anchored
     * @return whether the scan should keep walking upward past a terminator
     */
    private static boolean continuesDeclaration(String line) {
        String text = line.strip();
        int end = 0;
        while (end < text.length() && Character.isLetter(text.charAt(end))) {
            end++;
        }
        if (end == 0 || end >= text.length() || !Character.isWhitespace(text.charAt(end))) {
            return false;
        }
        return DECLARATION_CONTINUATIONS.contains(text.substring(0, end));
    }

    /**
     * <code>?</code>, <code>:</code> and <code>.</code> start a visually distinct
     * sub-block — a ternary branch or a link in a method chain — and readers expect the
     * matching closer under them. Arithmetic and boolean continuations (<code>+</code>,
     * <code>&amp;&amp;</code>, ...) deliberately are not listed: the closing brace of an
     * <code>if (a &amp;&amp; b) {</code> spread over two lines belongs under the
     * <code>if</code>, not under the operator.
     *
     * @param line a stripped source line
     * @return whether it opens with an operator that anchors its own block
     */
    private static boolean startsWithContinuationOperator(String line) {
        String text = line.strip();
        if (text.isEmpty()) {
            return false;
        }
        char c = text.charAt(0);
        return c == '?' || c == ':' || c == '.';
    }

    /** Zero-based column of the first non-whitespace character. */
    private static int indentOf(String line) {
        for (int k = 0; k < line.length(); k++) {
            char ch = line.charAt(k);
            if (ch != ' ' && ch != '\t') {
                return k;
            }
        }
        return 0;
    }

    private static boolean matches(char opener, char closer) {
        return (opener == '(' && closer == ')')
                || (opener == '{' && closer == '}')
                || (opener == '[' && closer == ']');
    }

    private static String where(String label, int li, int ci, Opener open, char closer) {
        return label + ":" + (li + 1)
                + " — closing `" + closer + "` (col " + (ci + 1) + ") for `" + open.type
                + "` opened at line " + (open.line + 1) + " col " + (open.col + 1);
    }

    private static Violation misaligned(String label, int li, int ci, int wantCol, Opener open, char closer) {
        String message = where(label, li, ci, open, closer)
                + " is at col " + (ci + 1) + " but must be at col " + wantCol
                + " to align with its opening line";
        return new Violation(label, li + 1, ci + 1, wantCol, open.line + 1, Violation.Kind.MISALIGNED, message);
    }

    private static Violation contentBefore(String label, int li, int ci, int wantCol, Opener open, char closer) {
        String message = where(label, li, ci, open, closer)
                + " is on a content-containing line — move it to its own line at col " + wantCol;
        return new Violation(
                label, li + 1, ci + 1, wantCol, open.line + 1, Violation.Kind.CONTENT_BEFORE_CLOSER, message
        );
    }

    private static Violation illegalStack(String label, int li, int ci, int wantCol, Opener open, int runOpenerLine) {
        String message = where(label, li, ci, open, open.type == '(' ? ')' : open.type == '{' ? '}' : ']')
                + " is stacked behind a closer whose block opened on line " + (runOpenerLine + 1)
                + "; closers may only stack when opened on the same line — move it to its own line at col " + wantCol;
        return new Violation(label, li + 1, ci + 1, wantCol, open.line + 1, Violation.Kind.ILLEGAL_STACK, message);
    }

    private record Opener(int line, int col, char type) {
    }
}
