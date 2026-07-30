package io.github.hurelhuyag.blockaligned;

/**
 * A single misplaced closing delimiter.
 *
 * @param file            label of the analysed source, usually a repository-relative path
 * @param line            1-based line the closer sits on
 * @param column          1-based column the closer sits at
 * @param expectedColumn  1-based column it should sit at
 * @param openerLine      1-based line the matching opener sits on
 * @param kind            why it was reported
 * @param message         human-readable rendering, the same text {@link #toString()} returns
 */
public record Violation(
        String file,
        int line,
        int column,
        int expectedColumn,
        int openerLine,
        Kind kind,
        String message
) {

    /** Why a closer was reported. */
    public enum Kind {
        /** The closer starts its line but at the wrong indent. */
        MISALIGNED,
        /** The closer is stacked behind one whose block opened on a different line. */
        ILLEGAL_STACK,
        /** Something other than whitespace and closers precedes it on the line. */
        CONTENT_BEFORE_CLOSER
    }

    @Override
    public String toString() {
        return message;
    }
}
