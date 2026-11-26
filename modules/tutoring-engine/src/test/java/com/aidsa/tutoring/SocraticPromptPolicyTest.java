package com.aidsa.tutoring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SocraticPromptPolicyTest {

    @Test
    void defaultPolicyPreventsFullSolutionReveal() {
        SocraticPromptPolicy policy = SocraticPromptPolicy.defaultStrictPolicy();
        assertEquals("Think with me. Don't solve it for me.", policy.philosophy());
        assertFalse(policy.allowFullSolutionReveal());
    }
}
