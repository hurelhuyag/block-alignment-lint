package io.github.hurelhuyag.blockalign;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The rule itself, pinned case by case on minimal snippets. */
class AlignmentAnalyzerTest {

    private static List<Violation> java(String source) {
        return BlockAlignLint.checkSource("x.java", source, Language.JAVA);
    }

    private static List<Violation> dart(String source) {
        return BlockAlignLint.checkSource("x.dart", source, Language.DART);
    }

    @Nested
    @DisplayName("alignment")
    class Alignment {

        @Test
        void closer_under_the_first_character_of_its_opening_line_passes() {
            Assertions.assertTrue(java("foo(\n  1\n);").isEmpty());
            Assertions.assertTrue(java("  foo(\n    1\n  );").isEmpty());
        }

        @Test
        void closer_indented_past_its_opening_line_is_flagged() {
            List<Violation> violations = java("  foo(\n    1\n    );");
            Assertions.assertEquals(1, violations.size());
            Assertions.assertEquals(Violation.Kind.MISALIGNED, violations.get(0).kind());
            Assertions.assertEquals(3, violations.get(0).expectedColumn());
        }

        @Test
        void closer_outdented_past_its_opening_line_is_flagged() {
            Assertions.assertEquals(1, java("  foo(\n    1\n);").size());
        }

        @Test
        void closer_sharing_its_opener_line_is_always_fine() {
            Assertions.assertTrue(java("foo(bar(1), baz(2));").isEmpty());
        }
    }

    @Nested
    @DisplayName("stacking")
    class Stacking {

        @Test
        void stack_is_legal_when_both_brackets_opened_on_one_line() {
            Assertions.assertTrue(java("run(() -> {\n  x();\n});").isEmpty());
            Assertions.assertTrue(dart("setState(() {\n  x();\n});").isEmpty());
        }

        @Test
        void stack_is_illegal_when_openers_are_on_different_lines() {
            List<Violation> violations = dart("Text('a',\n    style: Inter(\n        size: 1\n    ));");
            Assertions.assertEquals(1, violations.size());
            Assertions.assertEquals(Violation.Kind.ILLEGAL_STACK, violations.get(0).kind());
        }

        @Test
        void splitting_that_stack_onto_its_own_aligned_line_passes() {
            Assertions.assertTrue(dart("Text('a',\n    style: Inter(\n        size: 1\n    )\n);").isEmpty());
        }
    }

    @Nested
    @DisplayName("anchoring")
    class Anchoring {

        @Test
        void wrapped_header_anchors_on_the_line_the_declaration_starts() {
            Assertions.assertTrue(java("interface A\n        extends B {\n    void c();\n}").isEmpty());
            Assertions.assertEquals(1, java("interface A\n        extends B {\n    void c();\n        }").size());
        }

        @Test
        void closing_paren_then_brace_hops_back_to_the_wrapped_header() {
            Assertions.assertTrue(java("record R(\n        int a\n) {\n}").isEmpty());
        }

        @Test
        void nested_block_anchors_on_itself_not_on_its_parent() {
            Assertions.assertTrue(java("events.add(\n        violated(\n                a\n        )\n);").isEmpty());
        }

        @Test
        void trailing_comma_does_not_drag_the_anchor_upward() {
            Assertions.assertTrue(java("f(\n        g(\n                a\n        ),\n        g(\n                b\n        )\n);").isEmpty());
        }

        @Test
        void method_chain_links_anchor_under_their_dot() {
            Assertions.assertTrue(java("a.b()\n        .c(\n                d\n        )\n        .e();").isEmpty());
        }

        @Test
        void ternary_branches_anchor_under_their_operator() {
            Assertions.assertTrue(java("var x = flag\n        ? f(\n                a\n        )\n        : null;").isEmpty());
        }
    }

    @Nested
    @DisplayName("content before a closer")
    class ContentBefore {

        @Test
        void dangling_tail_is_flagged() {
            List<Violation> violations = java("events.add(describe(a,\n        b));");
            Assertions.assertFalse(violations.isEmpty());
            Assertions.assertEquals(Violation.Kind.CONTENT_BEFORE_CLOSER, violations.get(0).kind());
        }
    }
}
