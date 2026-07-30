package io.github.hurelhuyag.blockaligned;

/**
 * Blanks Java string literals, character literals, text blocks, line comments and block
 * comments. Java block comments do not nest, so the first {@code *}{@code /} ends one.
 */
public final class JavaStripper implements SourceStripper {

    /** Shared instance; the stripper holds no state. */
    public static final JavaStripper INSTANCE = new JavaStripper();

    private JavaStripper() {
    }

    @Override
    public String strip(String source) {
        int n = source.length();
        StringBuilder out = new StringBuilder(n);
        int i = 0;
        while (i < n) {
            char c = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : '\0';

            if (c == '/' && next == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
                continue;
            }
            if (c == '/' && next == '*') {
                out.append("  ");
                i += 2;
                while (i < n) {
                    if (i + 1 < n && source.charAt(i) == '*' && source.charAt(i + 1) == '/') {
                        out.append("  ");
                        i += 2;
                        break;
                    }
                    out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                continue;
            }
            if (c == '"' && next == '"' && i + 2 < n && source.charAt(i + 2) == '"') {
                out.append("   ");
                i += 3;
                while (i < n) {
                    if (i + 2 < n && source.startsWith("\"\"\"", i)) {
                        out.append("   ");
                        i += 3;
                        break;
                    }
                    out.append(source.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                i = blankQuoted(source, out, i, c);
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    /** Blanks a single-line {@code "..."} or {@code '...'} literal, honouring backslash escapes. */
    private static int blankQuoted(String source, StringBuilder out, int start, char quote) {
        int n = source.length();
        out.append(' ');
        int i = start + 1;
        while (i < n && source.charAt(i) != quote && source.charAt(i) != '\n') {
            if (source.charAt(i) == '\\' && i + 1 < n && source.charAt(i + 1) != '\n') {
                out.append("  ");
                i += 2;
            } else {
                out.append(' ');
                i++;
            }
        }
        if (i < n && source.charAt(i) == quote) {
            out.append(' ');
            i++;
        }
        return i;
    }
}
