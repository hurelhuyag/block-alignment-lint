package io.github.hurelhuyag.blockaligned;

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
            new Case("java line comment", JavaStripper.INSTANCE, "// ( [ {"),
            new Case("java block comment", JavaStripper.INSTANCE, "/* ( [ { */"),
            new Case("java string", JavaStripper.INSTANCE, "String s = \"(((\";"),
            new Case("java escaped quote", JavaStripper.INSTANCE, "String s = \"he said \\\"hi\\\" (\";"),
            new Case("java char literal", JavaStripper.INSTANCE, "char c = '(';"),
            new Case("java escaped char literal", JavaStripper.INSTANCE, "char c = '\\'';"),
            new Case("java text block", JavaStripper.INSTANCE, "String s = \"\"\"\n  { ( [\n  \"\"\";"),
            new Case("dart line comment", DartStripper.INSTANCE, "// ( [ {"),
            new Case("dart doc comment", DartStripper.INSTANCE, "/// ( [ {"),
            new Case("dart nested block comment", DartStripper.INSTANCE, "/* ( /* ) */ ( */"),
            new Case("dart string", DartStripper.INSTANCE, "var a = '(((';"),
            new Case("dart raw string", DartStripper.INSTANCE, "var s = r'$x\\(';"),
            new Case("dart escaped quote", DartStripper.INSTANCE, "var s = 'it\\'s (';"),
            new Case("dart interpolation nested quote", DartStripper.INSTANCE, "var s = '${m['k']}(';"),
            new Case("dart interpolation braces", DartStripper.INSTANCE, "var s = '${f({'a': 1})}';"),
            new Case("dart triple quoted", DartStripper.INSTANCE, "var s = '''( ' \" (''';"),
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
        Assertions.assertEquals("foo(bar[   ], 1);", JavaStripper.INSTANCE.strip("foo(bar[\"k\"], 1);"));
        Assertions.assertEquals("foo(bar[   ], {   : 1});", DartStripper.INSTANCE.strip("foo(bar['k'], {'a': 1});"));
    }

    private static int countNewlines(String text) {
        return (int) text.chars().filter(c -> c == '\n').count();
    }

    private static int countAny(String text, String chars) {
        return (int) text.chars().filter(c -> chars.indexOf(c) >= 0).count();
    }
}
