/**
 * CodeNarc ruleset for RUN-4839: deterministic cyclomatic complexity check.
 * Informative-only initial threshold; lower gradually as violations are refactored.
 */
ruleset {
    CyclomaticComplexity {
        maxMethodComplexity = 25
    }
}
