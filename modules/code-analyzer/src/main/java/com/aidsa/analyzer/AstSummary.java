package com.aidsa.analyzer;

/**
 * Structural summary extracted by static AST inspection (Tree-sitter integration planned in Task 2+).
 */
public record AstSummary(
        LanguageId language,
        int nodeCount,
        int maxLoopNestingDepth,
        boolean hasRecursiveCall
) {
}
