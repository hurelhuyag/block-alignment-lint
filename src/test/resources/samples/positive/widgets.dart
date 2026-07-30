import 'package:flutter/material.dart';

/// A widget tree in the conventional Flutter shape. Trailing commas keep every closer
/// on its own line, and `);` stacks legally because both brackets open on one line.
class SignageCard extends StatelessWidget {
  const SignageCard({super.key, required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(title, style: const TextStyle(fontSize: 24)),
            const SizedBox(height: 8),
            Text(
              title,
              maxLines: 2,
              style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
            ),
          ],
        ),
      ),
    );
  }
}

/// `setState(() {` opens both brackets on one line, so `});` is a legal stack.
class Counter extends StatefulWidget {
  const Counter({super.key});

  @override
  State<Counter> createState() => _CounterState();
}

class _CounterState extends State<Counter> {
  int _count = 0;

  void _increment() {
    setState(() {
      _count++;
    });
  }

  /// An expression body: the closer anchors under `Widget`, not under the continuation.
  @override
  Widget build(BuildContext context) => Text(
    '$_count',
    style: const TextStyle(fontSize: 20),
  );
}

/// An assignment continuation anchors on the line the declaration starts.
final numbers =
    List<int>.generate(
      10,
      (i) => i * 2,
);

/// A collection-if body. The `if (...)` header ends with `)`, but the widget below it
/// starts a new construct and anchors on itself — it is not a declaration continuation.
Widget conditional(bool showPin, double x) {
  return Column(
    children: [
      const Text('always'),
      if (showPin)
        Positioned(
          left: x,
          child: const Text('here'),
        ),
    ],
  );
}
