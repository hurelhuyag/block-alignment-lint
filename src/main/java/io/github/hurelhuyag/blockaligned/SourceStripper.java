package io.github.hurelhuyag.blockaligned;

/**
 * Blanks out everything a bracket scanner must ignore — string literals and comments —
 * so that only real code brackets remain.
 *
 * <p>Implementations must preserve offsets exactly: the returned string has the same
 * length as the input, every newline stays a newline, and every other stripped character
 * becomes a space. Line and column indices are therefore identical in both strings, which
 * is what lets the analyzer report positions against the original source.
 */
public interface SourceStripper {

    /**
     * Blanks literals and comments, preserving every offset.
     *
     * @param source raw source text
     * @return the same text with literals and comments replaced by spaces
     */
    String strip(String source);
}
