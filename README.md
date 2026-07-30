# block-aligned-formatting

A formatting check for **Java** and **Dart**: every closing bracket must either share a line
with its opener, or start its own line aligned with the first character of the line the block
began on — the `}` of a `for (...) {` sits under its `f`.

No formatting is performed. The check only reports.

```
implementation "io.github.hurelhuyag:block-aligned-formatting:1.0.0"
```

```xml
<dependency>
    <groupId>io.github.hurelhuyag</groupId>
    <artifactId>block-aligned-formatting</artifactId>
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
import io.github.hurelhuyag.blockaligned.BlockAlignedFormatting;

class FormattingTest {

    @Test
    void sources_are_block_aligned() {
        BlockAlignedFormatting.assertClean(
                Path.of("src/main/java"),
                Path.of("src/test/java")
        );
    }
}
```

`assertClean` throws an `AssertionError` naming every violation, its current column and the
column it must move to. To inspect rather than assert:

```java
List<Violation> violations = BlockAlignedFormatting.check(Path.of("src/main/java"));
violations.forEach(v -> System.out.println(v.file() + ":" + v.line() + " -> col " + v.expectedColumn()));
```

`Violation.kind()` is one of `MISALIGNED`, `ILLEGAL_STACK` or `CONTENT_BEFORE_CLOSER`.

### From the command line

For projects Maven does not build — a Flutter repository, say:

```
java -jar block-aligned-formatting-1.0.0.jar lib test
```

Exits `0` when clean, `1` when violations were found, `2` on bad usage.

## Supported languages

| Language | Extension | Notes |
|---|---|---|
| Java | `.java` | strings, char literals, text blocks, line and block comments |
| Dart | `.dart` | raw strings, triple-quoted strings, `${...}` interpolation, **nested** block comments |

Bracket alignment is the same question in both, so the analyzer is shared; only the literal and
comment stripping differs. Dart needs the extra care — `'${map['k']}'` is legal Dart, and a
scanner that looks for the next matching quote desynchronises for the rest of the file.

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
mvn test        # 59 tests
mvn package     # jar, sources, javadoc
```

## License

Apache License 2.0 — see [LICENSE](LICENSE).
