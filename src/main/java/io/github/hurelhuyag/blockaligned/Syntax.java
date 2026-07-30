package io.github.hurelhuyag.blockaligned;

/**
 * The lexical differences that matter when blanking a language's literals and comments.
 *
 * <p>There are only two. Everything else a scanner has to know — quote pairing, backslash
 * escapes, triple-quoted strings, raw-string prefixes, line and block comments — behaves
 * the same in Java and Dart, so {@link Stripper} covers both with one pass.
 *
 * <p>The Dart-only forms that are not listed here need no flag because they are inert in
 * Java: {@code '''} is not valid Java, and a Java identifier can never be immediately
 * followed by a quote, so raw-string detection never fires. Leaving them permanently
 * enabled costs nothing.
 *
 * <p>These two are different — switching either on for Java would silently swallow real
 * code, and switching either off for Dart would desynchronise the scanner for the rest of
 * the file.
 *
 * @param nestedBlockComments whether an inner block comment opener increases depth rather
 *                            than being plain text. Dart nests; Java ends the comment at
 *                            the first terminator, leaving anything after it as code.
 * @param stringInterpolation whether <code>${...}</code> inside a string holds code that
 *                            must be parsed. Dart interpolations may contain strings that
 *                            reuse the enclosing quote, so scanning for the next matching
 *                            quote lands in the wrong place.
 */
public record Syntax(boolean nestedBlockComments, boolean stringInterpolation) {

    /** Java: block comments do not nest, and there is no string interpolation. */
    public static final Syntax JAVA = new Syntax(false, false);

    /** Dart: block comments nest, and interpolations hold arbitrary code. */
    public static final Syntax DART = new Syntax(true, true);
}
