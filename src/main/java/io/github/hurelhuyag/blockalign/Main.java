package io.github.hurelhuyag.blockalign;

import java.nio.file.Path;
import java.util.List;

/**
 * Command-line entry point, so the check is usable from projects Maven does not build —
 * a Dart or Flutter repository, for instance:
 *
 * <pre>
 * java -jar block-alignment-lint.jar lib test
 * </pre>
 *
 * Exits 0 when clean, 1 when violations were found, 2 on bad usage.
 */
public final class Main {

    private Main() {
    }

    /**
     * @param args one or more files or directories to scan
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("usage: block-alignment-lint <file-or-directory>...");
            System.exit(2);
            return;
        }
        Path[] roots = new Path[args.length];
        for (int i = 0; i < args.length; i++) {
            roots[i] = Path.of(args[i]);
        }
        List<Violation> violations = BlockAlignmentLint.check(roots);
        if (violations.isEmpty()) {
            System.out.println("Block alignment: clean.");
            return;
        }
        System.err.println(BlockAlignmentLint.describe(violations));
        System.exit(1);
    }
}
