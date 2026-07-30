package io.github.hurelhuyag.blockaligned;

/**
 * Blanks string literals and comments so only real code brackets remain. One scanner
 * serves every supported language; {@link Syntax} carries the two lexical differences that
 * actually matter.
 *
 * <p>Offsets are preserved exactly: the result has the same length as the input, newlines
 * stay newlines and everything else stripped becomes a space, so line and column indices
 * are identical in both strings.
 */
public final class Stripper implements SourceStripper {

    private final Syntax syntax;

    /**
     * @param syntax the lexical profile to scan with
     */
    public Stripper(Syntax syntax) {
        this.syntax = syntax;
    }

    /** The profile this stripper scans with. */
    public Syntax syntax() {
        return syntax;
    }

    @Override
    public String strip(String source) {
        return new Scan(source, syntax).run();
    }

    private static final class Scan {

        private final String src;
        private final Syntax syntax;
        private final StringBuilder out;
        private int i;

        Scan(String src, Syntax syntax) {
            this.src = src;
            this.syntax = syntax;
            this.out = new StringBuilder(src.length());
        }

        String run() {
            while (i < src.length()) {
                if (src.startsWith("//", i)) {
                    blankLineComment();
                } else if (src.startsWith("/*", i)) {
                    blankBlockComment();
                } else if (isStringStart()) {
                    blankString();
                } else {
                    out.append(src.charAt(i));
                    i++;
                }
            }
            return out.toString();
        }

        /**
         * True when {@code i} begins a string literal — a quote, or an {@code r} prefix
         * that is not merely the tail of an identifier. Java has no raw strings, but the
         * identifier guard means this never fires there.
         */
        private boolean isStringStart() {
            char c = src.charAt(i);
            if (c == '\'' || c == '"') {
                return true;
            }
            if (c != 'r' || i + 1 >= src.length()) {
                return false;
            }
            char next = src.charAt(i + 1);
            if (next != '\'' && next != '"') {
                return false;
            }
            return i == 0 || !isIdentifierPart(src.charAt(i - 1));
        }

        private static boolean isIdentifierPart(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '$';
        }

        private void blankLineComment() {
            while (i < src.length() && src.charAt(i) != '\n') {
                out.append(' ');
                i++;
            }
        }

        /**
         * Where the languages part company. With nesting, an inner opener raises depth and
         * only the matching terminator ends the comment. Without it the first terminator
         * wins, so anything after it on the line is code again.
         */
        private void blankBlockComment() {
            out.append("  ");
            i += 2;
            int depth = 1;
            while (i < src.length()) {
                if (syntax.nestedBlockComments() && src.startsWith("/*", i)) {
                    depth++;
                    out.append("  ");
                    i += 2;
                } else if (src.startsWith("*/", i)) {
                    depth--;
                    out.append("  ");
                    i += 2;
                    if (depth == 0) {
                        return;
                    }
                } else {
                    blank(src.charAt(i));
                    i++;
                }
            }
        }

        /**
         * Blanks one literal: {@code '...'}, {@code "..."} or either tripled, optionally
         * {@code r}-prefixed. A raw string suppresses escapes and interpolation both.
         */
        private void blankString() {
            boolean raw = false;
            if (src.charAt(i) == 'r') {
                out.append(' ');
                i++;
                raw = true;
            }
            char quote = src.charAt(i);
            String triple = String.valueOf(quote).repeat(3);
            boolean isTriple = src.startsWith(triple, i);
            String delim = isTriple ? triple : String.valueOf(quote);
            out.append(" ".repeat(delim.length()));
            i += delim.length();

            while (i < src.length()) {
                if (src.startsWith(delim, i)) {
                    out.append(" ".repeat(delim.length()));
                    i += delim.length();
                    return;
                }
                char c = src.charAt(i);
                if (!raw && c == '\\' && i + 1 < src.length()) {
                    out.append(' ');
                    i++;
                    blank(src.charAt(i));
                    i++;
                } else if (!isTriple && c == '\n') {
                    return; // Unterminated single-line string; bail rather than eat the file.
                } else if (syntax.stringInterpolation() && !raw && src.startsWith("${", i)) {
                    out.append("  ");
                    i += 2;
                    blankInterpolation();
                } else {
                    blank(c);
                    i++;
                }
            }
        }

        /**
         * Called with {@code i} just past the interpolation opener. Blanks through the
         * matching brace, recursing into nested strings so a quote inside the interpolation
         * cannot be mistaken for the end of the enclosing literal.
         */
        private void blankInterpolation() {
            int depth = 1;
            while (i < src.length() && depth > 0) {
                char c = src.charAt(i);
                if (c == '{') {
                    depth++;
                    out.append(' ');
                    i++;
                } else if (c == '}') {
                    depth--;
                    out.append(' ');
                    i++;
                } else if (isStringStart()) {
                    blankString();
                } else {
                    blank(c);
                    i++;
                }
            }
        }

        private void blank(char c) {
            out.append(c == '\n' ? '\n' : ' ');
        }
    }
}
