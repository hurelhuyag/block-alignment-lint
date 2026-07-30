package io.github.hurelhuyag.blockalign;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Entry point for the block alignment check.
 *
 * <p>Typical use from a test:
 *
 * <pre>{@code
 * @Test
 * void sources_are_block_aligned() {
 *     BlockAlignmentLint.assertClean(Path.of("src/main/java"), Path.of("src/test/java"));
 * }
 * }</pre>
 *
 * <p>No formatting is performed; the check only reports.
 */
public final class BlockAlignmentLint {

    private BlockAlignmentLint() {
    }

    /**
     * Checks source text held in memory.
     *
     * @param label    name to report positions against
     * @param source   raw source text
     * @param language language {@code source} is written in
     * @return every misplaced closer
     */
    public static List<Violation> checkSource(String label, String source, Language language) {
        return AlignmentAnalyzer.analyze(label, source, language.stripper());
    }

    /**
     * Checks a single file, choosing the stripper from its extension.
     *
     * @param file a {@code .java} or {@code .dart} file
     * @return every misplaced closer, or an empty list when the extension is unsupported
     * @throws UncheckedIOException if the file cannot be read
     */
    public static List<Violation> checkFile(Path file) {
        return Language.of(file)
                .map(language -> checkSource(file.toString(), read(file), language))
                .orElseGet(List::of);
    }

    /**
     * Walks each root recursively and checks every supported file found. Roots that do not
     * exist are skipped, so callers can pass both {@code src/main/java} and
     * {@code src/test/java} without probing first.
     *
     * @param roots directories or files to scan
     * @return every misplaced closer, ordered by path then position
     * @throws UncheckedIOException if a directory cannot be walked
     */
    public static List<Violation> check(Path... roots) {
        List<Path> files = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile)
                        .filter(path -> Language.of(path).isPresent())
                        .forEach(files::add);
            } catch (IOException e) {
                throw new UncheckedIOException("Could not walk " + root, e);
            }
        }
        files.sort(Comparator.comparing(Path::toString));
        List<Violation> violations = new ArrayList<>();
        for (Path file : files) {
            violations.addAll(checkFile(file));
        }
        return violations;
    }

    /**
     * Runs {@link #check(Path...)} and throws when anything is reported.
     *
     * @param roots directories or files to scan
     * @throws AssertionError listing every violation
     */
    public static void assertClean(Path... roots) {
        List<Violation> violations = check(roots);
        if (!violations.isEmpty()) {
            throw new AssertionError(describe(violations));
        }
    }

    /**
     * Renders a report for humans.
     *
     * @param violations what {@link #check(Path...)} returned
     * @return a multi-line report explaining the rule and listing each violation
     */
    public static String describe(List<Violation> violations) {
        StringBuilder out = new StringBuilder();
        out.append("Block alignment violations (").append(violations.size()).append("). ")
                .append("A closing `)`/`}`/`]` whose opener is on an earlier line must start its own line, ")
                .append("indented to line up with the first character of the line that opened the block — ")
                .append("so the `}` of a `for (...) {` sits under its `f`. Closers may only stack (`}))`) ")
                .append("when the brackets they close were all opened on the same line:");
        for (Violation violation : violations) {
            out.append("\n  ").append(violation.message());
        }
        return out.toString();
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }
}
