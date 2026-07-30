package io.github.hurelhuyag.blockaligned;

/**
 * Blanks Dart string literals and comments. Dart needs more care than Java:
 *
 * <ul>
 *   <li>block comments nest, so an inner one does not end the outer;</li>
 *   <li>raw strings (<code>r'...'</code>) suppress both escapes and interpolation;</li>
 *   <li>triple-quoted strings span lines;</li>
 *   <li><code>${...}</code> interpolation holds arbitrary code, including strings that
 *       reuse the enclosing quote — <code>'${map['k']}'</code> is legal Dart — so
 *       interpolations are parsed recursively rather than scanned for the next matching
 *       quote. Getting this wrong desynchronises the scanner for the rest of the file.</li>
 * </ul>
 */
public final class DartStripper implements SourceStripper {

    /** Shared instance; the stripper holds no state. */
    public static final DartStripper INSTANCE = new DartStripper();

    private DartStripper() {
    }

    @Override
    public String strip(String source) {
        return new Scan(source).run();
    }

    private static final class Scan {

        private final String src;
        private final StringBuilder out;
        private int i;

        Scan(String src) {
            this.src = src;
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
         * that is not merely the tail of an identifier.
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

        private void blankBlockComment() {
            int depth = 0;
            while (i < src.length()) {
                if (src.startsWith("/*", i)) {
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
                } else if (!raw && c == '$' && src.startsWith("${", i)) {
                    out.append("  ");
                    i += 2;
                    blankInterpolation();
                } else {
                    blank(c);
                    i++;
                }
            }
        }

        /** Called with {@code i} just past the {@code ${}. Blanks through the matching brace. */
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
