package io.github.hurelhuyag.blockalign;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Drives the checker over the sample corpus.
 *
 * <p>Every file under {@code samples/positive} must report nothing. Every file under
 * {@code samples/negative} carries a {@code VIOLATION} marker comment on each line that
 * should be reported, and the test asserts the reported lines match the marked lines
 * exactly — so a sample cannot silently pass by reporting the wrong place, and cannot
 * over-report either.
 */
class SamplesTest {

    private static final Path POSITIVE = Path.of("src/test/resources/samples/positive");
    private static final Path NEGATIVE = Path.of("src/test/resources/samples/negative");
    private static final String MARKER = "VIOLATION";

    @TestFactory
    Stream<DynamicTest> positive_samples_report_nothing() {
        return samples(POSITIVE).map(file -> DynamicTest.dynamicTest(
                POSITIVE.relativize(file).toString(),
                () -> {
                    List<Violation> violations = BlockAlignmentLint.checkFile(file);
                    Assertions.assertTrue(violations.isEmpty(), () -> BlockAlignmentLint.describe(violations));
                }
        ));
    }

    @TestFactory
    Stream<DynamicTest> negative_samples_report_exactly_the_marked_lines() {
        return samples(NEGATIVE).map(file -> DynamicTest.dynamicTest(
                NEGATIVE.relativize(file).toString(),
                () -> {
                    List<Integer> expected = markedLines(file);
                    Assertions.assertFalse(expected.isEmpty(), "sample carries no " + MARKER + " marker: " + file);
                    List<Integer> actual = BlockAlignmentLint.checkFile(file).stream()
                            .map(Violation::line)
                            .distinct()
                            .sorted()
                            .toList();
                    Assertions.assertEquals(
                            expected,
                            actual,
                            () -> "reported lines do not match the " + MARKER + " markers in " + file
                                    + "\n" + BlockAlignmentLint.describe(BlockAlignmentLint.checkFile(file))
                    );
                }
        ));
    }

    @TestFactory
    Stream<DynamicTest> every_sample_is_covered_by_both_languages() {
        return Stream.of(POSITIVE, NEGATIVE).map(dir -> DynamicTest.dynamicTest(
                dir.getFileName() + " covers Java and Dart",
                () -> {
                    List<Path> files = samples(dir).toList();
                    for (Language language : Language.values()) {
                        boolean covered = files.stream().anyMatch(f -> f.toString().endsWith(language.extension()));
                        Assertions.assertTrue(covered, () -> dir + " has no " + language + " sample");
                    }
                }
        ));
    }

    /** Lines carrying a {@code VIOLATION} marker comment, 1-based and ascending. */
    private static List<Integer> markedLines(Path file) {
        List<String> lines = readLines(file);
        List<Integer> marked = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            // The marker must close the line, so prose describing it cannot mark one.
            if (lines.get(i).stripTrailing().endsWith("// " + MARKER)) {
                marked.add(i + 1);
            }
        }
        return marked;
    }

    private static Stream<Path> samples(Path dir) {
        Assertions.assertTrue(Files.isDirectory(dir), () -> "missing sample directory: " + dir.toAbsolutePath());
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> Language.of(path).isPresent())
                    .sorted()
                    .toList()
                    .stream();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not walk " + dir, e);
        }
    }

    private static List<String> readLines(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }
}
