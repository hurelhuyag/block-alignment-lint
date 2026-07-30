import 'package:flutter/material.dart';

/// The shapes a Flutter tree drifts into. Lines that should be reported carry a
/// VIOLATION marker, which the test matches against the reported lines.
class Drifted extends StatelessWidget {
  const Drifted({super.key, required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // A dangling tail: the closer parked after the style argument.
        Text(title,
            style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)), // VIOLATION

        // An illegal stack: `Text(` and `GoogleFonts.inter(` opened on different lines.
        Text(title,
            style: TextStyle(
              fontSize: 28,
            )), // VIOLATION

        // Misaligned: the closer starts its line but sits too deep.
        Text(
          title,
          maxLines: 2,
            ), // VIOLATION
      ],
    );
  }
}
