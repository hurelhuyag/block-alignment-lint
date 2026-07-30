package io.github.hurelhuyag.blockalign;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Dogfood: this project's own sources obey the rule it enforces.
 *
 * <p>Only {@code src/main/java} and {@code src/test/java} are scanned. The sample corpus
 * under {@code src/test/resources} is deliberately excluded — half of it is malformed on
 * purpose.
 */
class SelfCheckTest {

    @Test
    void own_sources_are_block_aligned() {
        BlockAlignmentLint.assertClean(
                Path.of("src/main/java"),
                Path.of("src/test/java")
        );
    }
}
