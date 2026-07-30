package io.github.hurelhuyag.blockalign;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Why {@link Syntax} has exactly two flags.
 *
 * <p>One scanner handles both languages. These tests pin the boundary: the two flags are
 * each load-bearing, and the remaining Dart-only lexis needs no flag because it never fires
 * on Java source.
 */
class SyntaxTest {

    private static final SourceStripper JAVA = new Stripper(Syntax.JAVA);
    private static final SourceStripper DART = new Stripper(Syntax.DART);

    @Test
    void nesting_is_load_bearing() {
        // Java ends the comment at the first terminator, so `c` and the `(` after it are
        // code again. Dart nests, so the whole thing is one comment.
        String source = "/* a /* b */ c ( */ foo(";
        Assertions.assertTrue(JAVA.strip(source).contains("c"), "Java must see code after the first terminator");
        Assertions.assertEquals(2, countAny(JAVA.strip(source), "("), "Java sees the comment's `(` as code");
        Assertions.assertEquals(1, countAny(DART.strip(source), "("), "Dart must swallow the whole comment");
    }

    @Test
    void interpolation_is_load_bearing() {
        // Two strings inside one interpolation, both reusing the enclosing quote. Scanning
        // for the next matching quote pairs them wrongly and the `(` lands in what the
        // scanner then thinks is code. With an even number of inner quotes the pairing can
        // re-synchronise by luck; this shape does not.
        String source = "var s = '${m['k'] ?? '('}';";
        Assertions.assertEquals(0, countAny(DART.strip(source), "()"), "Dart must blank the whole literal");
        Assertions.assertTrue(countAny(JAVA.strip(source), "()") > 0, "without the flag the scanner desyncs");
    }

    @Test
    void triple_quoted_and_raw_strings_need_no_flag_because_they_never_fire_on_java() {
        // `'''` is not valid Java and an identifier can never be followed by a quote, so
        // leaving both permanently enabled costs Java nothing.
        for (Path sample : javaSamples()) {
            String source = read(sample);
            Assertions.assertEquals(
                    JAVA.strip(source),
                    DART.strip(source),
                    () -> "Dart-only lexis changed the reading of " + sample
            );
        }
    }

    @Test
    void both_profiles_agree_on_the_shared_lexis() {
        List<String> shared = List.of(
                "// line comment ( [ {",
                "/* block comment ) } ] */",
                "String s = \"(((\";",
                "String s = \"escaped \\\" quote (\";",
                "char c = '(';",
                "String s = \"\"\"\n  { ( [\n  \"\"\";",
                "foo(bar[1], baz(2));"
        );
        for (String source : shared) {
            Assertions.assertEquals(JAVA.strip(source), DART.strip(source), () -> "diverged on: " + source);
        }
    }

    private static List<Path> javaSamples() {
        Path root = Path.of("src/test/resources/samples");
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(Language.JAVA.extension()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not walk " + root, e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    private static int countAny(String text, String chars) {
        return (int) text.chars().filter(c -> chars.indexOf(c) >= 0).count();
    }
}
