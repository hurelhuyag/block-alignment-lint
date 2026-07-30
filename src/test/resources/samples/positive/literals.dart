/// Dart literal forms that a Java-shaped stripper gets wrong. Every unbalanced bracket
/// below sits inside a string or comment; if any leaked, the balanced block at the bottom
/// would be misreported.

/* A block comment with unbalanced brackets: ) } ]
   /* and Dart block comments nest, so this inner one must not end the outer: ( { [ */
   still inside the outer comment: ( */

const unbalanced = '(((';
const escapedQuote = 'it\'s got a paren (';
const rawString = r'$notInterpolated \not\an\escape (';
const rawTriple = r"""a ' " ( inside a raw triple""";

const tripleQuoted = '''
{
  "unbalanced": "((( ",
}
''';

String interpolation(Map<String, String> map, List<int> values) {
  // `${map['k']}` reuses the enclosing quote inside the interpolation, and `${f({...})}`
  // puts braces in it. Both must be parsed, not scanned for the next quote.
  final a = '${map['k']} (';
  final b = '${values.map((v) => v + 1).join(',')} {';
  final c = 'plain $a and ${b.length} more (';
  return a + b + c;
}

/// If the stripper resynchronised correctly, this block is well formed and silent.
List<String> resynchronised(Map<String, String> map) {
  return <String>[
    unbalanced,
    escapedQuote,
    rawString,
    rawTriple,
    tripleQuoted,
    interpolation(
      map,
      const [1, 2],
    ),
  ];
}
