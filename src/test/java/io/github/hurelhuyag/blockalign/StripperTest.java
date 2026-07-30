package io.github.hurelhuyag.blockalign;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/**
 * The strippers carry the language-specific risk: a literal form parsed wrongly
 * desynchronises the bracket scanner for the rest of the file. Each case asserts both that
 * no bracket leaks and that the scanner resynchronises onto the code that follows.
 */
class StripperTest {

    private record Case(String name, SourceStripper stripper, String source) {
    }

    private static final Case[] CASES = {
            new Case("java line comment", Language.JAVA.stripper(), "// ( [ {"),
            new Case("java block comment", Language.JAVA.stripper(), "/* ( [ { */"),
            new Case("java string", Language.JAVA.stripper(), "String s = \"(((\";"),
            new Case("java escaped quote", Language.JAVA.stripper(), "String s = \"he said \\\"hi\\\" (\";"),
            new Case("java char literal", Language.JAVA.stripper(), "char c = '(';"),
            new Case("java escaped char literal", Language.JAVA.stripper(), "char c = '\\'';"),
            new Case("java text block", Language.JAVA.stripper(), "String s = \"\"\"\n  { ( [\n  \"\"\";"),
            new Case("dart line comment", Language.DART.stripper(), "// ( [ {"),
            new Case("dart doc comment", Language.DART.stripper(), "/// ( [ {"),
            new Case("dart nested block comment", Language.DART.stripper(), "/* ( /* ) */ ( */"),
            new Case("dart string", Language.DART.stripper(), "var a = '(((';"),
            new Case("dart raw string", Language.DART.stripper(), "var s = r'$x\\(';"),
            new Case("dart escaped quote", Language.DART.stripper(), "var s = 'it\\'s (';"),
            new Case("dart interpolation nested quote", Language.DART.stripper(), "var s = '${m['k']}(';"),
            new Case("dart interpolation braces", Language.DART.stripper(), "var s = '${f({'a': 1})}';"),
            new Case("dart triple quoted", Language.DART.stripper(), "var s = '''( ' \" (''';"),
    };

    @TestFactory
    Stream<DynamicTest> brackets_never_leak_and_offsets_are_preserved() {
        return Stream.of(CASES).map(c -> DynamicTest.dynamicTest(c.name(), () -> {
            String stripped = c.stripper().strip(c.source());
            Assertions.assertEquals(c.source().length(), stripped.length(), "offsets must be preserved");
            Assertions.assertEquals(
                    countNewlines(c.source()),
                    countNewlines(stripped),
                    "line numbers must be preserved"
            );
            Assertions.assertEquals(0, countAny(stripped, "([{)]}"), () -> "leaked from: " + stripped);
        }));
    }

    @TestFactory
    Stream<DynamicTest> scanner_resynchronises_onto_following_code() {
        return Stream.of(CASES).map(c -> DynamicTest.dynamicTest(c.name(), () -> {
            // Separated by a newline so the line-comment cases end before the trailing code.
            String stripped = c.stripper().strip(c.source() + "\nfoo(");
            Assertions.assertTrue(stripped.endsWith("foo("), () -> "stripped: " + stripped);
        }));
    }

    @Test
    void real_code_outside_literals_is_left_untouched() {
        Assertions.assertEquals("foo(bar[   ], 1);", Language.JAVA.stripper().strip("foo(bar[\"k\"], 1);"));
        Assertions.assertEquals("foo(bar[   ], {   : 1});", Language.DART.stripper().strip("foo(bar['k'], {'a': 1});"));
    }

    private static int countNewlines(String text) {
        return (int) text.chars().filter(c -> c == '\n').count();
    }

    private static int countAny(String text, String chars) {
        return (int) text.chars().filter(c -> chars.indexOf(c) >= 0).count();
    }
}
