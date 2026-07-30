# block-alignment-lint

A lint for **Java** and **Dart** that reports closing brackets which have drifted: every closer
must either share a line with its opener, or start its own line aligned with the first character
of the line the block began on — the `}` of a `for (...) {` sits under its `f`.

**It never rewrites a file.** There is no formatter here and no `--fix`. It reads your sources,
tells you which closers are misplaced and which column each belongs at, and stops. How you get
them there — by hand, by IDE, by not caring on a given line — stays your call.

The point is review. When every closer sits under the thing it closes, a reader can see the
nesting without counting brackets, and a diff that changes structure looks different from one
that changes a line. A dangling `));` hides that.

```
implementation "io.github.hurelhuyag:block-alignment-lint:1.0.0"
```

```xml
<dependency>
    <groupId>io.github.hurelhuyag</groupId>
    <artifactId>block-alignment-lint</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

## The rule

A closer whose opener is on an **earlier** line must start its own line, indented to the column
where that block's declaration begins.

```java
// bad — "dangling tail", the closer parked after content
events.add(SimpleConditionEvent.violated(item,
        item.getFullName() + " is an orphan adapter"));

// good
events.add(SimpleConditionEvent.violated(
        item,
        item.getFullName() + " is an orphan adapter"
));
```

### Stacking

Closers may stack (`}))`, `});`) **only when every bracket in the run was opened on the same
line**. That single condition is what makes the common idioms legal while catching real drift:

```java
// legal — `assertEquals(` and `queryForObject(` open on one line
assertEquals(3L, jdbcTemplate.queryForObject(
        "SELECT count(*) FROM run", Long.class, runId
));
```

```dart
// legal — the `(` and `{` of `setState(() {` open on one line
setState(() {
  _count++;
});

// not legal — `Text(` opened on one line, `GoogleFonts.inter(` on the next
Text('MALL',
    style: GoogleFonts.inter(
        fontSize: 28,
    )),

// good
Text('MALL',
    style: GoogleFonts.inter(
        fontSize: 28,
    )
),
```

### Anchoring

The target column comes from the line the **declaration** starts on, not the physical line the
opener happens to sit on. When a header wraps, those differ:

```java
public interface PartnerRepository
        extends JpaRepository<PartnerEntity, UUID> {
    List<PartnerEntity> findByOwnerId(UUID ownerId);
}   // <- under `public`, not under `extends`
```

A line ending in `;` `{` `}` `(` `[` `)` `]` or `,` ends the construct above it, so the next line
anchors itself. That is what keeps a nested argument block from borrowing its parent's indent:

```java
events.add(
        describe(          // <- anchors here, not on `events.add`
                item
        )
);
```

Lines opening with `?`, `:` or `.` anchor themselves too — a ternary branch or a link in a method
chain is where readers expect the matching closer.

## Usage

### From a Java test

```java
import io.github.hurelhuyag.blockalign.BlockAlignmentLint;

class FormattingTest {

    @Test
    void sources_are_block_aligned() {
        BlockAlignmentLint.assertClean(
                Path.of("src/main/java"),
                Path.of("src/test/java")
        );
    }
}
```

`assertClean` throws an `AssertionError` naming every violation, its current column and the
column it must move to. To inspect rather than assert:

```java
List<Violation> violations = BlockAlignmentLint.check(Path.of("src/main/java"));
violations.forEach(v -> System.out.println(v.file() + ":" + v.line() + " -> col " + v.expectedColumn()));
```

`Violation.kind()` is one of `MISALIGNED`, `ILLEGAL_STACK` or `CONTENT_BEFORE_CLOSER`.

### From the command line

For projects Maven does not build — a Flutter repository, say:

```
java -jar block-alignment-lint-1.0.0.jar lib test
```

Exits `0` when clean, `1` when violations were found, `2` on bad usage.

## Supported languages

| Language | Extension |
|---|---|
| Java | `.java` |
| Dart | `.dart` |

**It is one algorithm, not two.** Bracket alignment is the same question in every curly-brace
language, and so is most of the lexing — quote pairing, backslash escapes, triple-quoted
strings, raw-string prefixes, line and block comments all behave identically. A single
`Stripper` does the whole job, and `Syntax` carries the only two differences that matter:

| Flag | Java | Dart | Why it cannot be shared |
|---|---|---|---|
| `nestedBlockComments` | `false` | `true` | Java ends a comment at the first terminator, so text after it is code again; Dart nests. Get it wrong and you either leak a bracket or swallow real code. |
| `stringInterpolation` | `false` | `true` | `'${m['k'] ?? '('}'` is legal Dart. Scanning for the next matching quote pairs them wrongly and desynchronises the scanner for the rest of the file. |

Everything else Dart-only needs no flag because it is inert in Java: `'''` is not valid Java, and
a Java identifier can never be immediately followed by a quote, so raw-string detection never
fires. `SyntaxTest` pins all of this — including that every Java sample strips *identically*
under both profiles.

Adding a language is a `Syntax` value and an enum constant.

## Samples

`src/test/resources/samples/` is the executable specification.

- `positive/` — every shape that must pass. Any report here is a false positive.
- `negative/` — every shape that must fail. Each offending line ends with a `// VIOLATION`
  marker, and the test asserts the reported lines match the markers **exactly**, so a sample
  cannot pass by reporting the wrong line, nor by over-reporting.

Add a sample when you find a shape the checker gets wrong; it is the cheapest way to pin a fix.

The project also checks its own sources (`SelfCheckTest`).

## Building

```
mvn test        # 65 tests
mvn package     # jar, sources, javadoc
```

## Releasing

CI runs `mvn verify` on JDK 17, 21 and 25 for every push and pull request. Pushing a
`vMAJOR.MINOR.PATCH` tag sets the POM version from the tag, runs the tests, signs the
artifacts and publishes to Maven Central, then opens a GitHub Release with the three jars.

```
git tag v1.0.1 && git push origin v1.0.1
```

Four repository secrets are required:

| Secret | Where it comes from |
|---|---|
| `CENTRAL_TOKEN_USERNAME` | central.sonatype.com -> View Account -> Generate User Token |
| `CENTRAL_TOKEN_PASSWORD` | the same token's password half |
| `GPG_PRIVATE_KEY` | `gpg --armor --export-secret-keys <KEY_ID>`, whole block including the header lines |
| `GPG_PASSPHRASE` | the passphrase for that key |

The public half of the key must be on a keyserver Central checks, e.g.
`gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`.

The release job targets a `maven-central` environment. Add a required reviewer to it in
repository settings if you want the deploy gated behind an approval — the POM sets
`autoPublish=true`, and a version released to Central can never be replaced or withdrawn.

## License

Apache License 2.0 — see [LICENSE](LICENSE).
