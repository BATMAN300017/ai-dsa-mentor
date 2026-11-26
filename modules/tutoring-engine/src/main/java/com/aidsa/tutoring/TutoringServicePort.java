package com.aidsa.tutoring;

/**
 * Contract for generating progressive Socratic guidance in future tasks.
 */
public interface TutoringServicePort {

    SocraticPromptPolicy activePolicy();
}
