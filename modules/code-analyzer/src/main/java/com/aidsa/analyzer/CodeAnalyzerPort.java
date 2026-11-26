package com.aidsa.analyzer;

/**
 * Port for Tree-sitter based source parsing and complexity heuristic extraction.
 */
public interface CodeAnalyzerPort {

    AstSummary inspect(LanguageId language, String sourceCode);
}
