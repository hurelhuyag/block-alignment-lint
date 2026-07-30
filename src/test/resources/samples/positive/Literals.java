package samples.positive;

/**
 * Brackets hidden inside literals and comments must never reach the analyzer. Every
 * unbalanced bracket below is inside a string, a char literal, a text block or a comment;
 * if the stripper leaked any of them the bracket stack would desynchronise and the closers
 * at the bottom of this file would be reported.
 */
class Literals {

    // A line comment with unbalanced brackets: ( { [ and a quote '
    /* A block comment with unbalanced brackets: ) } ] and a quote " */

    private static final String UNBALANCED = "(((";
    private static final String ESCAPED_QUOTE = "he said \"hi\" and left (";
    private static final String BACKSLASH = "trailing backslash \\";
    private static final char PAREN = '(';
    private static final char QUOTE = '\'';

    private static final String TEXT_BLOCK = """
            {
              "unbalanced": "((( ",
              "quote": "\\""
            }
            """;

    String resynchronised() {
        // If any literal above leaked, this well-formed block would be misreported.
        return String.join(
                "",
                UNBALANCED,
                ESCAPED_QUOTE,
                BACKSLASH,
                TEXT_BLOCK,
                String.valueOf(PAREN),
                String.valueOf(QUOTE)
        );
    }
}
