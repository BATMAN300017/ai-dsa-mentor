package com.aidsa.tutoring;

/**
 * Policy guardrails ensuring tutoring hints never reveal complete solutions prematurely.
 */
public record SocraticPromptPolicy(
        String philosophy,
        boolean allowFullSolutionReveal,
        HintLevel maxAllowedHintLevel
) {
    public static final String CORE_PHILOSOPHY = "Think with me. Don't solve it for me.";

    public static SocraticPromptPolicy defaultStrictPolicy() {
        return new SocraticPromptPolicy(CORE_PHILOSOPHY, false, HintLevel.INVARIANT_CHECK);
    }
}
